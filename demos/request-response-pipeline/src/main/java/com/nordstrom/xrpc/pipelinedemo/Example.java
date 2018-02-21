/*
 * Copyright 2017 Nordstrom, Inc.
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

package com.nordstrom.xrpc.pipelinedemo;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableMap;
import com.nordstrom.xrpc.logging.ExceptionLogger;
import com.nordstrom.xrpc.server.IdleDisconnectHandler;
import com.nordstrom.xrpc.server.Server;
import com.nordstrom.xrpc.server.State;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.http.ApplicationCodecPlaceholderHandler;
import com.xjeffrose.xio.http.CodecPlaceholderHandler;
import com.xjeffrose.xio.http.Http2HandlerBuilder;
import com.xjeffrose.xio.http.HttpNegotiationHandler;
import com.xjeffrose.xio.http.PipelineRequestHandler;
import com.xjeffrose.xio.http.PipelineRouter;
import com.xjeffrose.xio.http.Request;
import com.xjeffrose.xio.http.Response;
import com.xjeffrose.xio.http.ResponseBuilders;
import com.xjeffrose.xio.http.Route;
import com.xjeffrose.xio.http.RouteApplicator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Example {
  public static class MyChannelInitializer extends ChannelInitializer<Channel> {
    private final State state;
    private final ImmutableMap<Route, PipelineRequestHandler> routes;

    public MyChannelInitializer(State state, ImmutableMap<Route, PipelineRequestHandler> routes) {
      this.state = state;
      this.routes = routes;
    }

    /*
     * This is a lot of boilerplate that allows us to setup a custom
     * pipeline. This should be refactored away in the future.
     */
    @Override
    public void initChannel(Channel ch) throws Exception {
      ChannelPipeline cp = ch.pipeline();
      cp.addLast(
          "idleDisconnectHandler",
          new IdleDisconnectHandler(
              state.config().readerIdleTimeout(),
              state.config().writerIdleTimeout(),
              state.config().allIdleTimeout()));
      cp.addLast("serverConnectionLimiter", state.globalConnectionLimiter());
      cp.addLast("serverRateLimiter", state.rateLimiter());

      if (state.config().enableWhiteList()) {
        cp.addLast("whiteList", state.whiteListFilter());
      } else if (state.config().enableBlackList()) {
        cp.addLast("blackList", state.blackListFilter());
      }

      cp.addLast("firewall", state.firewall());
      cp.addLast(
          "encryptionHandler", state.tls().encryptionHandler(ch.alloc())); // Add Config for Certs

      // all of the following functionality replaces this line in ServerChannelInitializer
      // cp.addLast("codec", state.h1h2());
      cp.addLast(
          "codecNegotiation",
          new HttpNegotiationHandler(() -> new Http2HandlerBuilder().server(true).build()));
      cp.addLast("codec", CodecPlaceholderHandler.INSTANCE);
      cp.addLast("application codec", ApplicationCodecPlaceholderHandler.INSTANCE);
      cp.addLast("application router", new PipelineRouter(routes));
      cp.addLast("router applicator", new RouteApplicator());

      cp.addLast("exceptionLogger", new ExceptionLogger());
    }
  }

  public static ByteBuf bb(String str) {
    return Unpooled.wrappedBuffer(str.getBytes(UTF_8));
  }

  public static void main(String[] args) {
    // Load application config from jar resources. The 'load' method below also allows supports
    // overrides from environment variables.
    Config config = ConfigFactory.load("demo.conf");

    Map<Route, PipelineRequestHandler> map = new LinkedHashMap<>();
    map.put(
        Route.build("/hello/"),
        (ChannelHandlerContext ctx, Request request, Route route) -> {
          Response response = ResponseBuilders.newOk().body(bb("world")).build();
          ctx.writeAndFlush(response);
        });
    ImmutableMap<Route, PipelineRequestHandler> routes = ImmutableMap.copyOf(map);

    // Build your server. This overrides the default configuration with values from
    // src/main/resources/demo.conf.
    Server server =
        new Server(config) {
          @Override
          public ChannelInitializer<Channel> initializer(State state) {
            return new MyChannelInitializer(state, routes);
          }
        };

    try {
      // Fire away!
      server.listenAndServe();
    } catch (IOException e) {
      log.error("Failed to start people server", e);
    }
  }
}
