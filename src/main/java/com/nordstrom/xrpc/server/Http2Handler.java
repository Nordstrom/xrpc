package com.nordstrom.xrpc.server;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import com.codahale.metrics.Meter;
import com.google.common.collect.ImmutableSortedMap;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.*;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Http2Handler extends Http2ConnectionHandler implements Http2FrameListener {

  private final Meter requests;
  private final AtomicReference<ImmutableSortedMap<Route, Map<XHttpMethod, Handler>>> routes;
  private XrpcRequest xrpcRequest;

  Http2Handler(
      Meter requests,
      AtomicReference<ImmutableSortedMap<Route, Map<XHttpMethod, Handler>>> routes,
      Http2ConnectionDecoder decoder,
      Http2ConnectionEncoder encoder,
      Http2Settings initialSettings) {
    super(decoder, encoder, initialSettings);
    this.requests = requests;
    this.routes = routes;
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
    requests.mark();
  }

  private void executeHandler(ChannelHandlerContext ctx, int streamId, Route route)
      throws IOException {
    XHttpMethod methodName =
        routes
            .get()
            .get(route)
            .keySet()
            .stream()
            .filter(
                m ->
                    m.compareTo(XHttpMethod.valueOf(xrpcRequest.getH2Headers().method().toString()))
                        == 0)
            .findFirst()
            .orElse(XHttpMethod.ANY);

    FullHttpResponse h1Resp =
        (FullHttpResponse) routes.get().get(route).get(methodName).handle(xrpcRequest);
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
  }

  @Override
  public int onDataRead(
      ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) {
    int processed = data.readableBytes() + padding;

    if (endOfStream) {
      xrpcRequest.setData(data);
      for (Route route : routes.get().descendingKeySet()) {
        Optional<Map<String, String>> groups =
            Optional.ofNullable(route.groups(xrpcRequest.getH2Headers().path().toString()));
        if (groups.isPresent()) {
          try {
            executeHandler(ctx, streamId, route);
          } catch (IOException e) {
            log.error("Error in handling Route", e);
            // Error
            ByteBuf buf = ctx.channel().alloc().directBuffer();
            buf.writeBytes("Error executing endpoint".getBytes());
            writeResponse(ctx, streamId, HttpResponseStatus.INTERNAL_SERVER_ERROR, buf);
          }
        }
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

    String uri = headers.path().toString();
    for (Route route : routes.get().descendingKeySet()) {
      Optional<Map<String, String>> groups = Optional.ofNullable(route.groups(uri));
      if (groups.isPresent()) {
        xrpcRequest = new XrpcRequest(headers, groups.get(), ctx.channel(), streamId);
        Optional<CharSequence> contentLength = Optional.ofNullable(headers.get("content-length"));
        if (!contentLength.isPresent()) {
          try {
            executeHandler(ctx, streamId, route);
            return;
          } catch (IOException e) {
            log.error("Error in handling Route", e);
            // Error
            ByteBuf buf = ctx.channel().alloc().directBuffer();
            buf.writeBytes("Error executing endpoint".getBytes());
            writeResponse(ctx, streamId, HttpResponseStatus.INTERNAL_SERVER_ERROR, buf);
          }
        }
      }
    }
    // No Valid Route
    ByteBuf buf = ctx.channel().alloc().directBuffer();
    buf.writeBytes("Endpoint not found".getBytes());
    writeResponse(ctx, streamId, HttpResponseStatus.NOT_FOUND, buf);
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
