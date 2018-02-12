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
import io.netty.handler.codec.http.HttpMethod;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** Class to build routes for a server to handle. */
@Slf4j
public class Routes {
  private final Map<Route, Map<HttpMethod, Handler>> routes = new HashMap<>();

  /**
   * Binds a handler for GET requests to the given route.
   *
   * @throws IllegalArgumentException if either the route or handler is null; if the route is empty;
   *     or if there is already a GET handler for the route.
   */
  public Routes get(String route, Handler handler) {
    return addRoute(route, handler, HttpMethod.GET);
  }

  /**
   * Binds a handler for POST requests to the given route.
   *
   * @throws IllegalArgumentException if either the route or handler is null; if the route is empty;
   *     or if there is already a POST handler for the route.
   */
  public Routes post(String route, Handler handler) {
    return addRoute(route, handler, HttpMethod.POST);
  }

  /**
   * Binds a handler for PUT requests to the given route.
   *
   * @throws IllegalArgumentException if either the route or handler is null; if the route is empty;
   *     or if there is already a PUT handler for the route.
   */
  public Routes put(String route, Handler handler) {
    return addRoute(route, handler, HttpMethod.PUT);
  }

  /**
   * Binds a handler for DELETE requests to the given route.
   *
   * @throws IllegalArgumentException if either the route or handler is null; if the route is empty;
   *     or if there is already a DELETE handler for the route.
   */
  public Routes delete(String route, Handler handler) {
    return addRoute(route, handler, HttpMethod.DELETE);
  }

  /**
   * Binds a handler for HEAD requests to the given route.
   *
   * @throws IllegalArgumentException if either the route or handler is null; if the route is empty;
   *     or if there is already a HEAD handler for the route.
   */
  public Routes head(String route, Handler handler) {
    return addRoute(route, handler, HttpMethod.HEAD);
  }

  /**
   * Binds a handler for OPTIONS requests to the given route.
   *
   * @throws IllegalArgumentException if either the route or handler is null; if the route is empty;
   *     or if there is already a OPTIONS handler for the route.
   */
  public Routes options(String route, Handler handler) {
    return addRoute(route, handler, HttpMethod.OPTIONS);
  }

  /**
   * Binds a handler for PATCH requests to the given route.
   *
   * @throws IllegalArgumentException if either the route or handler is null; if the route is empty;
   *     or if there is already a PATCH handler for the route.
   */
  public Routes patch(String route, Handler handler) {
    return addRoute(route, handler, HttpMethod.PATCH);
  }

  /**
   * Binds a handler for TRACE requests to the given route.
   *
   * @throws IllegalArgumentException if either the route or handler is null; if the route is empty;
   *     or if there is already a TRACE handler for the route.
   */
  public Routes trace(String route, Handler handler) {
    return addRoute(route, handler, HttpMethod.TRACE);
  }

  /**
   * Binds a handler for CONNECT requests to the given route.
   *
   * @throws IllegalArgumentException if either the route or handler is null; if the route is empty;
   *     or if there is already a CONNECT handler for the route.
   */
  public Routes connect(String route, Handler handler) {
    return addRoute(route, handler, HttpMethod.CONNECT);
  }

  private Routes addRoute(String routePattern, Handler handler, HttpMethod method) {
    Preconditions.checkArgument(routePattern != null, "routePattern must not be null");
    Preconditions.checkArgument(!routePattern.isEmpty(), "routePattern must not be empty");
    Preconditions.checkArgument(handler != null, "handler must not be null");
    Preconditions.checkArgument(method != null, "method must not be null");

    Route route = Route.build(routePattern);

    Map<HttpMethod, Handler> methods = routes.get(route);
    if (methods == null) {
      methods = new HashMap<>();
      routes.put(route, methods);
    }

    // Verify that this method doesn't already exist.
    Preconditions.checkArgument(
        !methods.containsKey(method),
        String.format(
            "route %s already has a handler defined for method %s", route, method.toString()));

    methods.put(method, handler);

    return this;
  }

  /**
   * Returns the routes compiled from this builder, using the given MetricRegistry to track access
   * statistics.
   */
  public CompiledRoutes compile(MetricRegistry metricRegistry) {
    return new CompiledRoutes(this.routes, metricRegistry);
  }
}
