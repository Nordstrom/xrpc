package com.nordstrom.xrpc.server;

import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;

public class MetricsUtil {

  protected static String getMeterNameForRoute(Route route, XHttpMethod httpMethod) {
    return getMeterNameForRoute(route, httpMethod.name());
  }

  protected static String getMeterNameForRoute(Route route, String httpMethod) {
    String result = "";
    if (httpMethod != null) {
      result += httpMethod.toLowerCase();
    }

    if (route != null && route.toString() != null) {
      result += route.toString().replace('/', '.');
    }

    return result;
  }
}
