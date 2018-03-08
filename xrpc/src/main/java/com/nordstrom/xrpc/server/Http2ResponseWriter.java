package com.nordstrom.xrpc.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.Optional;

@FunctionalInterface
public interface Http2ResponseWriter {
  void write(
      final ChannelHandlerContext ctx,
      final int streamId,
      Http2Headers headers,
      Optional<ByteBuf> bodyOpt);
}
