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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.nordstrom.xrpc.server.http.Route;
import io.netty.handler.codec.http.HttpMethod;
import java.util.Map;
import java.util.Optional;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Compiled routes instance for use at serving time. The term "compiled" is used very loosely. This
 * is responsible for routing requests to a given handler, and for tracking how often handlers are
 * invoked.
 */
@Slf4j
public class CompiledRoutes {
  /**
   * Map of routes to their handlers-by-method maps. Routes are sorted alphabetically by path for
   * consistent application.
   */
  private final ImmutableSortedMap<Route, ImmutableMap<HttpMethod, Handler>> routes;

  /**
   * Returns compiled routes built from the given route map.
   *
   * @param metricRegistry the registry to generate per-(route,method) rate statistics in
   */
  public CompiledRoutes(
      Map<Route, Map<HttpMethod, Handler>> rawRoutes, MetricRegistry metricRegistry) {
    // Build a sorted map of the routes.
    ImmutableSortedMap.Builder<Route, ImmutableMap<HttpMethod, Handler>> routesBuilder =
        ImmutableSortedMap.naturalOrder();
    for (Map.Entry<Route, Map<HttpMethod, Handler>> routeEntry : rawRoutes.entrySet()) {
      ImmutableMap.Builder<HttpMethod, Handler> handlers = new ImmutableMap.Builder<>();
      Route route = routeEntry.getKey();
      for (Map.Entry<HttpMethod, Handler> methodHandlerEntry : routeEntry.getValue().entrySet()) {
        HttpMethod method = methodHandlerEntry.getKey();

        // Wrap the user-provided handler in one that tracks request rates.
        String metricName = MetricRegistry.name("routes", method.name(), route.toString());
        final Handler userHandler = methodHandlerEntry.getValue();
        final Meter meter = metricRegistry.meter(metricName);
        Handler meteredHandler =
            request -> {
              meter.mark();
              // TODO(jkinkead): Add a timer here per https://github.com/Nordstrom/xrpc/issues/121
              return userHandler.handle(request);
            };
        handlers.put(method, meteredHandler);
      }

      routesBuilder.put(route, handlers.build());
    }

    this.routes = routesBuilder.build();
  }

  /**
   * Gets the handler and matched groups for the given path and method.
   *
   * @return the handler for the given path and method, if a handler matched. The handler returned
   *     will set the match groups on the request when passed in, then forward the request to the
   *     user-provided handler.
   */
  public Optional<Match> match(String path, HttpMethod method) {
    boolean pathMatched = false;
    for (Map.Entry<Route, ImmutableMap<HttpMethod, Handler>> routeToHandlers : routes.entrySet()) {
      Map<String, String> groups = routeToHandlers.getKey().groups(path);
      if (groups != null) {
        pathMatched = true;
        Handler handler = routeToHandlers.getValue().get(method);
        if (handler != null) {
          return Optional.of(new Match(handler, groups));
        }
      }
    }

    return Optional.empty();
  }

  /** Container class for storing a route coupled with the handler implementations for it. */
  @Value
  private static class RouteWithHandlers {
    /** The route that this matches. */
    Route route;

    /** A map of HttpMethod to the handler for that method. Will not be empty. */
    ImmutableMap<HttpMethod, Handler> handlers;
  }

  /** Container for a matched Route. */
  @Value
  static class Match {
    /** The handler that matched the request path. */
    Handler handler;

    /** Groups which were pulled out of the request path. */
    Map<String, String> groups;
  }
}
