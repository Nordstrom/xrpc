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
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.http.HttpHeaderValues;
import java.io.IOException;
import java.io.OutputStreamWriter;
import lombok.AllArgsConstructor;

/** An Encoder that encodes an object to ByteBuf in JSON format. */
@AllArgsConstructor
public class JsonEncoder implements Encoder {
  private final ObjectMapper mapper;

  public CharSequence mediaType() {
    return HttpHeaderValues.APPLICATION_JSON;
  }

  /**
   * Encode a response object to JSON format for the HttpResponse.
   *
   * @param buf target byte buffer for encoding
   * @param object object to encode
   * @return ByteBuf representing JSON formatted String
   */
  @Override
  public ByteBuf encode(ByteBuf buf, CharSequence charset, Object object) throws IOException {
    try (OutputStreamWriter writer =
        new OutputStreamWriter(new ByteBufOutputStream(buf), charset.toString())) {
      mapper.writeValue(writer, object);
      return buf;
    }
  }
}
