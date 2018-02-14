package com.nordstrom.xrpc.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nordstrom.xrpc.server.XrpcConnectionContext;
import com.nordstrom.xrpc.server.XrpcRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class JsonEncoder implements Encoder {
  private final ObjectMapper mapper;

  public JsonEncoder(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public ByteBuf encode(XrpcConnectionContext ctx, XrpcRequest request, Object object)
      throws IOException {
    ByteBuf buf = request.getAlloc().directBuffer();
    OutputStream stream = new ByteBufOutputStream(buf);
    mapper.writeValue(stream, object);
    return buf;
  }
}
