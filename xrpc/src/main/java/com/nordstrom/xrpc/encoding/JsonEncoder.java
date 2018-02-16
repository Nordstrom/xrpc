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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class JsonEncoder implements Encoder {
  @Override
  public ByteBuf encode(XrpcRequest request, Object object) throws IOException {
    ByteBuf buf = request.getAlloc().directBuffer();
    OutputStream stream = new ByteBufOutputStream(buf);
    request.getConnectionContext().getMapper().writeValue(stream, object);
    return buf;
  }
}
