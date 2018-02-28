package com.nordstrom.xrpc.encoding;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import lombok.Value;
import lombok.experimental.Accessors;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@Value
@Accessors(fluent = true)
public class TextDecoder implements Decoder {
  String contentType;

  @Override
  public <T> T decode(ByteBuf body, CharSequence contentType, Class<T> clazz) throws IOException {
    throw new NotImplementedException();
  }
}
