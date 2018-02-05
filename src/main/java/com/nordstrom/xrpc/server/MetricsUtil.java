package com.nordstrom.xrpc.server;

import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import java.util.Optional;

public class MetricsUtil {

  protected static String getMeterNameForRoute(Route route, XHttpMethod httpMethod) {
    return getMeterNameForRoute(route, httpMethod.name());
  }

  protected static String getMeterNameForRoute(Route route, String httpMethod) {
    String method = Optional.ofNullable(httpMethod).orElse("ANY");

    String routeIdentifier;
    if (route != null && route.toString() != null) {
      routeIdentifier = route.toString().replace('/', '.');
    } else {
      throw new IllegalArgumentException("Route cannot be null.");
    }

    return method + routeIdentifier;
  }
}
