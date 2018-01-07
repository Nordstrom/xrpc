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
class WhiteListFilter extends ChannelDuplexHandler {

  private final MetricRegistry metrics;
  private final ImmutableSet<String> whiteList;

  public WhiteListFilter(MetricRegistry metrics, ImmutableSet<String> whiteList) {

    this.metrics = metrics;
    this.whiteList = whiteList;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    String remoteAddress =
        ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

    if (!whiteList.contains(remoteAddress)) {
      ctx.channel().attr(XrpcConstants.IP_WHITE_LIST).set(Boolean.FALSE);
    }

    ctx.fireChannelActive();
  }
}
