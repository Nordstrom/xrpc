package com.nordstrom.xrpc.server;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableSortedMap;
import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RouterTest {
  @Test
  void addRoute() {
    XConfig config = new XConfig();
    Router r = new Router(config, 1);

    Handler h1 =
        new Handler() {
          @Override
          public HttpResponse handle(XrpcRequest xrpcRequest) {
            return null;
          }
        };

    Handler h2 =
        new Handler() {
          @Override
          public HttpResponse handle(XrpcRequest xrpcRequest) {
            return null;
          }
        };

    Handler h3 =
        new Handler() {
          @Override
          public HttpResponse handle(XrpcRequest xrpcRequest) {
            return null;
          }
        };

    Handler h4 =
        new Handler() {
          @Override
          public HttpResponse handle(XrpcRequest xrpcRequest) {
            return null;
          }
        };

    Handler h5 =
        new Handler() {
          @Override
          public HttpResponse handle(XrpcRequest xrpcRequest) {
            return null;
          }
        };

    // Test Basic operation
    r.addRoute("/foo", h1);
    r.addRoute("/foo/bar", h2);
    r.addRoute("/baz", h3);
    r.addRoute("/baz/.*", h4);
    r.addRoute("/people/{person}", h5);

    AtomicReference<ImmutableSortedMap<Route, Map<XHttpMethod, Handler>>> routes = r.getRoutes();

    for (Route route : routes.get().descendingKeySet()) {
      Optional<Map<String, String>> groups = Optional.ofNullable(route.groups("/foo"));
      if (groups.isPresent()) {
        assertEquals(h1, routes.get().descendingMap().get(route).get(XHttpMethod.ANY));
      }
    }

    for (Route route : routes.get().descendingKeySet()) {
      Optional<Map<String, String>> groups = Optional.ofNullable(route.groups("/foo/bar"));
      if (groups.isPresent()) {
        assertEquals(h2, routes.get().descendingMap().get(route).get(XHttpMethod.ANY));
      }
    }

    for (Route route : routes.get().descendingKeySet()) {
      Optional<Map<String, String>> groups = Optional.ofNullable(route.groups("/baz"));
      if (groups.isPresent()) {
        assertEquals(h3, routes.get().descendingMap().get(route).get(XHttpMethod.ANY));
      }
    }

    for (Route route : routes.get().descendingKeySet()) {
      Optional<Map<String, String>> groups = Optional.ofNullable(route.groups("/baz/biz"));
      if (groups.isPresent()) {
        assertEquals(h4, routes.get().descendingMap().get(route).get(XHttpMethod.ANY));
      }
    }

    for (Route route : routes.get().descendingKeySet()) {
      Optional<Map<String, String>> groups = Optional.ofNullable(route.groups("/people/admin"));
      if (groups.isPresent()) {
        assertEquals(h5, routes.get().descendingMap().get(route).get(XHttpMethod.ANY));
      }
    }

    for (Route route : routes.get().descendingKeySet()) {
      Optional<Map<String, String>> groups = Optional.ofNullable(route.groups("/people/jeff"));
      if (groups.isPresent()) {
        assertEquals(h5, routes.get().descendingMap().get(route).get(XHttpMethod.ANY));
      }
    }
  }
}
