package com.nordstrom.xrpc.server.Firewall;

import io.netty.channel.ChannelHandlerContext;

interface XrpcAppFirewall {

  boolean block(ChannelHandlerContext ctx, Object msg);
  
}
