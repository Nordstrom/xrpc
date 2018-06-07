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

import com.google.protobuf.MessageLite;
import com.nordstrom.xrpc.XrpcConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import java.io.IOException;

/**
 * Encoder that encodes an object to ByteBuf in protobuf format.
 *
 * <p>Currently this encoder uses Proto 3 generated classes to encode
 */
public class ProtoEncoder implements Encoder {
  /** Media type this decoder supports. */
  @Override
  public CharSequence mediaType() {
    // TODO (AD): since this is not the official content type of protobuf,
    // consider supporting other types used in the wild.
    return XrpcConstants.PROTO_CONTENT_TYPE;
  }

  /**
   * Encode a response object to protobuf format for the HttpResponse.
   *
   * @param buf target byte buffer for encoding
   * @param acceptCharset Accept-Charset header
   * @param object object to encode
   * @return ByteBuf representing protobuf formatted bytes
   */
  @Override
  public ByteBuf encode(ByteBuf buf, CharSequence acceptCharset, Object object) throws IOException {
    if (object == null) {
      return buf;
    }
    if (!(object instanceof MessageLite)) {
      throw new IllegalArgumentException(
          String.format("%s does not extend from MessageLite", object.getClass().getName()));
    }
    MessageLite msg = (MessageLite) object;
    try (ByteBufOutputStream stream = new ByteBufOutputStream(buf)) {
      msg.writeTo(stream);
      return buf;
    }
  }
}
