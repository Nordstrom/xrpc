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

package com.nordstrom.xrpc.http;

import static com.codahale.metrics.MetricRegistry.name;
import static io.netty.channel.ChannelOption.SO_BACKLOG;
import static io.netty.channel.ChannelOption.SO_KEEPALIVE;
import static io.netty.channel.ChannelOption.TCP_NODELAY;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.logging.ExceptionLogger;
import com.nordstrom.xrpc.logging.MessageLogger;
import com.nordstrom.xrpc.tls.Tls;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.*;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

@Slf4j
public class Router {
  private final XConfig config;
  /** Format to use for worker thread names. */
  private final String workerNameFormat;

  private final int bossThreadCount;
  private final int workerThreadCount;
  // TODO(JR): Not sure why we would want to retain ordering here, but added a high
  // performance hash map impl JIC.
  private final Map<Route, Handler> routes = PlatformDependent.newConcurrentHashMap();

  private Channel channel;
  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  private Class<? extends ServerChannel> channelClass;

  // see http://metrics.dropwizard.io/3.2.2/getting-started.html for more on this
  private static final MetricRegistry metrics = new MetricRegistry();
  private final Meter requests = metrics.meter("requests");
  
  private final ConsoleReporter consoleReporter =
      ConsoleReporter.forRegistry(metrics)
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();

  final Slf4jReporter slf4jReporter =
      Slf4jReporter.forRegistry(metrics)
          .outputTo(LoggerFactory.getLogger(Router.class))
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();

  final JmxReporter jmxReporter = JmxReporter.forRegistry(metrics).build();

  private final Tls tls;

  private static ThreadFactory threadFactory(String nameFormat) {
    return new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
  }

  public Router(XConfig config) {
    this.config = config;
    workerNameFormat = config.workerNameFormat();
    bossThreadCount = config.bossThreadCount();
    workerThreadCount = config.workerThreadCount();
    tls = new Tls(config.cert(), config.key());
  }

  public void addRoute(String route, Handler handler) {
    routes.put(Route.build(route), handler);
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
    UrlRouter router = new UrlRouter();

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
            cp.addLast("encryptionHandler", tls.getEncryptionHandler(ch.alloc())); // Add Config for Certs
            //cp.addLast("messageLogger", new MessageLogger()); // TODO(JR): Do not think we need this
            cp.addLast("codec", new Http2OrHttpHandler(router));
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
                  // log.log(Level.WARNING, "Error shutting down server", future.cause());
                }
                synchronized (Router.this) {
                  // listener.serverShutdown();
                }
              }
            });
  }

  public class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {

    private static final int MAX_CONTENT_LENGTH = 1024 * 100;
    private UrlRouter router;

    protected Http2OrHttpHandler(UrlRouter router) {
      super(ApplicationProtocolNames.HTTP_1_1);
      this.router = router;
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
      if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
        log.info("Using Http/2");
        ChannelPipeline cp = ctx.pipeline();
        cp.addLast(new Http2HandlerBuilder().server(true).build());
        cp.addLast("routingFiler", router);
        return;
      }

      if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
        log.info("Using Http/1.1");
        ChannelPipeline cp = ctx.pipeline();
        cp.addLast("codec", new HttpServerCodec());
        cp.addLast("aggregator", new HttpObjectAggregator(1 * 1024 * 1024)); // Aggregate up to 1MB
        //cp.addLast("authHandler", new NoOpHandler()); // TODO(JR): OAuth2.0 Impl needed
        cp.addLast("routingFilter", router);
        return;
      }

      throw new IllegalStateException("unknown protocol: " + protocol);
    }
  }




  @ChannelHandler.Sharable
  private class UrlRouter extends ChannelDuplexHandler {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      requests.mark();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      log.info("Received Req");
      if (msg instanceof HttpRequest) {
        FullHttpRequest request = (FullHttpRequest) msg;
        String uri = request.uri();
        for (Route route : routes.keySet()) {
          Map<String, String> groups = route.groups(uri);
          if (groups != null) {
            Context context = new Context(request, groups, ctx.alloc());
            HttpResponse resp = routes.get(route).handle(context);

            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
            ctx.fireChannelRead(msg);
            return;
          }
        }
        // No matching route.
        FullHttpResponse response =
            new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        response.headers().set(CONTENT_TYPE, "text/plain");
        response.headers().setInt(CONTENT_LENGTH, 0);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
      }

      if (msg instanceof Http2Request) {
        log.info("Is a Http2 Request");
        Http2Request<Http2Headers> request = (Http2Request) msg;
        String uri = request.payload.path().toString();
        for (Route route : routes.keySet()) {
          Map<String, String> groups = route.groups(uri);
          if (groups != null) {
            log.info("Applying uri to route");
            Context context = new Context(request, groups, ctx.alloc());
            FullHttpResponse h1_resp = (FullHttpResponse) routes.get(route).handle(context);
            //ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
            Http2Headers headers = HttpConversionUtil.toHttp2Headers(h1_resp, true);
            Http2DataFrame dataFrame = new DefaultHttp2DataFrame(h1_resp.content(), true);
            
            ctx.write(Http2Response.build(request.streamId, headers));
            ctx.write(Http2Response.build(request.streamId, dataFrame)).addListener(ChannelFutureListener.CLOSE);
            ctx.fireChannelRead(msg);
            return;
          }
        }
      }

      ctx.fireChannelRead(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
      ctx.fireChannelReadComplete();
    }
  }

  @ChannelHandler.Sharable
  private static class ServiceRateLimiter extends ChannelDuplexHandler {
    private final RateLimiter limiter;
    private final Meter reqs;
    private final Timer timer;

    private static Timer.Context context;

    public ServiceRateLimiter(MetricRegistry metrics, double rateLimit) {
      this.limiter = RateLimiter.create(rateLimit);
      this.reqs = metrics.meter(name(Router.class, "requests", "Rate"));
      this.timer = metrics.timer("Request Latency");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      // TODO(JR): Should this be before or after the acquire? Do we want to know when
      //          we are limiting? Do we want to know what the actual rate of incoming
      //          requests are?
      reqs.mark();
      limiter.acquire();
      context = timer.time();

      ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      context.stop();

      ctx.fireChannelInactive();
    }
  }

  @ChannelHandler.Sharable
  private static class ConnectionLimiter extends ChannelDuplexHandler {
    private final AtomicInteger numConnections;
    private final int maxConnections;
    private final Counter connections;

    public ConnectionLimiter(MetricRegistry metrics, int maxConnections) {
      this.maxConnections = maxConnections;
      this.numConnections = new AtomicInteger(0);
      this.connections = metrics.counter(name(Router.class, "Active Connections"));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      connections.inc();

      if (maxConnections > 0) {
        if (numConnections.incrementAndGet() > maxConnections) {
          ctx.channel().close();
          // numConnections will be decremented in channelClosed
          log.info("Accepted connection above limit (" + maxConnections + "). Dropping.");
        }
      }
      ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      connections.dec();

      if (maxConnections > 0) {
        if (numConnections.decrementAndGet() < 0) {
          log.error("BUG in ConnectionLimiter");
        }
      }
      ctx.fireChannelInactive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      ctx.fireChannelRead(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
      ctx.fireChannelReadComplete();
    }
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
