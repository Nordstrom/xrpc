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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.util.AttributeKey;

@ChannelHandler.Sharable
public class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {
  private static final AttributeKey<XrpcConnectionContext> CONNECTION_CONTEXT =
      AttributeKey.valueOf("XrpcConnectionContext");
  private static final int MAX_PAYLOAD_SIZE = 1024 * 1024; // TODO(JR): This should be configurable
  private final UrlRouter router;
  private final XrpcConnectionContext xctx;
  private final CorsConfig corsConfig;

  protected Http2OrHttpHandler(
      UrlRouter router, XrpcConnectionContext xctx, CorsConfig corsConfig) {
    super(ApplicationProtocolNames.HTTP_1_1);
    this.router = router;
    this.xctx = xctx;
    this.corsConfig = corsConfig;
  }

  @Override
  protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
    ctx.channel().attr(CONNECTION_CONTEXT).set(xctx);

    if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
      ChannelPipeline cp = ctx.pipeline();
      cp.addLast("codec", new Http2HandlerBuilder(xctx).build());
      return;
    }

    if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
      ChannelPipeline cp = ctx.pipeline();
      cp.addLast("codec", new HttpServerCodec());
      cp.addLast("aggregator", new HttpObjectAggregator(MAX_PAYLOAD_SIZE));

      if (corsConfig.isCorsSupportEnabled()) {
        cp.addLast("cors", new CorsHandler(corsConfig));
      }
      // cp.addLast("authHandler", new NoOpHandler()); // TODO(JR): OAuth2.0 Impl needed
      cp.addLast("routingFilter", router);
      return;
    }

    throw new IllegalStateException("unknown protocol: " + protocol);
  }
}
