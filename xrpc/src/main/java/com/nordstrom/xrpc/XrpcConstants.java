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
import io.netty.util.AttributeKey;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class XrpcConstants {
  public static final AttributeKey<Boolean> XRPC_SOFT_RATE_LIMITED =
      AttributeKey.valueOf("XrpcSoftRateLimited");
  public static final AttributeKey<Boolean> XRPC_HARD_RATE_LIMITED =
      AttributeKey.valueOf("XrpcHardRateLimited");
  public static final AttributeKey<XrpcConnectionContext> CONNECTION_CONTEXT =
      AttributeKey.valueOf("XrpcConnectionContext");
  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
  public static final byte[] PAYLOAD_EXCEEDED_RESPONSE =
      "Request payload too large".getBytes(DEFAULT_CHARSET);
  public static final byte[] RATE_LIMIT_RESPONSE =
      "Too many requests being sent to the server".getBytes(DEFAULT_CHARSET);

  public static final byte[] INTERNAL_SERVER_ERROR_RESPONSE =
      "Internal Server Error".getBytes(DEFAULT_CHARSET);
  public static final AttributeKey<Boolean> IP_WHITE_LIST = AttributeKey.valueOf("IpWhiteList");
  public static final AttributeKey<Boolean> IP_BLACK_LIST = AttributeKey.valueOf("IpBlackList");
}
