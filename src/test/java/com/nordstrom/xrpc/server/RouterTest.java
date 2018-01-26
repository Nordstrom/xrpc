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
    r.addRoute("/foo", h1);
    r.addRoute("/foo/bar", h2);
    r.addRoute("/baz", h3);
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
