/*
 * Copyright 2018 Nordstrom, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nordstrom.xrpc.encoding;

import com.nordstrom.xrpc.XrpcConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * Decoder that decodes a protobuf ByteBuf to an object.
 *
 * <p>Currently this decoder uses Proto 3 generated classes to decode.
 */
@Slf4j
public class ProtoDecoder implements Decoder {
  /** Content type this decoder supports. */
  @Override
  public CharSequence mediaType() {
    return XrpcConstants.PROTO_CONTENT_TYPE;
  }

  /**
   * Decode a ByteBuf body from protobuf format to an object of designated Class type.
   *
   * @param body current http request
   * @param clazz target class for decoding
   * @return object of type clazz
   */
  @Override
  public <T> T decode(ByteBuf body, CharSequence contentType, Class<T> clazz) throws IOException {
    // TODO (AD): given a Content-Type of application/protobuf; proto=org.some.Message,
    // we currently ignore the 2nd part, but should at least validate it in the future.
    try (ByteBufInputStream stream = new ByteBufInputStream(body)) {
      return invoke(clazz, "parseFrom", new Class<?>[] {InputStream.class}, new Object[] {stream});
    }
  }
}
