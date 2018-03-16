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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.nordstrom.xrpc.XrpcConstants;
import com.nordstrom.xrpc.client.XUrl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2EventAdapter;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Http2Handler extends Http2EventAdapter {
  /**
   * Map of stream ID to the request sent on the stream. Entries are created when headers are read
   * and a handler is matched.
   */
  @VisibleForTesting final IntObjectMap<XrpcRequest> requests = new IntObjectHashMap<>();
  /**
   * Map of stream ID to the handler for the request. Entries are created when headers are read and
   * a handler is matched.
   */
  @VisibleForTesting final IntObjectMap<Handler> handlers = new IntObjectHashMap<>();

  /** The encoder to write response data to. */
  private final Http2ConnectionEncoder encoder;

  /** The maximum total data payload to accept. */
  private final int maxPayloadBytes;

  /** Helper for a CORS request. */
  private final Http2CorsHandler corsHandler;

  Http2Handler(Http2ConnectionEncoder encoder, int maxPayloadBytes, Http2CorsHandler corsHandler) {
    this.encoder = encoder;
    this.maxPayloadBytes = maxPayloadBytes;
    this.corsHandler = corsHandler;

    encoder.connection().addListener(this);
  }

  /** Marks the meter for the given response status in the given connection context. */
  private void markResponseStatus(ChannelHandlerContext ctx, HttpResponseStatus status) {
    ServerContext xctx = ctx.channel().attr(ServerContext.ATTRIBUTE_KEY).get();
    // TODO(jkinkead): Per issue #152, this should track ALL response codes.
    Meter meter = xctx.metersByStatusCode().get(status);
    if (meter != null) {
      meter.mark();
    }
  }

  /**
   * Writes the given response data to the given stream. Closes the stream after writing the
   * response.
   */
  private void writeResponse(
      final ChannelHandlerContext ctx,
      final int streamId,
      Http2Headers headers,
      Optional<ByteBuf> bodyOpt) {
    corsHandler.outbound(headers);
    encoder.writeHeaders(ctx, streamId, headers, 0, !bodyOpt.isPresent(), ctx.newPromise());
    bodyOpt.ifPresent(body -> encoder.writeData(ctx, streamId, body, 0, true, ctx.newPromise()));
  }

  /**
   * Writes the given HTTP/1 response to the given stream. Marks the response status metric. Closes
   * the stream after writing the response.
   */
  private void writeResponse(ChannelHandlerContext ctx, int streamId, HttpResponse h1Response) {
    markResponseStatus(ctx, h1Response.status());

    // Convert and validate headers.
    Http2Headers headers = HttpConversionUtil.toHttp2Headers(h1Response, true);

    Optional<ByteBuf> body = Optional.empty();
    if (h1Response instanceof FullHttpResponse) {
      ByteBuf content = ((FullHttpResponse) h1Response).content();
      if (content.readableBytes() > 0) {
        body = Optional.of(content);
      }
    }

    writeResponse(ctx, streamId, headers, body);
  }

  /**
   * Writes the given response body as "text/plain" to the given stream. Marks the response status
   * metric. Closes the stream after writing the response.
   */
  private void writeResponse(
      ChannelHandlerContext ctx, int streamId, HttpResponseStatus status, ByteBuf body) {

    Preconditions.checkArgument(body != null, "body must not be null");

    markResponseStatus(ctx, status);

    Http2Headers headers = new DefaultHttp2Headers(true);
    // TODO(jkinkead): This should honor accept headers; we shouldn't send text/plain if the client
    // doesn't want it.
    headers.set(CONTENT_TYPE, "text/plain");
    headers.setInt(CONTENT_LENGTH, body.readableBytes());
    headers.status(status.codeAsText());

    writeResponse(ctx, streamId, headers, Optional.of(body));
  }

  @Override
  public int onDataRead(
      ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) {

    XrpcRequest request = requests.get(streamId);

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

      Optional<HttpResponse> corsResponse = corsHandler.inbound(request.h2Headers(), streamId);
      if (corsResponse.isPresent()) {
        writeResponse(ctx, streamId, corsResponse.get());
        return processed;
      }

      Handler handler = handlers.get(streamId);
      try {
        HttpResponse response = handler.handle(request);
        writeResponse(ctx, streamId, response);
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
    ServerContext xctx = channel.attr(ServerContext.ATTRIBUTE_KEY).get();

    // Check if this is a new stream. This should either be a new stream, or be the set of
    // trailer-headers for a finished request.
    XrpcRequest request = requests.get(streamId);

    // Mark the request counter if this is a new stream.
    if (request == null) {
      xctx.requestMeter().mark();
    }

    // Rate limit if requested.
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

    // Find the request handler. If we found a request for the stream already, the handler will be
    // in our handler map.
    Handler handler;
    if (request == null) {
      // Determine the handler for the request's path.
      String path = getPathFromHeaders(headers);
      CompiledRoutes.Match match = xctx.routes().match(path, headers.method().toString());
      request = new XrpcRequest(headers, xctx, match.getGroups(), channel);
      handler = match.getHandler();
    } else {
      // Add the new headers to the request.
      request.h2Headers().add(headers);
      handler = handlers.get(streamId);
    }

    // If there's no data expected, call the handler. Else, pass the handler and request through in
    // the context.
    if (endOfStream) {
      // Handle CORS.
      Optional<HttpResponse> corsResponse = corsHandler.inbound(headers, streamId);
      if (corsResponse.isPresent()) {
        writeResponse(ctx, streamId, corsResponse.get());
        return;
      }

      try {
        HttpResponse response = handler.handle(request);

        writeResponse(ctx, streamId, response);
      } catch (IOException e) {
        log.error("Error in handling Route", e);
        // Error
        ByteBuf buf = channel.alloc().directBuffer();
        buf.writeBytes("Error executing endpoint".getBytes(XrpcConstants.DEFAULT_CHARSET));
        writeResponse(ctx, streamId, HttpResponseStatus.INTERNAL_SERVER_ERROR, buf);
      }
    } else {
      // Save request & handler to use when the stream is ended.
      // Note that per the HTTP/2 protocol, endOfStream MUST have been set if this is a
      // trailer-part.
      requests.put(streamId, request);
      handlers.put(streamId, handler);
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
    // Ignore stream priority.
    onHeadersRead(ctx, streamId, headers, padding, endOfStream);
  }

  static String getPathFromHeaders(Http2Headers headers) {
    String uri = headers.path().toString();
    return XUrl.path(uri);
  }

  /** Removes all requests and handlers for a given stream. */
  @Override
  public void onStreamRemoved(Http2Stream stream) {
    int id = stream.id();
    requests.remove(id);
    handlers.remove(id);
  }
}
