package com.nordstrom.xrpc.server;

import com.codahale.metrics.Meter;
import com.google.common.collect.ImmutableSortedMap;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import io.netty.handler.codec.http2.*;
import io.netty.handler.logging.LogLevel;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class Http2HandlerBuilder
    extends AbstractHttp2ConnectionHandlerBuilder<Http2Handler, Http2HandlerBuilder> {

  private final Http2FrameLogger logger = new Http2FrameLogger(LogLevel.INFO, Http2Handler.class);
  private final AtomicReference<ImmutableSortedMap<Route, Map<XHttpMethod, Handler>>> routes;
  private final Meter requests;

  public Http2HandlerBuilder(
      AtomicReference<ImmutableSortedMap<Route, Map<XHttpMethod, Handler>>> routes,
      Meter requests) {
    this.routes = routes;
    this.requests = requests;
    frameLogger(logger);
  }

  @Override
  public Http2Handler build() {
    return super.build();
  }

  @Override
  protected Http2Handler build(
      Http2ConnectionDecoder decoder,
      Http2ConnectionEncoder encoder,
      Http2Settings initialSettings) {
    Http2Handler handler = new Http2Handler(requests, routes, decoder, encoder, initialSettings);
    frameListener(handler);
    return handler;
  }
}
