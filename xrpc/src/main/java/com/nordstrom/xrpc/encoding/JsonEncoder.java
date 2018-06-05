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
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.http.HttpHeaderValues;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import lombok.AllArgsConstructor;

/**
 * Encoder that encodes an object to ByteBuf in JSON format.
 *
 * <p>Currently this encoder uses Jackson ObjectMapper to encode, but eventually will use
 * configurable JSON encode provider.
 */
@AllArgsConstructor
public class JsonEncoder implements Encoder {
  public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
  private static final ImmutableSet<String> ALLOWED_CHARSETS =
      ImmutableSet.copyOf(new String[] {"UTF-8", "UTF-16", "UTF-32"});

  private final ObjectMapper mapper;
  private final JsonFormat.Printer printer;

  /** Media type this encoder supports. */
  public CharSequence mediaType() {
    return HttpHeaderValues.APPLICATION_JSON;
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
    try (OutputStreamWriter writer =
        new OutputStreamWriter(new ByteBufOutputStream(buf), charset(acceptCharset))) {
      if (object instanceof MessageOrBuilder) {
        // Encode object of proto generated Class
        String json = printer.print((MessageOrBuilder) object);
        writer.write(json);
      } else {
        // Encode POJO
        if (object != null) {
          mapper.writeValue(writer, object);
        }
      }
      return buf;
    }
  }

  @Override
  public Charset charset(CharSequence acceptCharset) {
    if (acceptCharset == null) {
      return DEFAULT_CHARSET;
    }
    String[] charsets = CHARSET_DELIMITER.split(acceptCharset);
    for (String charset : charsets) {
      String charsetUpper = charset.toUpperCase();
      if (ALLOWED_CHARSETS.contains(charsetUpper)) {
        return Charset.forName(charsetUpper);
      }
    }
    return DEFAULT_CHARSET;
  }
}
