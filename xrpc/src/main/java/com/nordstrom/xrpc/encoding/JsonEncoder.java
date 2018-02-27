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
import java.io.IOException;
import java.io.OutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

/** An Encoder that encodes an object to ByteBuf in JSON format. */
@AllArgsConstructor
@Accessors(fluent = true)
public class JsonEncoder implements Encoder {
  @Getter private final String contentType;
  private final ObjectMapper mapper;

  /**
   * Encode a response object to JSON format for the HttpResponse.
   *
   * @param buf target byte buffer for encoding
   * @param object object to encode
   * @return ByteBuf representing JSON formatted String
   */
  @Override
  public ByteBuf encode(ByteBuf buf, Object object) throws IOException {
    OutputStream stream = new ByteBufOutputStream(buf);
    mapper.writeValue(stream, object);
    return buf;
  }
}
