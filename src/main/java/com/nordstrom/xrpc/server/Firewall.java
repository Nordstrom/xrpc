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
        log.error("Channel " + ctx.channel() + " Closed due to Xrpc Hard Rate Limit being reached");
      }

      rateLimits.mark();
      ctx.channel().closeFuture();
    }

    if ((ctx.channel().hasAttr(XrpcConstants.IP_BLACK_LIST))) {
      if (log.isErrorEnabled()) {
        log.error("Channel " + ctx.channel() + " Closed due to Xrpc IP Black List Configuration");
      }
      ctx.channel().closeFuture();
    }

    // This will always be set to False
    if ((ctx.channel().hasAttr(XrpcConstants.IP_WHITE_LIST))) {
      if (log.isErrorEnabled()) {
        log.error(ctx.channel() + " is not a white listed client. Dropping Connection");
      }
      ctx.channel().closeFuture();
    }

    ctx.fireChannelActive();
  }
}
