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
import static io.netty.channel.ChannelOption.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import com.codahale.metrics.*;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.logging.ExceptionLogger;
import com.nordstrom.xrpc.tls.Tls;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
  private Channel channel;
  private EventLoopGroup bossGroup;
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

  protected AtomicReference<ImmutableSortedMap<Route, Handler>> getRoutes() {

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
    UrlRouter router = new UrlRouter();
    Http2OrHttpHandler h1h2 = new Http2OrHttpHandler(router);

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
  private static class ServiceRateLimiter extends ChannelDuplexHandler {
    private static Timer.Context context;
    private final RateLimiter limiter;
    private final Meter reqs;
    private final Timer timer;

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
  public class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {
    private final UrlRouter router;

    protected Http2OrHttpHandler(UrlRouter router) {
      super(ApplicationProtocolNames.HTTP_1_1);
      this.router = router;
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
      if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
        ChannelPipeline cp = ctx.pipeline();
        cp.addLast("codec", new Http2HandlerBuilder().build());
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

  public final class Http2HandlerBuilder
      extends AbstractHttp2ConnectionHandlerBuilder<Http2Handler, Http2HandlerBuilder> {

    private final Http2FrameLogger logger = new Http2FrameLogger(LogLevel.INFO, Http2Handler.class);

    public Http2HandlerBuilder() {
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
      Http2Handler handler = new Http2Handler(decoder, encoder, initialSettings);
      frameListener(handler);
      return handler;
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
      if (msg instanceof HttpRequest) {
        FullHttpRequest request = (FullHttpRequest) msg;
        String uri = request.uri();
        for (Route route : routes.get().descendingKeySet()) {
          Optional<Map<String, String>> groups = Optional.ofNullable(route.groups(uri));
          if (groups.isPresent()) {
            XrpcRequest xrpcRequest = new XrpcRequest(request, groups.get(), ctx.alloc());
            HttpResponse resp = routes.get().get(route).handle(xrpcRequest);

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

  public final class Http2Handler extends Http2ConnectionHandler implements Http2FrameListener {

    private XrpcRequest xrpcRequest;

    Http2Handler(
        Http2ConnectionDecoder decoder,
        Http2ConnectionEncoder encoder,
        Http2Settings initialSettings) {
      super(decoder, encoder, initialSettings);
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

    private void writeResponse(ChannelHandlerContext ctx, int streamId, Route route)
        throws IOException {
      FullHttpResponse h1Resp = (FullHttpResponse) routes.get().get(route).handle(xrpcRequest);
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

        //TODO(JR): This is terrible and should be changed prior to real use
        // Specifically, we should not have to re-cycle over the routes again
        for (Route route : routes.get().descendingKeySet()) {
          Optional<Map<String, String>> groups =
              Optional.ofNullable(route.groups(xrpcRequest.getH2Headers().path().toString()));
          if (groups.isPresent()) {
            try {
              writeResponse(ctx, streamId, route);
            } catch (IOException e) {
              log.error("Error in handling Route", e);
              //TODO(JR): Should we return a 500 here?
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
          xrpcRequest = new XrpcRequest(headers, groups.get(), ctx.alloc(), streamId);
          Optional<CharSequence> contentLength = Optional.ofNullable(headers.get("content-length"));
          if (!contentLength.isPresent()) {
            try {
              writeResponse(ctx, streamId, route);
            } catch (IOException e) {
              log.error("Error in handling Route", e);
              //TODO(JR): Should we return a 500 here?
            }
          }
        }
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
        ChannelHandlerContext ctx,
        byte frameType,
        int streamId,
        Http2Flags flags,
        ByteBuf payload) {}
  }
}
