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
import com.nordstrom.xrpc.server.XrpcRequest;
import io.netty.buffer.ByteBuf;
import java.io.IOException;

/** An Encoder that encodes a response object to Text format. */
public class TextEncoder implements Encoder {
  /**
   * Encode a response object to JSON format for the HttpResponse.
   *
   * @param request current http request
   * @param object response object
   * @return ByteBuf representing JSON formatted String
   */
  @Override
  public ByteBuf encode(XrpcRequest request, Object object) throws IOException {
    // TODO (AD): Add Content-Type charset recognition
    String text =
        (object instanceof TextEncodable) ? ((TextEncodable) object).encode() : object.toString();
    byte[] bytes = text.getBytes(XrpcConstants.DEFAULT_CHARSET);
    ByteBuf buf = request.getAlloc().directBuffer(bytes.length);
    buf.writeBytes(bytes);
    return buf;
  }
}
