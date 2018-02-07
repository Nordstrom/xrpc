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

package com.nordstrom.xrpc;

import com.nordstrom.xrpc.server.XrpcConnectionContext;
import com.nordstrom.xrpc.server.XrpcRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AttributeKey;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class XrpcConstants {
  public static final AttributeKey<XrpcRequest> XRPC_REQUEST = AttributeKey.valueOf("XrpcRequest");
  public static final AttributeKey<Boolean> XRPC_SOFT_RATE_LIMITED =
      AttributeKey.valueOf("XrpcSoftRateLimited");
  public static final AttributeKey<Boolean> XRPC_HARD_RATE_LIMITED =
      AttributeKey.valueOf("XrpcHardRateLimited");
  public static final AttributeKey<XrpcConnectionContext> CONNECTION_CONTEXT =
      AttributeKey.valueOf("XrpcConnectionContext");
  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
  public static final ByteBuf RATE_LIMIT_RESPONSE =
      Unpooled.directBuffer()
          .writeBytes(
              "This response is being send due to too many requests being sent to the server"
                  .getBytes(DEFAULT_CHARSET));
  public static final AttributeKey<Boolean> IP_WHITE_LIST = AttributeKey.valueOf("IpWhiteList");
  public static final AttributeKey<Boolean> IP_BLACK_LIST = AttributeKey.valueOf("IpBlackList");
}
