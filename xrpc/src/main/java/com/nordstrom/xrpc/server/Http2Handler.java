/*
 * Copyright 2018 Nordstrom, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nordstrom.xrpc.server;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import com.codahale.metrics.Meter;
import com.nordstrom.xrpc.XrpcConstants;
import com.nordstrom.xrpc.client.XUrl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.HttpConversionUtil;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Http2Handler extends Http2ConnectionHandler implements Http2FrameListener {

  private final int maxPayloadBytes;

  Http2Handler(
      Http2ConnectionDecoder decoder,
      Http2ConnectionEncoder encoder,
      Http2Settings initialSettings,
      int maxPayloadBytes) {
    super(decoder, encoder, initialSettings);

    this.maxPayloadBytes = maxPayloadBytes;
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {}

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    super.exceptionCaught(ctx, cause);
    cause.printStackTrace();
    ctx.close();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ctx.channel().attr(XrpcConstants.CONNECTION_CONTEXT).get().getRequestMeter().mark();
  }

  /** Writes the given HTTP/1 response to the given stream, using the given context. */
  private void sendResponse(ChannelHandlerContext ctx, FullHttpResponse h1Resp, int streamId)
      throws IOException {
    XrpcConnectionContext xctx = ctx.channel().attr(XrpcConstants.CONNECTION_CONTEXT).get();
    Optional<Meter> statusMeter =
        Optional.ofNullable(xctx.getMetersByStatusCode().get(h1Resp.status()));
    statusMeter.ifPresent(Meter::mark);

    Http2Headers responseHeaders = HttpConversionUtil.toHttp2Headers(h1Resp, true);
    Http2DataFrame responseDataFrame = new DefaultHttp2DataFrame(h1Resp.content(), true);
    encoder().writeHeaders(ctx, streamId, responseHeaders, 0, false, ctx.newPromise());
    encoder().writeData(ctx, streamId, responseDataFrame.content(), 0, true, ctx.newPromise());
  }

  private void writeResponse(
      ChannelHandlerContext ctx, int streamId, HttpResponseStatus status, ByteBuf buffer) {
    FullHttpResponse h1Resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buffer);
    h1Resp.headers().set(CONTENT_TYPE, "text/plain");
    h1Resp.headers().setInt(CONTENT_LENGTH, buffer.readableBytes());

    Http2Headers responseHeaders = HttpConversionUtil.toHttp2Headers(h1Resp, true);
    Http2DataFrame responseDataFrame = new DefaultHttp2DataFrame(h1Resp.content(), true);
    encoder().writeHeaders(ctx, streamId, responseHeaders, 0, false, ctx.newPromise());
    encoder().writeData(ctx, streamId, responseDataFrame.content(), 0, true, ctx.newPromise());

    ctx.channel()
        .attr(XrpcConstants.CONNECTION_CONTEXT)
        .get()
        .getMetersByStatusCode()
        .get(status)
        .mark();
  }

  @Override
  public int onDataRead(
      ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) {
    // Both request and handler should be set if endOfStream wasn't set earlier. The checks below
    // shouldn't be triggered without a bad request coming in (end of stream marked twice).
    // TODO(jkinkead): Check if the codec will even pass through a data frame if END_STREAM was set
    // previously.
    XrpcRequest request = ctx.channel().attr(XrpcConstants.XRPC_REQUEST).get();
    if (request == null) {
      log.error("Missing request attribute in channel; can't process request");
      // TODO(jkinkead): Return a 500?
      return 0;
    }

    int totalRead = request.addData(data);
    int processed = data.readableBytes() + padding;
    if (totalRead > maxPayloadBytes) {
      // Close request & channel to prevent overflow.
      writeResponse(
          ctx,
          streamId,
          HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE,
          Unpooled.wrappedBuffer(XrpcConstants.PAYLOAD_EXCEEDED_RESPONSE));
      ctx.flush();
      ctx.close();
      return processed;
    }

    if (endOfStream) {
      Handler handler = ctx.channel().attr(XrpcConstants.XRPC_HANDLER).get();
      if (handler == null) {
        // This is an error in our handler if it happens, since we already verified the request was
        // set, and they should always BOTH be set.
        log.error("Missing handler attribute in channel; can't process request");
        // TODO(jkinkead): Return a 500 here? It seems appropriate.
        return processed;
      }

      try {
        FullHttpResponse response = (FullHttpResponse) handler.handle(request);
        sendResponse(ctx, response, streamId);
        return processed;
      } catch (IOException e) {
        log.error("Error in handling Route", e);
        // Error
        ByteBuf buf = ctx.channel().alloc().directBuffer();
        buf.writeBytes("Error executing endpoint".getBytes(XrpcConstants.DEFAULT_CHARSET));
        writeResponse(ctx, streamId, HttpResponseStatus.INTERNAL_SERVER_ERROR, buf);
      }
    }
    return processed;
  }

  @Override
  public void onHeadersRead(
      ChannelHandlerContext ctx,
      int streamId,
      Http2Headers headers,
      int padding,
      boolean endOfStream) {

    Channel channel = ctx.channel();
    if (channel.hasAttr(XrpcConstants.XRPC_SOFT_RATE_LIMITED)) {
      writeResponse(
          ctx,
          streamId,
          HttpResponseStatus.TOO_MANY_REQUESTS,
          Unpooled.wrappedBuffer(XrpcConstants.RATE_LIMIT_RESPONSE));
      return;
    }

    // Check content-length and short-circuit the channel if it's too big.
    long contentLength = 0;
    try {
      contentLength = headers.getLong(CONTENT_LENGTH, 0);
    } catch (NumberFormatException nfe) {
      // Malformed header, ignore.
      // This isn't supposed to happen, but does; see https://github.com/netty/netty/issues/7710 .
    }

    if (contentLength > maxPayloadBytes) {
      // Friendly short-circuit if someone intends to send us a huge payload.
      writeResponse(
          ctx,
          streamId,
          HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE,
          Unpooled.wrappedBuffer(XrpcConstants.PAYLOAD_EXCEEDED_RESPONSE));
      ctx.flush();
      ctx.close();
      return;
    }

    // After headers are read, we can determine if we have a route that handles the request. If
    // there's no data expected, we immediately call the handler. Else, we'll pass the handler and
    // request through in the context.
    XrpcConnectionContext xctx = channel.attr(XrpcConstants.CONNECTION_CONTEXT).get();
    String path = getPathFromHeaders(headers);
    HttpMethod method = HttpMethod.valueOf(headers.method().toString());
    CompiledRoutes.Match match = xctx.getRoutes().match(path, method);
    XrpcRequest request = new XrpcRequest(headers, xctx.getMapper(), match.getGroups(), channel);
    if (endOfStream) {
      // No request body expected; execute handler now with empty body.
      try {
        FullHttpResponse response = (FullHttpResponse) match.getHandler().handle(request);
        sendResponse(ctx, response, streamId);
      } catch (IOException e) {
        log.error("Error in handling Route", e);
        // Error
        ByteBuf buf = channel.alloc().directBuffer();
        buf.writeBytes("Error executing endpoint".getBytes(XrpcConstants.DEFAULT_CHARSET));
        writeResponse(ctx, streamId, HttpResponseStatus.INTERNAL_SERVER_ERROR, buf);
      }
    } else {
      // Save request & handler for use when data is received.
      channel.attr(XrpcConstants.XRPC_REQUEST).set(request);
      channel.attr(XrpcConstants.XRPC_HANDLER).set(match.getHandler());
    }
  }

  @Override
  public void onHeadersRead(
      ChannelHandlerContext ctx,
      int streamId,
      Http2Headers headers,
      int streamDependency,
      short weight,
      boolean exclusive,
      int padding,
      boolean endOfStream) {
    onHeadersRead(ctx, streamId, headers, padding, endOfStream);
  }

  static String getPathFromHeaders(Http2Headers headers) {
    String uri = headers.path().toString();
    return XUrl.getPath(uri);
  }

  @Override
  public void onPriorityRead(
      ChannelHandlerContext ctx,
      int streamId,
      int streamDependency,
      short weight,
      boolean exclusive) {}

  @Override
  public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) {}

  @Override
  public void onSettingsAckRead(ChannelHandlerContext ctx) {}

  @Override
  public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) {}

  @Override
  public void onPingRead(ChannelHandlerContext ctx, ByteBuf data) {}

  @Override
  public void onPingAckRead(ChannelHandlerContext ctx, ByteBuf data) {}

  @Override
  public void onPushPromiseRead(
      ChannelHandlerContext ctx,
      int streamId,
      int promisedStreamId,
      Http2Headers headers,
      int padding) {}

  @Override
  public void onGoAwayRead(
      ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) {}

  @Override
  public void onWindowUpdateRead(
      ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) {}

  @Override
  public void onUnknownFrame(
      ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload) {}
}
