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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RouterTest {
  @Test
  void addRoute() {
    XConfig config = new XConfig();
    Router r = new Router(config, 1);

    Handler h1 = xrpcRequest -> null;
    Handler h2 = xrpcRequest -> null;
    Handler h3 = xrpcRequest -> null;
    Handler h4 = xrpcRequest -> null;
    Handler h5 = xrpcRequest -> null;
    Handler h6 = xrpcRequest -> null;

    // Test Basic operation
    r.any("/foo", h1);
    r.any("/foo/bar", h2);
    r.any("/baz", h3);
    r.any("/baz/.*", h4);
    r.get("/people/{person}", h5);
    r.post("/people/{person}", h6);

    AtomicReference<ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>>> routes =
        r.getRoutes();

    for (Route route : routes.get().descendingKeySet()) {
      Optional<Map<String, String>> groups = Optional.ofNullable(route.groups("/foo"));
      if (groups.isPresent()) {
        assertEquals(
            h1,
            routes
                .get()
                .descendingMap()
                .get(route)
                .stream()
                .filter(m -> m.containsKey(XHttpMethod.ANY))
                .findFirst()
                .get()
                .get(XHttpMethod.ANY));
      }
    }

    for (Route route : routes.get().descendingKeySet()) {
      Optional<Map<String, String>> groups = Optional.ofNullable(route.groups("/foo/bar"));
      if (groups.isPresent()) {
        assertEquals(
            h2,
            routes
                .get()
                .descendingMap()
                .get(route)
                .stream()
                .filter(m -> m.containsKey(XHttpMethod.ANY))
                .findFirst()
                .get()
                .get(XHttpMethod.ANY));
      }
    }

    for (Route route : routes.get().descendingKeySet()) {
      Optional<Map<String, String>> groups = Optional.ofNullable(route.groups("/baz"));
      if (groups.isPresent()) {
        assertEquals(
            h3,
            routes
                .get()
                .descendingMap()
                .get(route)
                .stream()
                .filter(m -> m.containsKey(XHttpMethod.ANY))
                .findFirst()
                .get()
                .get(XHttpMethod.ANY));
      }
    }

    for (Route route : routes.get().descendingKeySet()) {
      Optional<Map<String, String>> groups = Optional.ofNullable(route.groups("/baz/biz"));
      if (groups.isPresent()) {
        assertEquals(
            h4,
            routes
                .get()
                .descendingMap()
                .get(route)
                .stream()
                .filter(m -> m.containsKey(XHttpMethod.ANY))
                .findFirst()
                .get()
                .get(XHttpMethod.ANY));
      }
    }

    for (Route route : routes.get().descendingKeySet()) {
      Optional<Map<String, String>> groups = Optional.ofNullable(route.groups("/people/jeff"));
      if (groups.isPresent()) {
        assertEquals(
            h5,
            routes
                .get()
                .descendingMap()
                .get(route)
                .stream()
                .filter(m -> m.containsKey(XHttpMethod.GET))
                .findFirst()
                .get()
                .get(XHttpMethod.GET));
      }
    }

    for (Route route : routes.get().descendingKeySet()) {
      Optional<Map<String, String>> groups = Optional.ofNullable(route.groups("/people/jeff"));
      if (groups.isPresent()) {
        assertEquals(
            h6,
            routes
                .get()
                .descendingMap()
                .get(route)
                .stream()
                .filter(m -> m.containsKey(XHttpMethod.POST))
                .findFirst()
                .get()
                .get(XHttpMethod.POST));
      }
    }
  }
}
