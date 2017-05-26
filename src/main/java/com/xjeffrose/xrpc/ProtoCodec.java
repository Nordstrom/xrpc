package com.xjeffrose.xrpc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtoCodec extends
    CombinedChannelDuplexHandler<ProtoObjectDecoder, ProtoObjectEncoder> implements
    HttpServerUpgradeHandler.SourceCodec {

  public ProtoCodec() {
    super(new ProtoObjectDecoder(), new ProtoObjectEncoder());
  }

  @Override
  public void upgradeFrom(ChannelHandlerContext channelHandlerContext) {

  }
}