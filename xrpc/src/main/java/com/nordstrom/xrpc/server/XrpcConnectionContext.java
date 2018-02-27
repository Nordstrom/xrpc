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
import com.google.common.collect.ImmutableMap;
import com.nordstrom.xrpc.encoding.Decoders;
import com.nordstrom.xrpc.encoding.Encoders;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder(builderClassName = "Builder")
@Accessors(fluent = true)
// TODO: (AD) Merge with State
public class XrpcConnectionContext {
  @Getter private final Meter requestMeter;

  @Singular("meterByStatusCode")
  @Getter
  private final ImmutableMap<HttpResponseStatus, Meter> metersByStatusCode;

  @Getter private final CompiledRoutes routes;

  @Getter private final ExceptionHandler exceptionHandler;

  @Getter private final Encoders encoders;

  @Getter private final Decoders decoders;
}
