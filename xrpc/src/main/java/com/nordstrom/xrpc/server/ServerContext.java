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
import io.netty.util.AttributeKey;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/** Contextual data for the server. The same instance of this is used for all channel handlers. */
@Accessors(fluent = true)
@Builder(builderClassName = "Builder")
@Getter
@Slf4j
// TODO: (AD) Merge with State
public class ServerContext {
  public static final AttributeKey<ServerContext> ATTRIBUTE_KEY =
      AttributeKey.valueOf("ServerContext");
  private final Meter requestMeter;

  @Singular("meterByStatusCode")
  private final ImmutableMap<HttpResponseStatus, Meter> metersByStatusCode;

  private final CompiledRoutes routes;

  private final ExceptionHandler exceptionHandler;

  private final Encoders encoders;

  private final Decoders decoders;

  // This can be generated automatically by lombok, but we declare it here to fix a javadoc warning.
  // TODO(jkinkead): Remove once we have delombok integrated (issue #160).
  public static class Builder {}
}
