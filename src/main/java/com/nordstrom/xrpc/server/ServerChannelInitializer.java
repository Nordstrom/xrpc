/*
 * Copyright 2017 Nordstrom, Inc.
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
import com.nordstrom.xrpc.logging.ExceptionLogger;
import com.nordstrom.xrpc.server.tls.Tls;
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
  private final XConfig config;
  private final ConnectionLimiter globalConnectionLimiter;
  private final ServiceRateLimiter rateLimiter;
  private final WhiteListFilter whiteListFilter;
  private final BlackListFilter blackListFilter;
  private final Firewall firewall;
  private final Tls tls;
  private final Http2OrHttpHandler h1h2;

  public ServerChannelInitializer(
      XConfig config,
      ConnectionLimiter globalConnectionLimiter,
      ServiceRateLimiter rateLimiter,
      WhiteListFilter whiteListFilter,
      BlackListFilter blackListFilter,
      Firewall firewall,
      Tls tls,
      Http2OrHttpHandler h1h2) {
    this.config = config;
    this.globalConnectionLimiter = globalConnectionLimiter;
    this.rateLimiter = rateLimiter;
    this.whiteListFilter = whiteListFilter;
    this.blackListFilter = blackListFilter;
    this.firewall = firewall;
    this.tls = tls;
    this.h1h2 = h1h2;
  }

  @Override
  public void initChannel(Channel ch) throws Exception {
    ChannelPipeline cp = ch.pipeline();
    cp.addLast(
        "idleDisconnectHandler",
        new IdleDisconnectHandler(
            config.readerIdleTimeout(), config.writerIdleTimeout(), config.allIdleTimeout()));
    cp.addLast("serverConnectionLimiter", globalConnectionLimiter);
    cp.addLast("serverRateLimiter", rateLimiter);

    if (config.enableWhiteList()) {
      cp.addLast("whiteList", whiteListFilter);
    } else if (config.enableBlackList()) {
      cp.addLast("blackList", blackListFilter);
    }

    cp.addLast("firewall", firewall);
    cp.addLast("encryptionHandler", tls.getEncryptionHandler(ch.alloc())); // Add Config for Certs
    cp.addLast("codec", h1h2);
    cp.addLast("exceptionLogger", new ExceptionLogger());
  }
}
