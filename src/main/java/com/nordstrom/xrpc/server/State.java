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

import com.google.auto.value.AutoValue;
import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.server.tls.Tls;

/** State is a value object for storing server state */
@AutoValue
public abstract class State {
  public abstract XConfig config();

  public abstract ConnectionLimiter globalConnectionLimiter();

  public abstract ServiceRateLimiter rateLimiter();

  public abstract WhiteListFilter whiteListFilter();

  public abstract BlackListFilter blackListFilter();

  public abstract Firewall firewall();

  public abstract Tls tls();

  public abstract Http2OrHttpHandler h1h2();

  static Builder builder() {
    return new AutoValue_State.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder config(XConfig config);

    public abstract Builder globalConnectionLimiter(ConnectionLimiter globalConnectionLimiter);

    public abstract Builder rateLimiter(ServiceRateLimiter rateLimiter);

    public abstract Builder whiteListFilter(WhiteListFilter whiteListFilter);

    public abstract Builder blackListFilter(BlackListFilter blackListFilter);

    public abstract Builder firewall(Firewall firewall);

    public abstract Builder tls(Tls tls);

    public abstract Builder h1h2(Http2OrHttpHandler h1h2);

    public abstract State build();
  }
}
