package com.nordstrom.xrpc.codec;

import com.nordstrom.xrpc.server.XrpcConnectionContext;
import com.nordstrom.xrpc.server.XrpcRequest;
import io.netty.buffer.ByteBuf;
import java.io.IOException;

public interface Encoder {
  ByteBuf encode(XrpcConnectionContext ctx, XrpcRequest request, Object object) throws IOException;
}
