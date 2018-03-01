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
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

/** Interface for encoding a response Object into a ByteBuf. */
public interface Encoder extends MediaTypeable {
  Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");
  Pattern CHARSET_DELIMITER = Pattern.compile(" *, *");

  /**
   * Encode an object to ByteBuf.
   *
   * @param buf target byte buffer for encoding
   * @param acceptCharset Accept-Charset header
   * @param object object to encode
   * @return ByteBuf representing encoded object
   */
  ByteBuf encode(ByteBuf buf, CharSequence acceptCharset, Object object) throws IOException;

  default Charset charset(CharSequence acceptCharset) {
    String[] charsets = CHARSET_DELIMITER.split(acceptCharset);
    for (String charset : charsets) {
      if (Charset.isSupported(charset)) {
        return Charset.forName(charset);
      }
    }
    return DEFAULT_CHARSET;
  }
}
