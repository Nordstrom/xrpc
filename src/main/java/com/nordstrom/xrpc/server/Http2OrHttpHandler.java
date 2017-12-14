package com.nordstrom.xrpc.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

@ChannelHandler.Sharable
public class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {
  private static final int MAX_PAYLOAD_SIZE =
      1 * 1024 * 1024; //TODO(JR): This should be configurable
  private final UrlRouter router;
  private final XrpcChannelContext xctx;

  protected Http2OrHttpHandler(UrlRouter router, XrpcChannelContext xctx) {
    super(ApplicationProtocolNames.HTTP_1_1);
    this.router = router;
    this.xctx = xctx;
  }

  @Override
  protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
    if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
      ChannelPipeline cp = ctx.pipeline();
      cp.addLast("codec", new Http2HandlerBuilder(xctx).build());
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
