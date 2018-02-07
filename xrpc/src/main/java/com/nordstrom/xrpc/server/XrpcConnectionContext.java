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

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder(builderClassName = "Builder")
// TODO: (AD) Merge with State
// TODO: (AD) Make Immutable.
public class XrpcConnectionContext {
  @Getter private final Meter requestMeter;
  @Getter private final int maxPayloadSize;

  @Singular("meterByStatusCode")
  @Getter
  private final ImmutableMap<HttpResponseStatus, Meter> metersByStatusCode;

  @Getter
  private final AtomicReference<ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>>>
      routes = new AtomicReference<>();

  @Getter private final ObjectMapper mapper;

  @Getter private final ConcurrentHashMap<String, Meter> metersByRoute = new ConcurrentHashMap<>();
}
