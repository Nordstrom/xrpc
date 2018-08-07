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

import com.nordstrom.xrpc.logging.ExceptionLogger;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import lombok.extern.slf4j.Slf4j;

/**
 * ServerChannelInitializer sets up the ChannelPipeline for an xrpc Server. This class should be
 * extended if you need to customize the pipeline for your Server.
 */
@Slf4j
public class ServerChannelInitializer extends ChannelInitializer<Channel> {
  private final State state;

  public ServerChannelInitializer(State state) {
    this.state = state;
  }

  @Override
  public void initChannel(Channel ch) {
    ChannelPipeline cp = ch.pipeline();
    cp.addLast(
        "idleDisconnectHandler",
        new IdleDisconnectHandler(
            state.config().readerIdleTimeout(),
            state.config().writerIdleTimeout(),
            state.config().allIdleTimeout()));
    cp.addLast("serverConnectionLimiter", state.globalConnectionLimiter());
    cp.addLast("serverRateLimiter", state.rateLimiter());

    if (state.config().enableWhiteList()) {
      cp.addLast("whiteList", state.whiteListFilter());
    } else if (state.config().enableBlackList()) {
      cp.addLast("blackList", state.blackListFilter());
    }

    cp.addLast("firewall", state.firewall());
    cp.addLast(
        "encryptionHandler", state.tls().encryptionHandler(ch.alloc())); // Add Config for Certs
    cp.addLast("codec", state.h1h2());
    cp.addLast("exceptionLogger", new ExceptionLogger());
  }
}
