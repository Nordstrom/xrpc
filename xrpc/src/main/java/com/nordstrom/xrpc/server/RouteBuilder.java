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

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.RoutePath;
import io.netty.handler.codec.http.HttpMethod;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/** Class to build routes for a server to handle. */
@Slf4j
public class RouteBuilder implements Routes {
  private final Map<RoutePath, Map<HttpMethod, Handler>> routes = new HashMap<>();

  @Override
  public Routes addRoute(Route route) {
    Map<HttpMethod, Handler> methods = routes.computeIfAbsent(route.path(), k -> new HashMap<>());

    // Verify that this method doesn't already exist.
    Preconditions.checkArgument(
        !methods.containsKey(route.method()),
        String.format(
            "route %s already has a handler defined for method %s",
            route, route.method().toString()));

    methods.put(route.method(), route.handler());

    return this;
  }

  /**
   * Returns the routes compiled from this builder, using the given MetricRegistry to track access
   * statistics.
   */
  public CompiledRoutes compile(MetricRegistry metricRegistry) {
    return new CompiledRoutes(this.routes, metricRegistry);
  }

  /** Returns iterator or routes. */
  @Override
  public Iterator<Route> iterator() {
    return routes
        .entrySet()
        .stream()
        .flatMap(
            r ->
                r.getValue()
                    .entrySet()
                    .stream()
                    .map(m -> new Route(m.getKey(), r.getKey(), m.getValue())))
        .collect(Collectors.toList())
        .iterator();
  }
}
