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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderValues;
import java.io.IOException;
import java.util.Formattable;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

/**
 * An Encoder that encodes a response object to Text format. If the Object being encoded implements
 * TextEncodable, it will be encoded using this interface; otherwise, it will use the Object's
 * toString method.
 */
@AllArgsConstructor
@Accessors(fluent = true)
public class TextEncoder implements Encoder {
  @Override
  public CharSequence mediaType() {
    return HttpHeaderValues.TEXT_PLAIN;
  }

  /**
   * Encode a response object to JSON format for the HttpResponse.
   *
   * @param buf target byte buffer for encoding
   * @param acceptCharset Accept-Charset header
   * @param object object to encode
   * @return ByteBuf representing JSON formatted String
   */
  @Override
  public ByteBuf encode(ByteBuf buf, CharSequence acceptCharset, Object object) throws IOException {
    String text =
        (object instanceof Formattable) ? ((TextEncodable) object).encode() : object.toString();
    byte[] bytes = text.getBytes(charset(acceptCharset));
    buf.writeBytes(bytes);
    return buf;
  }
}
