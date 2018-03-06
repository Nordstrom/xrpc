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
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpUtil;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import lombok.AllArgsConstructor;

/**
 * Decoder that decodes a JSON ByteBuf to an object.
 *
 * <p>Currently this decoder uses Jackson ObjectMapper to decode, but eventually will use
 * configurable JSON decode provider.
 */
@AllArgsConstructor
public class JsonDecoder implements Decoder {
  /** Media type this decoder supports. */
  public CharSequence mediaType() {
    return HttpHeaderValues.APPLICATION_JSON;
  }

  private final ObjectMapper mapper;

  /**
   * Decode a ByteBuf body from JSON format to an object of designated Class type.
   *
   * @param body current http request
   * @param clazz target class for decoding
   * @return object of type clazz
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T decode(ByteBuf body, CharSequence contentType, Class<T> clazz) throws IOException {
    try (InputStreamReader reader =
        new InputStreamReader(
            new ByteBufInputStream(body),
            HttpUtil.getCharset(contentType, Charset.forName("UTF-8")))) {
      if (MessageOrBuilder.class.isAssignableFrom(clazz)) {
        // Use proto classes to decode
        Message.Builder builder = invoke(clazz, "newBuilder", new Class<?>[] {}, new Object[] {});
        JsonFormat.parser().merge(reader, builder);
        return (T) builder.build();
      }
      return mapper.readValue(reader, clazz);
    }
  }
}
