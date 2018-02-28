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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** An Decoder that decodes a JSON ByteBuf to an object. */
@AllArgsConstructor
@Accessors(fluent = true)
public class JsonDecoder implements Decoder {
  /** Content type this decoder supports. */
  @Getter private final String contentType;

  private final ObjectMapper mapper;

  /**
   * Decode a ByteBuf body from JSON format to an object of designated Class type.
   *
   * @param body current http request
   * @param clazz target class for decoding
   * @return object of type clazz
   */
  @Override
  public <T> T decode(ByteBuf body, CharSequence contentType, Class<T> clazz) throws IOException {
    // TODO (AD): Does this handle encoding properly?
    // TODO (AD): Close stream?  It does not release bytebuf so no?
    InputStream input = new ByteBufInputStream(body);
    return mapper.readValue(input, clazz);
  }
}
