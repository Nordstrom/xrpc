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

import com.nordstrom.xrpc.XConfig;
import io.netty.handler.ssl.SslContext;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/** State is a value object for storing server state. */
@Accessors(fluent = true)
@Builder(builderClassName = "Builder")
@Getter
public class State {
  private final XConfig config;

  private final ConnectionLimiter globalConnectionLimiter;

  private final ServiceRateLimiter rateLimiter;

  private final WhiteListFilter whiteListFilter;

  private final BlackListFilter blackListFilter;

  private final Firewall firewall;

  private final SslContext sslContext;

  private final Http2OrHttpHandler h1h2;

  // This can be generated automatically by lombok, but we declare it here to fix a javadoc warning.
  // TODO(jkinkead): Remove once we have delombok integrated (issue #160).
  public static class Builder {}
}
