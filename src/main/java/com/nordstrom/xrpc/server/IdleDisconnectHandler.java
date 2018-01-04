package com.nordstrom.xrpc.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

@ChannelHandler.Sharable
class IdleDisconnectHandler extends IdleStateHandler {

  public IdleDisconnectHandler(
      int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
    super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
  }

  @Override
  protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
    ctx.close().addListener(ChannelFutureListener.CLOSE);
  }
}
