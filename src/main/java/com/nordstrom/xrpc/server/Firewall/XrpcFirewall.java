package com.nordstrom.xrpc.server.Firewall;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetSocketAddress;
import java.util.HashSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class XrpcFirewall extends ChannelDuplexHandler {
  private final HashSet<String> blacklist;
  private final HashSet<String> whitelist;
  //private final ZkClient zkClient;
  private final boolean noOp;
  private int packetSize;
  private int destinationPort;
  private String destinationAddress;
  private int sourcePort;
  private String sourceAddress;

  public XrpcFirewall(boolean noOp) {
    //this.zkClient = null;
    this.blacklist = null;
    this.whitelist = null;
    this.noOp = noOp;
  }

  public XrpcFirewall(HashSet blacklist, HashSet whitelist) {
    //this.zkClient = null;
    this.blacklist = blacklist;
    this.whitelist = whitelist;
    this.noOp = false;
  }

  //  public XrpcFirewall(ZkClient zkClient, boolean b) {
  //    this.zkClient = zkClient;
  //    // TODO(JR): ZK should populate this in the constructor?
  //    this.blacklist = null;
  //    this.whitelist = null;
  //    this.noOp = b;
  //  }

  private void buildReqCtx(ChannelHandlerContext ctx) {
    destinationPort = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
    destinationAddress = ((InetSocketAddress) ctx.channel().localAddress()).getHostString();
    sourcePort = ((InetSocketAddress) ctx.channel().remoteAddress()).getPort();
    sourceAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
  }

  abstract void runRuleSet(ChannelHandlerContext ctx, Object msg);

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object _evt) {
    XrpcEvent evt;

    if (_evt instanceof XrpcEvent) {
      evt = (XrpcEvent) _evt;
    } else {
      evt = null;
      //TODO(JR): Throw probably?
    }

    switch (evt) {
      case RATE_LIMIT:
        log.info("Xio Firewall blocked based on rate limit req:" + ctx.channel());
        ctx.channel().deregister();
        break;
      case BLOCK_REQ_POLICY_BASED:
        log.info("Xio Firewall blocked based on policy:" + ctx.channel());
        ctx.channel().deregister();
        break;
      case BLOCK_REQ_BEHAVIORAL_BASED:
        log.info("Xio Firewall blocked based on behavior:" + ctx.channel());
        ctx.channel().deregister();
        break;
      default:
        break;
    }
  }

  @Override
  @SuppressWarnings("deprecated")
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("Exception Caught in Xio Firewall: ", cause);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    if (noOp) {
      ctx.pipeline().remove(this);
      ctx.fireChannelActive();
      return;
    }

    buildReqCtx(ctx);

    if (!whitelist.contains(sourceAddress)) {
      if (blacklist.contains(sourceAddress)) {
        log.info("Xio Firewall blocked blacklisted channel:" + ctx.channel());
        ctx.channel().deregister();
      } else {
        ctx.fireChannelActive();
      }
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelInactive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      packetSize = ((ByteBuf) msg).readableBytes();
    } else {
      log.info("Xio Firewall blocked unreadable message :" + ctx.channel());
      ctx.channel().deregister();
    }

    runRuleSet(ctx, msg);

    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelReadComplete();
  }
}
