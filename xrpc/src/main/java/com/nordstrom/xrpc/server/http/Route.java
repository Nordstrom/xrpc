package com.nordstrom.xrpc.server.http;

import com.nordstrom.xrpc.server.Handler;
import io.netty.handler.codec.http.HttpMethod;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class Route {
  HttpMethod method;
  RoutePath path;
  Handler handler;
}
