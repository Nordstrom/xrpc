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

package com.nordstrom.xrpc.server;

import static io.netty.channel.ChannelOption.*;

import com.codahale.metrics.*;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.logging.ExceptionLogger;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.tls.Tls;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

@Slf4j
public class Router {
  // see http://metrics.dropwizard.io/3.2.2/getting-started.html for more on this
  private static final MetricRegistry metrics = new MetricRegistry();
  final Slf4jReporter slf4jReporter =
      Slf4jReporter.forRegistry(metrics)
          .outputTo(LoggerFactory.getLogger(Router.class))
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();
  final JmxReporter jmxReporter = JmxReporter.forRegistry(metrics).build();
  private final XConfig config;
  /** Format to use for worker thread names. */
  private final String workerNameFormat;

  private final int bossThreadCount;
  private final int workerThreadCount;
  private final AtomicReference<ImmutableSortedMap<Route, Handler>> routes =
      new AtomicReference<>();
  private final int MAX_PAYLOAD_SIZE;
  private final Meter requests = metrics.meter("requests");
  private final ConsoleReporter consoleReporter =
      ConsoleReporter.forRegistry(metrics)
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();
  private final Tls tls;
  @Getter
  private Channel channel;
  private EventLoopGroup bossGroup;
  @Getter
  private EventLoopGroup workerGroup;
  private Class<? extends ServerChannel> channelClass;

  public Router(XConfig config) {
    this(config, 1 * 1024 * 1024);
  }

  public Router(XConfig config, int maxPayload) {
    this.config = config;
    this.workerNameFormat = config.workerNameFormat();
    this.bossThreadCount = config.bossThreadCount();
    this.workerThreadCount = config.workerThreadCount();
    this.tls = new Tls(config.cert(), config.key());
    this.MAX_PAYLOAD_SIZE = maxPayload;
  }

  private static ThreadFactory threadFactory(String nameFormat) {
    return new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
  }

  public void addRoute(String route, Handler handler) {
    ImmutableSortedMap.Builder<Route, Handler> routeMap =
        new ImmutableSortedMap.Builder<Route, Handler>(Ordering.usingToString())
            .put(Route.build(route), handler);

    Optional<ImmutableSortedMap<Route, Handler>> routesOptional = Optional.ofNullable(routes.get());

    routesOptional.map(value -> routeMap.putAll(value.descendingMap()));
    routes.set(routeMap.build());
  }

  public AtomicReference<ImmutableSortedMap<Route, Handler>> getRoutes() {

    return routes;
  }

  public MetricRegistry getMetrics() {
    return metrics;
  }

  public void listenAndServe() throws IOException {
    ConnectionLimiter globalConnectionLimiter =
        new ConnectionLimiter(
            metrics, config.maxConnections()); // All endpoints for a given service
    ServiceRateLimiter rateLimiter =
        new ServiceRateLimiter(
            metrics,
            config.rateLimit()); // RateLimit incomming connections in terms of req / second

    ServerBootstrap b = new ServerBootstrap();
    UrlRouter router = new UrlRouter(routes, requests);
    Http2OrHttpHandler h1h2 = new Http2OrHttpHandler(router, routes, requests);

    if (Epoll.isAvailable()) {
      bossGroup = new EpollEventLoopGroup(bossThreadCount, threadFactory(workerNameFormat));
      workerGroup = new EpollEventLoopGroup(workerThreadCount, threadFactory(workerNameFormat));
      channelClass = EpollServerSocketChannel.class;
    } else {
      bossGroup = new NioEventLoopGroup(bossThreadCount, threadFactory(workerNameFormat));
      workerGroup = new NioEventLoopGroup(workerThreadCount, threadFactory(workerNameFormat));
      channelClass = NioServerSocketChannel.class;

      b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
      b.option(SO_BACKLOG, 1024);
      b.childOption(SO_KEEPALIVE, true);
      b.childOption(TCP_NODELAY, true);
    }

    b.group(bossGroup, workerGroup);
    b.channel(channelClass);
    b.childHandler(
        new ChannelInitializer<Channel>() {
          @Override
          public void initChannel(Channel ch) throws Exception {
            ChannelPipeline cp = ch.pipeline();
            cp.addLast("serverConnectionLimiter", globalConnectionLimiter);
            cp.addLast("serverRateLimiter", rateLimiter);
            cp.addLast(
                "encryptionHandler", tls.getEncryptionHandler(ch.alloc())); // Add Config for Certs
            //cp.addLast("messageLogger", new MessageLogger()); // TODO(JR): Do not think we need this
            cp.addLast("codec", h1h2);
            cp.addLast(
                "idleDisconnectHandler",
                new IdleDisconnectHandler(
                    config.readerIdleTimeout(),
                    config.writerIdleTimeout(),
                    config.allIdleTimeout()));
            cp.addLast("exceptionLogger", new ExceptionLogger());
          }
        });

    ChannelFuture future = b.bind(new InetSocketAddress(config.port()));

    try {
      // Get some loggy logs
      consoleReporter.start(30, TimeUnit.SECONDS);
      // This is too noisy right now, re-enable prior to shipping.
      //slf4jReporter.start(30, TimeUnit.SECONDS);
      jmxReporter.start();

      future.await();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted waiting for bind");
    }

    if (!future.isSuccess()) {
      throw new IOException("Failed to bind", future.cause());
    }

    channel = future.channel();
  }

  public void shutdown() {
    if (channel == null || !channel.isOpen()) {
      return;
    }

    channel
        .close()
        .addListener(
            new ChannelFutureListener() {
              @Override
              public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                  log.warn("Error shutting down server", future.cause());
                }
                synchronized (Router.this) {
                  // listener.serverShutdown();
                }
              }
            });
  }

  @ChannelHandler.Sharable
  class NoOpHandler extends ChannelDuplexHandler {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      ctx.pipeline().remove(this);
      ctx.fireChannelActive();
    }
  }

  class IdleDisconnectHandler extends IdleStateHandler {

    public IdleDisconnectHandler(
        int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
      super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
      ctx.channel().close();
    }
  }
}
