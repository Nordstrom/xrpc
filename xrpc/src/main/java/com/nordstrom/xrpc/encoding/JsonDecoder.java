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

import com.nordstrom.xrpc.server.XrpcRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpUtil;
import java.io.IOException;
import java.nio.charset.Charset;

/** An Encoder that encodes a response object to JSON format. */
public class JsonDecoder implements Decoder {
  /**
   * Encode a response object to JSON format for the HttpResponse.
   *
   * @param request current http request
   * @return ByteBuf representing JSON formatted String
   */
  @Override
  public <T> T decode(XrpcRequest request, Class<T> clazz) throws IOException {
    Charset charset = HttpUtil.getCharset(request.getHeader(HttpHeaderNames.CONTENT_TYPE));
    String json = request.getData().toString(charset);
    return request.getConnectionContext().getMapper().readValue(json, clazz);
  }
}
