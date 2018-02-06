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
import com.codahale.metrics.MetricRegistry;
import com.nordstrom.xrpc.XrpcConstants;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
class Firewall extends ChannelDuplexHandler {

  private final MetricRegistry metrics;
  private final Meter rateLimits;

  public Firewall(MetricRegistry metrics) {

    this.metrics = metrics;
    this.rateLimits = metrics.meter("Hard Rate Limits");
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    if (ctx.channel().hasAttr(XrpcConstants.XRPC_HARD_RATE_LIMITED)) {
      if (log.isErrorEnabled()) {
        log.error("Channel {} Closed due to Xrpc Hard Rate Limit being reached", ctx.channel());
      }

      rateLimits.mark();
      ctx.channel().closeFuture();
    }

    if ((ctx.channel().hasAttr(XrpcConstants.IP_BLACK_LIST))) {
      if (log.isErrorEnabled()) {
        log.error("Channel {} Closed due to Xrpc IP Black List Configuration", ctx.channel());
      }
      ctx.channel().closeFuture();
    }

    // This will always be set to False
    if ((ctx.channel().hasAttr(XrpcConstants.IP_WHITE_LIST))) {
      if (log.isErrorEnabled()) {
        log.error("{} is not a white listed client. Dropping Connection", ctx.channel());
      }
      ctx.channel().closeFuture();
    }

    ctx.fireChannelActive();
  }
}
