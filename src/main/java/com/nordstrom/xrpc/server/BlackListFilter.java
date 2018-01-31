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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import com.nordstrom.xrpc.XrpcConstants;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetSocketAddress;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
class BlackListFilter extends ChannelDuplexHandler {

  private final MetricRegistry metrics;
  private final ImmutableSet<String> blackList;

  public BlackListFilter(MetricRegistry metrics, ImmutableSet<String> blackList) {

    this.metrics = metrics;
    this.blackList = blackList;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    String remoteAddress =
        ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

    if (blackList.contains(remoteAddress)) {
      ctx.channel().attr(XrpcConstants.IP_BLACK_LIST).set(Boolean.TRUE);
    }

    ctx.fireChannelActive();
  }
}
