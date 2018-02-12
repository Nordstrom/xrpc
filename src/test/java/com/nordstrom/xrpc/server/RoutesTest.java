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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RoutesTest {

  @Test
  public void addTwice() {
    assertThrows(
        IllegalStateException.class,
        () -> {
          Routes routes =
              Routes.builder()
                  .any("/twice", request -> null)
                  .any("/twice", request -> null)
                  .build();
        });
  }

  @Test
  void addRoute() {
    Handler h1 = request -> null;
    Handler h2 = request -> null;
    Handler h3 = request -> null;
    Handler h4 = request -> null;
    Handler h5 = request -> null;
    Handler h6 = request -> null;

    Routes routes =
        Routes.builder()
            .any("/foo", h1)
            .post("/foo/bar", h2)
            .put("/baz", h3)
            .delete("/baz/.*", h4)
            .get("/people/{person}", h5)
            .head("/people/{person}", h6)
            .build();

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
                .filter(m -> m.containsKey(XHttpMethod.POST))
                .findFirst()
                .get()
                .get(XHttpMethod.POST));
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
                .filter(m -> m.containsKey(XHttpMethod.PUT))
                .findFirst()
                .get()
                .get(XHttpMethod.PUT));
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
                .filter(m -> m.containsKey(XHttpMethod.DELETE))
                .findFirst()
                .get()
                .get(XHttpMethod.DELETE));
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
                .filter(m -> m.containsKey(XHttpMethod.HEAD))
                .findFirst()
                .get()
                .get(XHttpMethod.HEAD));
      }
    }
  }
}
