package com.nordstrom.xrpc.server;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

@ChannelHandler.Sharable
class NoOpHandler extends ChannelDuplexHandler {

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ctx.pipeline().remove(this);
    ctx.fireChannelActive();
  }
}
