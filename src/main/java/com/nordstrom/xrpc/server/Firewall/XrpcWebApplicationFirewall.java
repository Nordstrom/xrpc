package com.nordstrom.xrpc.server.Firewall;

import io.netty.channel.ChannelHandlerContext;
import java.util.HashSet;

public class XrpcWebApplicationFirewall extends XrpcFirewall {

  public XrpcWebApplicationFirewall(boolean noOp) {
    super(noOp);
  }

  public XrpcWebApplicationFirewall(HashSet blacklist, HashSet whitelist) {
    super(blacklist, whitelist);
  }

  //  public XrpcWebApplicationFirewall(ZkClient zkClient, boolean b) {
  //    super(zkClient, b);
  //  }

  @Override
  void runRuleSet(ChannelHandlerContext ctx, Object msg) {}
}
