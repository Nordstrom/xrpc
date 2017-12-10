package com.nordstrom.xrpc.server;

import com.codahale.metrics.Meter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.*;
import io.netty.handler.logging.LogLevel;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class Http2HandlerBuilder
    extends AbstractHttp2ConnectionHandlerBuilder<Http2Handler, Http2HandlerBuilder> {

  private final Http2FrameLogger logger = new Http2FrameLogger(LogLevel.INFO, Http2Handler.class);
  private final AtomicReference<ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>>>
      routes;
  private final Meter requests;
  private ConcurrentHashMap<HttpResponseStatus, Meter> metersByStatusCode;

  public Http2HandlerBuilder(
      AtomicReference<ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>>> routes,
      Meter requests,
      ConcurrentHashMap<HttpResponseStatus, Meter> metersByStatusCode) {
    this.routes = routes;
    this.requests = requests;
    this.metersByStatusCode = metersByStatusCode;
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
    Http2Handler handler =
        new Http2Handler(requests, routes, metersByStatusCode, decoder, encoder, initialSettings);
    frameListener(handler);
    return handler;
  }
}
