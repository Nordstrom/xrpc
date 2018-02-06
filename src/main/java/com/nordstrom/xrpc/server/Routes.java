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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Routes {
  private final ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>> routeMap;

  private Routes(ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>> routeMap) {
    this.routeMap = routeMap;
  }

  public static Builder builder() {
    return new Builder();
  }

  public ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>> get() {
    return routeMap;
  }

  public static class Builder {
    private final Map<String, Map<XHttpMethod, Handler>> mutableMap;

    private Builder() {
      mutableMap = new LinkedHashMap<>();
    }

    /** Binds a handler for GET requests to the given route. */
    public Builder get(String route, Handler handler) {
      return addRoute(route, handler, XHttpMethod.GET);
    }

    /** Binds a handler for POST requests to the given route. */
    public Builder post(String route, Handler handler) {
      return addRoute(route, handler, XHttpMethod.POST);
    }

    /** Binds a handler for PUT requests to the given route. */
    public Builder put(String route, Handler handler) {
      return addRoute(route, handler, XHttpMethod.PUT);
    }

    /** Binds a handler for DELETE requests to the given route. */
    public Builder delete(String route, Handler handler) {
      return addRoute(route, handler, XHttpMethod.DELETE);
    }

    /** Binds a handler for HEAD requests to the given route. */
    public Builder head(String route, Handler handler) {
      return addRoute(route, handler, XHttpMethod.HEAD);
    }

    /** Binds a handler for OPTIONS requests to the given route. */
    public Builder options(String route, Handler handler) {
      return addRoute(route, handler, XHttpMethod.OPTIONS);
    }

    /** Binds a handler for PATCH requests to the given route. */
    public Builder patch(String route, Handler handler) {
      return addRoute(route, handler, XHttpMethod.PATCH);
    }

    /** Binds a handler for TRACE requests to the given route. */
    public Builder trace(String route, Handler handler) {
      return addRoute(route, handler, XHttpMethod.TRACE);
    }

    /** Binds a handler for CONNECT requests to the given route. */
    public Builder connect(String route, Handler handler) {
      return addRoute(route, handler, XHttpMethod.CONNECT);
    }

    /** Binds a handler for ANY requests to the given route. */
    public Builder any(String route, Handler handler) {
      return addRoute(route, handler, XHttpMethod.ANY);
    }

    private Builder addRoute(String routePattern, Handler handler, XHttpMethod method) {
      Preconditions.checkState(!routePattern.isEmpty());
      Preconditions.checkState(handler != null);
      Preconditions.checkState(method != null);

      Map<XHttpMethod, Handler> methods = mutableMap.get(routePattern);
      System.out.println("good " + routePattern + " method " + method);
      if (methods != null) {
        // error already defined
        Preconditions.checkState(
            methods.get(method) == null,
            "There is already a handler defined for pattern '%s' and method '%s'",
            routePattern,
            method);
      } else {
        // no map entry for this pattern, create an empty entry
        methods = new LinkedHashMap<XHttpMethod, Handler>();
        mutableMap.put(routePattern, methods);
      }
      // modify the methods map for this routePattern
      methods.put(method, handler);

      return this;
    }

    public Routes build() {
      ImmutableSortedMap.Builder<Route, List<ImmutableMap<XHttpMethod, Handler>>> mapBuilder =
          new ImmutableSortedMap.Builder<Route, List<ImmutableMap<XHttpMethod, Handler>>>(
              Ordering.usingToString());
      for (Map.Entry<String, Map<XHttpMethod, Handler>> entry : mutableMap.entrySet()) {
        List<ImmutableMap<XHttpMethod, Handler>> newEntries = new ArrayList<>();
        for (Map.Entry<XHttpMethod, Handler> subEntry : entry.getValue().entrySet()) {
          newEntries.add(
              new ImmutableMap.Builder<XHttpMethod, Handler>()
                  .put(subEntry.getKey(), subEntry.getValue())
                  .build());
        }
        mapBuilder.put(Route.build(entry.getKey()), newEntries);
      }
      return new Routes(mapBuilder.build());
    }
  }
}
