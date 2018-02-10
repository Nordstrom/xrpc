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

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import com.nordstrom.xrpc.server.tls.Tls;
import com.typesafe.config.Config;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

@Slf4j
public class Router {
  private final XConfig config;
  private final Tls tls;
  private final XrpcConnectionContext ctx;

  private final MetricRegistry metricRegistry = new MetricRegistry();

  @Getter private Channel channel;
  @Getter private final HealthCheckRegistry healthCheckRegistry;

  public Router(Config config) {
    this(new XConfig(config));
  }

  public Router(XConfig config) {
    this.config = config;
    this.tls = new Tls(config.cert(), config.key());
    this.healthCheckRegistry = new HealthCheckRegistry(config.asyncHealthCheckThreadCount());

    XrpcConnectionContext.Builder contextBuilder =
        XrpcConnectionContext.builder()
            .requestMeter(metricRegistry.meter("requests"))
            .mapper(new ObjectMapper());
    addResponseCodeMeters(contextBuilder);

    this.ctx = contextBuilder.build();
  }

  /** Adds a meter for all HTTP response codes to the given XrpcConnectionContext. */
  private void addResponseCodeMeters(XrpcConnectionContext.Builder contextBuilder) {
    Map<HttpResponseStatus, String> namesByCode = new HashMap<>();
    namesByCode.put(HttpResponseStatus.OK, "ok");
    namesByCode.put(HttpResponseStatus.CREATED, "created");
    namesByCode.put(HttpResponseStatus.ACCEPTED, "accepted");
    namesByCode.put(HttpResponseStatus.NO_CONTENT, "noContent");
    namesByCode.put(HttpResponseStatus.BAD_REQUEST, "badRequest");
    namesByCode.put(HttpResponseStatus.UNAUTHORIZED, "unauthorized");
    namesByCode.put(HttpResponseStatus.FORBIDDEN, "forbidden");
    namesByCode.put(HttpResponseStatus.NOT_FOUND, "notFound");
    namesByCode.put(HttpResponseStatus.TOO_MANY_REQUESTS, "tooManyRequests");
    namesByCode.put(HttpResponseStatus.INTERNAL_SERVER_ERROR, "serverError");

    // Create the proper metrics containers.
    for (Map.Entry<HttpResponseStatus, String> entry : namesByCode.entrySet()) {
      String meterName = name("responseCodes", entry.getValue());
      contextBuilder.meterByStatusCode(entry.getKey(), metricRegistry.meter(meterName));
    }
  }

  public void addHealthCheck(String name, HealthCheck check) {
    healthCheckRegistry.register(name, check);
  }

  public void scheduleHealthChecks(EventLoopGroup workerGroup) {
    scheduleHealthChecks(workerGroup, 60, 60, TimeUnit.SECONDS);
  }

  public void scheduleHealthChecks(
      EventLoopGroup workerGroup, int initialDelay, int delay, TimeUnit timeUnit) {

    workerGroup.scheduleWithFixedDelay(
        () -> healthCheckRegistry.runHealthChecks(workerGroup), initialDelay, delay, timeUnit);
  }

  /** Binds a handler for GET requests to the given route. */
  public void get(String route, Handler handler) {
    addRoute(route, handler, XHttpMethod.GET);
  }

  /** Binds a handler for POST requests to the given route. */
  public void post(String route, Handler handler) {
    addRoute(route, handler, XHttpMethod.POST);
  }

  /** Binds a handler for PUT requests to the given route. */
  public void put(String route, Handler handler) {
    addRoute(route, handler, XHttpMethod.PUT);
  }

  /** Binds a handler for DELETE requests to the given route. */
  public void delete(String route, Handler handler) {
    addRoute(route, handler, XHttpMethod.DELETE);
  }

  /** Binds a handler for HEAD requests to the given route. */
  public void head(String route, Handler handler) {
    addRoute(route, handler, XHttpMethod.HEAD);
  }

  /** Binds a handler for OPTIONS requests to the given route. */
  public void options(String route, Handler handler) {
    addRoute(route, handler, XHttpMethod.OPTIONS);
  }

  /** Binds a handler for PATCH requests to the given route. */
  public void patch(String route, Handler handler) {
    addRoute(route, handler, XHttpMethod.PATCH);
  }

  /** Binds a handler for TRACE requests to the given route. */
  public void trace(String route, Handler handler) {
    addRoute(route, handler, XHttpMethod.TRACE);
  }

  /** Binds a handler for CONNECT requests to the given route. */
  public void connect(String route, Handler handler) {
    addRoute(route, handler, XHttpMethod.CONNECT);
  }

  /** Binds a handler for ANY requests to the given route. */
  public void any(String route, Handler handler) {
    addRoute(route, handler, XHttpMethod.ANY);
  }

  @Deprecated
  public void addRoute(String route, Handler handler) {
    addRoute(route, handler, XHttpMethod.ANY);
  }

  public void addRoute(String routePattern, Handler handler, HttpMethod method) {
    Preconditions.checkState(!routePattern.isEmpty());
    Preconditions.checkState(handler != null);
    Preconditions.checkState(method != null);

    ImmutableMap<XHttpMethod, Handler> handlerMap =
        new ImmutableMap.Builder<XHttpMethod, Handler>()
            .put(new XHttpMethod(method.name()), handler)
            .build();

    Route route = Route.build(routePattern);
    Optional<ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>>> routesOptional =
        Optional.ofNullable(ctx.getRoutes().get());

    if (routesOptional.isPresent()) {

      if (routesOptional.get().containsKey(route)) {
        ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>> routes =
            routesOptional.get();
        routes.get(route).add(handlerMap);
        ctx.getRoutes().set(routes);

      } else {
        ImmutableSortedMap.Builder<Route, List<ImmutableMap<XHttpMethod, Handler>>> routeMap =
            new ImmutableSortedMap.Builder<Route, List<ImmutableMap<XHttpMethod, Handler>>>(
                    Ordering.usingToString())
                .put(Route.build(routePattern), Lists.newArrayList(handlerMap));

        routesOptional.map(value -> routeMap.putAll(value.descendingMap()));

        ctx.getRoutes().set(routeMap.build());
      }
    } else {
      ImmutableSortedMap.Builder<Route, List<ImmutableMap<XHttpMethod, Handler>>> routeMap =
          new ImmutableSortedMap.Builder<Route, List<ImmutableMap<XHttpMethod, Handler>>>(
                  Ordering.usingToString())
              .put(Route.build(routePattern), Lists.newArrayList(handlerMap));

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

    /*
    '/info' -> should expose version number, git commit number, etc
    '/metrics' -> should return the metrics reporters in JSON format
    '/health' -> should expose a summary of downstream health checks
    '/ping' -> should respond with a 200-OK status code and the text 'PONG'
    '/ready' -> should expose a Kubernetes or ELB specific healthcheck for liveliness
    '/restart' -> restart service (should be restricted to approved devs / tooling)
     '/killkillkill' -> shutdown service (should be restricted to approved devs / tooling)```
     */

    get("/info", AdminHandlers.infoHandler());
    get("/metrics", AdminHandlers.metricsHandler(metricRegistry, metricsMapper));
    get("/health", AdminHandlers.healthCheckHandler(healthCheckRegistry, healthMapper));
    get("/ping", AdminHandlers.pingHandler());
    get("/ready", AdminHandlers.readyHandler());
    get("/restart", AdminHandlers.restartHandler(this));
    get("/killkillkill", AdminHandlers.killHandler(this));

    get("/gc", AdminHandlers.pingHandler());
  }

  /**
   * Builds an initializer that sets up the server pipeline, override this method to customize your
   * pipeline.
   *
   * @param state a value object with server wide state
   * @return a ChannelInitializer that builds this servers ChannelPipeline
   */
  public ChannelInitializer<Channel> initializer(State state) {
    return new ServerChannelInitializer(state);
  }

  /**
   * The listenAndServe method is the primary entry point for the server and should only be called
   * once and only from the main thread.
   *
   * @throws IOException throws in the event the network services, as specified, cannot be accessed
   */
  public void listenAndServe() throws IOException {
    State state =
        State.builder()
            .config(config)
            .globalConnectionLimiter(
                new ConnectionLimiter(
                    metricRegistry, config.maxConnections())) // All endpoints for a given service
            .rateLimiter(new ServiceRateLimiter(metricRegistry, config, ctx))
            .whiteListFilter(new WhiteListFilter(metricRegistry, config.ipWhiteList()))
            .blackListFilter(new BlackListFilter(metricRegistry, config.ipBlackList()))
            .firewall(new Firewall(metricRegistry))
            .tls(tls)
            .h1h2(new Http2OrHttpHandler(new UrlRouter(), ctx, config.corsConfig()))
            .build();

    configEndpointRequestCountMeters(metricRegistry, ctx);

    ServerBootstrap b =
        XrpcBootstrapFactory.buildBootstrap(
            config.bossThreadCount(), config.workerThreadCount(), config.workerNameFormat());

    b.childHandler(initializer(state));

    if (config.runBackgroundHealthChecks()) {
      scheduleHealthChecks(b.config().childGroup());
    }

    if (config.serveAdminRoutes()) {
      serveAdmin();
    }

    InetSocketAddress address = new InetSocketAddress(config.port());
    log.info("Listening at {}", address);
    ChannelFuture future = b.bind(address);

    try {
      // Build out the loggers that are specified in the config

      if (config.slf4jReporter()) {
        final Slf4jReporter slf4jReporter =
            Slf4jReporter.forRegistry(metricRegistry)
                .outputTo(LoggerFactory.getLogger(Router.class))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        slf4jReporter.start(config.slf4jReporterPollingRate(), TimeUnit.SECONDS);
      }

      if (config.jmxReporter()) {
        final JmxReporter jmxReporter = JmxReporter.forRegistry(metricRegistry).build();
        jmxReporter.start();
      }

      if (config.consoleReporter()) {
        final ConsoleReporter consoleReporter =
            ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        consoleReporter.start(config.consoleReporterPollingRate(), TimeUnit.SECONDS);
      }

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

  private void configEndpointRequestCountMeters(
      MetricRegistry metricRegistry, XrpcConnectionContext ctx) {
    ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>> routes =
        ctx.getRoutes().get();

    if (routes != null) {
      for (Map.Entry<Route, List<ImmutableMap<XHttpMethod, Handler>>> entry : routes.entrySet()) {
        Route route = entry.getKey();

        for (ImmutableMap<XHttpMethod, Handler> map : entry.getValue()) {
          for (XHttpMethod httpMethod : map.keySet()) {
            String routeName = MetricsUtil.getMeterNameForRoute(route, httpMethod);
            ctx.getMetersByRoute().put(routeName, metricRegistry.meter(name("routes", routeName)));
          }
        }
      }
    }
  }

  public void shutdown() {
    if (channel == null || !channel.isOpen()) {
      return;
    }

    channel
        .close()
        .addListener(
            future -> {
              if (!future.isSuccess()) {
                log.warn("Error shutting down server", future.cause());
              }
              synchronized (Router.this) {
                // TODO(JR): We should probably be more thoughtful here.
                shutdown();
              }
            });
  }
}
