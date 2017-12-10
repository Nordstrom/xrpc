package com.nordstrom.xrpc.server;

import com.codahale.metrics.Meter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@ChannelHandler.Sharable
public class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {
  private static final int MAX_PAYLOAD_SIZE =
      1 * 1024 * 1024; //TODO(JR): This should be configurable
  private final UrlRouter router;
  private final AtomicReference<ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>>>
      routes;
  private final Meter requests;
  private ConcurrentHashMap<HttpResponseStatus, Meter> metersByStatusCode;

  protected Http2OrHttpHandler(
      UrlRouter router,
      AtomicReference<ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>>> routes,
      Meter requests,
      ConcurrentHashMap<HttpResponseStatus, Meter> metersByStatusCode) {
    super(ApplicationProtocolNames.HTTP_1_1);
    this.router = router;
    this.routes = routes;
    this.requests = requests;
    this.metersByStatusCode = metersByStatusCode;
  }

  @Override
  protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
    if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
      ChannelPipeline cp = ctx.pipeline();
      cp.addLast("codec", new Http2HandlerBuilder(routes, requests, metersByStatusCode).build());
      return;
    }

    if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
      ChannelPipeline cp = ctx.pipeline();
      cp.addLast("codec", new HttpServerCodec());
      cp.addLast("aggregator", new HttpObjectAggregator(MAX_PAYLOAD_SIZE));
      //cp.addLast("authHandler", new NoOpHandler()); // TODO(JR): OAuth2.0 Impl needed
      cp.addLast("routingFilter", router);
      return;
    }

    throw new IllegalStateException("unknown protocol: " + protocol);
  }
}
