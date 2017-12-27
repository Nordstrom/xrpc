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

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.*;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.RateLimiter;
import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.XrpcConstants;
import com.nordstrom.xrpc.logging.ExceptionLogger;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import com.nordstrom.xrpc.server.tls.Tls;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

@Slf4j
public class Router {
  private final String workerNameFormat;
  private final int bossThreadCount;
  private final int workerThreadCount;

  private final XConfig config;
  private final int MAX_PAYLOAD_SIZE;
  private final Tls tls;
  private final XrpcConnectionContext ctx;

  private final MetricRegistry metricRegistry = new MetricRegistry();

  final Slf4jReporter slf4jReporter =
      Slf4jReporter.forRegistry(metricRegistry)
          .outputTo(LoggerFactory.getLogger(Router.class))
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();
  final JmxReporter jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
  private final ConsoleReporter consoleReporter =
      ConsoleReporter.forRegistry(metricRegistry)
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();

  @Getter private Channel channel;
  @Getter private HealthCheckRegistry healthCheckRegistry;
  private final Map<String, HealthCheck> healthCheckMap = new ConcurrentHashMap<>();

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

    this.ctx =
        XrpcConnectionContext.builder()
            .requestMeter(metricRegistry.meter("requests"))
            .maxPayloadSize(MAX_PAYLOAD_SIZE)
            .build();

    configResponseCodeMeters();
  }

  private void configResponseCodeMeters() {
    final Map<HttpResponseStatus, String> meterNamesByStatusCode = new ConcurrentHashMap<>(6);

    // Create the proper metrics containers
    final String NAME_PREFIX = "responseCodes.";
    meterNamesByStatusCode.put(HttpResponseStatus.OK, NAME_PREFIX + "ok");
    meterNamesByStatusCode.put(HttpResponseStatus.CREATED, NAME_PREFIX + "created");
    meterNamesByStatusCode.put(HttpResponseStatus.NO_CONTENT, NAME_PREFIX + "noContent");
    meterNamesByStatusCode.put(HttpResponseStatus.BAD_REQUEST, NAME_PREFIX + "badRequest");
    meterNamesByStatusCode.put(HttpResponseStatus.NOT_FOUND, NAME_PREFIX + "notFound");
    meterNamesByStatusCode.put(
        HttpResponseStatus.TOO_MANY_REQUESTS, NAME_PREFIX + "tooManyRequests");
    meterNamesByStatusCode.put(
        HttpResponseStatus.INTERNAL_SERVER_ERROR, NAME_PREFIX + "serverError");

    for (Map.Entry<HttpResponseStatus, String> entry : meterNamesByStatusCode.entrySet()) {
      ctx.getMetersByStatusCode().put(entry.getKey(), metricRegistry.meter(entry.getValue()));
    }
  }

  public void addHealthCheck(String s, HealthCheck check) {
    Preconditions.checkState(
        !healthCheckMap.containsKey(s), "A Health Check by that name has already been registered");
    healthCheckMap.put(s, check);
  }

  public void scheduleHealthChecks(EventLoopGroup workerGroup) {
    scheduleHealthChecks(workerGroup, 60, 60, TimeUnit.SECONDS);
  }

  public void scheduleHealthChecks(
      EventLoopGroup workerGroup, int initialDelay, int delay, TimeUnit timeUnit) {

    for (Map.Entry<String, HealthCheck> entry : healthCheckMap.entrySet()) {
      healthCheckRegistry.register(entry.getKey(), entry.getValue());
    }

    workerGroup.scheduleWithFixedDelay(
        new Runnable() {
          @Override
          public void run() {
            healthCheckRegistry.runHealthChecks(workerGroup);
          }
        },
        initialDelay,
        delay,
        timeUnit);
  }

  public void addRoute(String route, Handler handler) {
    addRoute(route, handler, XHttpMethod.ANY);
  }

  public void addRoute(String route, Handler handler, HttpMethod method) {
    Preconditions.checkState(!route.isEmpty());
    Preconditions.checkState(handler != null);
    Preconditions.checkState(method != null);

    ImmutableMap<XHttpMethod, Handler> handlerMap =
        new ImmutableMap.Builder<XHttpMethod, Handler>()
            .put(new XHttpMethod(method.name()), handler)
            .build();

    Route _route = Route.build(route);
    Optional<ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>>> routesOptional =
        Optional.ofNullable(ctx.getRoutes().get());

    if (routesOptional.isPresent()) {

      if (routesOptional.get().containsKey(_route)) {
        ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>> _routes =
            routesOptional.get();
        _routes.get(_route).add(handlerMap);
        ctx.getRoutes().set(_routes);

      } else {
        ImmutableSortedMap.Builder<Route, List<ImmutableMap<XHttpMethod, Handler>>> routeMap =
            new ImmutableSortedMap.Builder<Route, List<ImmutableMap<XHttpMethod, Handler>>>(
                    Ordering.usingToString())
                .put(Route.build(route), Lists.newArrayList(handlerMap));

        routesOptional.map(value -> routeMap.putAll(value.descendingMap()));

        ctx.getRoutes().set(routeMap.build());
      }
    } else {
      ImmutableSortedMap.Builder<Route, List<ImmutableMap<XHttpMethod, Handler>>> routeMap =
          new ImmutableSortedMap.Builder<Route, List<ImmutableMap<XHttpMethod, Handler>>>(
                  Ordering.usingToString())
              .put(Route.build(route), Lists.newArrayList(handlerMap));

      ctx.getRoutes().set(routeMap.build());
    }
  }

  public AtomicReference<ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>>>
      getRoutes() {

    return ctx.getRoutes();
  }

  public MetricRegistry getMetricRegistry() {
    return metricRegistry;
  }

  public void serveAdmin() {
    MetricsModule metricsModule = new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, true);
    ObjectMapper metricsMapper = new ObjectMapper().registerModule(metricsModule);
    ObjectMapper healthMapper = new ObjectMapper();

    addRoute("/admin", AdminHandlers.adminHandler(), HttpMethod.GET);
    addRoute("/ping", AdminHandlers.pingHandler(), HttpMethod.GET);
    addRoute(
        "/health",
        AdminHandlers.healthCheckHandler(healthCheckRegistry, healthMapper),
        HttpMethod.GET);
    addRoute(
        "/metrics", AdminHandlers.metricsHandler(metricRegistry, metricsMapper), HttpMethod.GET);
  }

  public void listenAndServe() throws IOException {
    listenAndServe(true, true);
  }

  /**
   * The listenAndServe method is the primary entry point for the server and should only be called
   * once and only from the main thread.
   *
   * @param serveAdmin pass true to serve the admin handlers, false if not
   * @param scheduleHealthChecks pass true to schedule periodic health checks, otherwise, the health
   *     checks will be run every time the health endpoint is hit
   * @throws IOException throws in the event the network services, as specified, cannot be accessed
   */
  public void listenAndServe(boolean serveAdmin, boolean scheduleHealthChecks) throws IOException {
    ConnectionLimiter globalConnectionLimiter =
        new ConnectionLimiter(
            metricRegistry, config.maxConnections()); // All endpoints for a given service
    ServiceRateLimiter rateLimiter =
        new ServiceRateLimiter(
            metricRegistry,
            config.softReqPerSec(),
            config.hardReqPerSec()); // RateLimit incomming connections in terms of req / second

    ServerBootstrap b =
        XrpcBootstrapFactory.buildBootstrap(bossThreadCount, workerThreadCount, workerNameFormat);
    UrlRouter router = new UrlRouter();
    Http2OrHttpHandler h1h2 = new Http2OrHttpHandler(router, ctx);

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

    if (scheduleHealthChecks) {
      final EventLoopGroup _workerGroup = b.config().childGroup();
      healthCheckRegistry = new HealthCheckRegistry(_workerGroup);
      scheduleHealthChecks(_workerGroup);
    }

    if (serveAdmin) {
      serveAdmin();
    }

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
                  //TODO(JR): We should probably be more thoughtful here.
                  shutdown();
                }
              }
            });
  }

  /**
   * This section of classes have been added as Static inner classes to reduce GC pressure. As these
   * classes are only instantiated once and only from the Router class, this seems to be a more
   * appropriate class def than these objects living in their own public classes.
   */
  @ChannelHandler.Sharable
  static class ConnectionLimiter extends ChannelDuplexHandler {
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

      //TODO(JR): Should this return a 429 or is the current logic of silently dropping the connection sufficient?
      if (maxConnections > 0) {
        if (numConnections.incrementAndGet() > maxConnections) {
          ctx.channel().close();
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
  static class ServiceRateLimiter extends ChannelDuplexHandler {
    private final RateLimiter softLimiter;
    private final RateLimiter hardLimiter;
    private final Meter reqs;
    private final Timer timer;

    private Timer.Context context;

    public ServiceRateLimiter(MetricRegistry metrics, double softRateLimit, double hardRateLimit) {
      this.softLimiter = RateLimiter.create(softRateLimit);
      this.hardLimiter = RateLimiter.create(hardRateLimit);
      this.reqs = metrics.meter(name(Router.class, "requests", "Rate"));
      this.timer = metrics.timer("Request Latency");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      reqs.mark();

      double timeSlept = hardLimiter.acquire();
      log.debug("Hard Rate limit fired and slept for " + timeSlept + " seconds");

      if (!softLimiter.tryAcquire()) {
        ctx.channel().attr(XrpcConstants.XRPC_RATE_LIMIT).set(Boolean.TRUE);
      }

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
  static class IdleDisconnectHandler extends IdleStateHandler {

    public IdleDisconnectHandler(
        int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
      super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
      ctx.channel().close();
    }
  }

  @ChannelHandler.Sharable
  static class NoOpHandler extends ChannelDuplexHandler {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      ctx.pipeline().remove(this);
      ctx.fireChannelActive();
    }
  }
}
