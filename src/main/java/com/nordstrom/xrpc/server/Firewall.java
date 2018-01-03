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
    if (ctx.channel().hasAttr(XrpcConstants.XRPC_HARD_RATE_LIMIT)) {
      log.debug("Channel " + ctx.channel() + " Closed due to Xrpc Hard Rate Limit being reached");
      rateLimits.mark();
      ctx.pipeline().channel().closeFuture();
    }

    if ((ctx.channel().hasAttr(XrpcConstants.IP_BLACK_LIST))) {
      log.debug("Channel " + ctx.channel() + " Closed due to Xrpc IP Black List Configuration");
      rateLimits.mark();
      ctx.pipeline().channel().closeFuture();
    }

    ctx.fireChannelActive();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelInactive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelReadComplete();
  }
}
