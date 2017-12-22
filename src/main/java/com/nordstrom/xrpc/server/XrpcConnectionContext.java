package com.nordstrom.xrpc.server;

import com.codahale.metrics.Meter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class XrpcConnectionContext {
  @Getter private Meter requestMeter;
  @Getter private int maxPayloadSize;

  @Getter
  private final ConcurrentHashMap<HttpResponseStatus, Meter> metersByStatusCode =
      new ConcurrentHashMap<>(6);

  @Getter
  private final AtomicReference<ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>>>
      routes = new AtomicReference<>();
}
