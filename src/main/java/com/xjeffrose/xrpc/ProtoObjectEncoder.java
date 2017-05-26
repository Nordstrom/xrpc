package com.xjeffrose.xrpc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtoObjectEncoder extends MessageToMessageEncoder<Object> {

  @Override
  protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {

  }

}
