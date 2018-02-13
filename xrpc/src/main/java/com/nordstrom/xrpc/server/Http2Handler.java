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
  Http2Handler(
      Http2ConnectionDecoder decoder,
      Http2ConnectionEncoder encoder,
      Http2Settings initialSettings) {
    super(decoder, encoder, initialSettings);
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
    int processed = data.readableBytes() + padding;

    if (endOfStream) {
      // TODO(jkinkead): Will this trigger on a not found with a request body?
      XrpcRequest request = ctx.channel().attr(XrpcConstants.XRPC_REQUEST).get();
      request.setData(data);
      Handler handler = ctx.channel().attr(XrpcConstants.XRPC_HANDLER).get();
      try {
        FullHttpResponse response = (FullHttpResponse) handler.handle(request);
        sendResponse(ctx, response, streamId);
        return processed;
      } catch (Exception e) {
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
          XrpcConstants.RATE_LIMIT_RESPONSE.retain());

      return;
    }

    // After headers are read, we can determine if we have a route that handles the request. If
    // there is no handler, we short-circuit 404 and drop data. Otherwise, we'll pass through to
    // the handler iff there is no data expected.
    XrpcConnectionContext xctx = channel.attr(XrpcConstants.CONNECTION_CONTEXT).get();
    String path = getPathFromHeaders(headers);
    HttpMethod method = HttpMethod.valueOf(headers.method().toString());
    CompiledRoutes.Match match = xctx.getRoutes().match(path, method);
    XrpcRequest request =
        new XrpcRequest(headers, xctx.getMapper(), match.getGroups(), channel, streamId);
    Optional<CharSequence> contentLength = Optional.ofNullable(headers.get(CONTENT_LENGTH));
    if (!contentLength.isPresent()) {
      // No request body expected; execute handler now with empty body.
      try {
        FullHttpResponse response = (FullHttpResponse) match.getHandler().handle(request);
        sendResponse(ctx, response, streamId);
      } catch (Exception e) {
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
