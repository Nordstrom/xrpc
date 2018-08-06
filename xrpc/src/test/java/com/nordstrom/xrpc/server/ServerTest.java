package com.nordstrom.xrpc.server;

import static com.nordstrom.xrpc.server.http.RoutePath.build;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.RoutePath;
import io.netty.handler.codec.http.HttpMethod;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerTest {

  private static final List<RoutePath> EXPECTED_ADMIN_ROUTES =
      ImmutableList.of(
          build("/metrics"), build("/health"), build("/info"), build("/ping"), build("/ready"));
  private static final int NUMBER_OF_INFO_ADMIN_ROUTES = 5;
  private Server server;

  @BeforeEach
  void setup() {
    server = new Server();
  }

  @AfterEach
  void tearDown() {
    server.shutdown();
  }

  @Test
  void shouldRegisterInfoAdminRoutesByDefault() throws IOException {
    server.listenAndServe();
    List<Route> routes = new ArrayList<>();
    server.iterator().forEachRemaining(routes::add);
    assertEquals(NUMBER_OF_INFO_ADMIN_ROUTES, routes.size());
    for (Route route : routes) {
      assertTrue(EXPECTED_ADMIN_ROUTES.contains(route.path()));
    }
  }

  @Test
  void shouldRegisterCustomRoute() throws IOException {
    List<Route> routes = new ArrayList<>();
    Route testRoute = new Route(HttpMethod.GET, RoutePath.build("test-route"), null);
    server.addRoute(testRoute);
    server.listenAndServe();
    server.iterator().forEachRemaining(routes::add);
    assertEquals(NUMBER_OF_INFO_ADMIN_ROUTES + 1, routes.size());
    assertTrue(routes.contains(testRoute));
  }
}
