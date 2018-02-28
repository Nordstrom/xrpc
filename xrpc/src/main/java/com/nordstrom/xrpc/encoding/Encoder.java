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

/** Interface for encoding a response Object into a ByteBuf. */
public interface Encoder {
  /** Content type this encoder supports. */
  String contentType();

  /**
   * Encode an object to ByteBuf.
   *
   * @param buf target byte buffer for encoding
   * @param object object to encode
   * @return ByteBuf representing encoded object
   */
  ByteBuf encode(ByteBuf buf, Object object) throws IOException;
}
