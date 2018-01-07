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
