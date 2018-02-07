package com.nordstrom.xrpc.server;

import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MetricsUtil {

  private static Map<Route, String> routeIdentifierMap = new HashMap<>();

  protected static String getMeterNameForRoute(Route route, XHttpMethod httpMethod) {
    return getMeterNameForRoute(route, httpMethod.name());
  }

  protected static String getMeterNameForRoute(Route route, String httpMethod) {
    String method = Optional.ofNullable(httpMethod).orElse("ANY");

    String routeIdentifier = routeIdentifierMap.get(route);
    if (routeIdentifier == null) {
      if (route == null || route.toString() == null) {
        throw new IllegalArgumentException("Route cannot be null.");
      }

      routeIdentifier = route.toString().replace('/', '.');
      routeIdentifierMap.put(route, routeIdentifier);
    }

    return String.format("%s%s", method, routeIdentifier);
  }
}
