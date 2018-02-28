package com.nordstrom.xrpc.encoding;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import lombok.Value;
import lombok.experimental.Accessors;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * A Decoder that decodes a request body in Text format to a designated Object. If the target Object
 * for decoding implements TextDecodable, it will be decoded using this interface; otherwise, it
 * will fail.
 */
@Value
@Accessors(fluent = true)
public class TextDecoder implements Decoder {
  String contentType;

  @Override
  public <T> T decode(ByteBuf body, CharSequence contentType, Class<T> clazz) throws IOException {
    throw new NotImplementedException();
  }
}
