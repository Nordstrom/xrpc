/*
 * Copyright 2017 Nordstrom, Inc.
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

package com.nordstrom.xrpc.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Xprc specific Request object.
 */
public class XrpcRequest {
  /**
   * The request to handle.
   */
  @Getter
  private final FullHttpRequest h1Request;

  @Getter
  private final Http2Headers h2Headers;
  @Getter
  private final ByteBufAllocator alloc;
  /**
   * The variables captured from the route path.
   */
  private final Map<String, String> groups;
  private final int streamId;
  @Setter
  private ByteBuf data;

  public XrpcRequest(FullHttpRequest request, Map<String, String> groups, ByteBufAllocator alloc) {
    this.h1Request = request;
    this.h2Headers = null;
    this.groups = groups;
    this.alloc = alloc;
    this.streamId = -1;
  }

  public XrpcRequest(Http2Headers headers, Map<String, String> groups, ByteBufAllocator alloc, int streamId) {
    this.h1Request = null;
    this.h2Headers = headers;
    this.groups = groups;
    this.alloc = alloc;
    this.streamId = streamId;
  }

  /**
   * Returns the variable with the given name, or null if that variable doesn't exist.
   */
  public String variable(String name) {
    return groups.get(name);
  }

  /**
   * Create a convenience function to prevent direct access too the Allocator
   */
  public ByteBuf getByteBuf() {
    return alloc.compositeDirectBuffer();
  }

  public FullHttpRequest getRequest() {
    if (h1Request != null) {
      return h1Request;
    }

    if (h2Headers != null) {
      try {
        FullHttpRequest h1req = HttpConversionUtil.toFullHttpRequest(0, h2Headers, alloc, true);
        if (data != null) {
          h1req.replace(data);
        }

        return h1Request;
      } catch (Http2Exception e) {
        //TODO(JR): Do something more meaningful with this exception
        e.printStackTrace();
      }
    }

    return null;
  }
}
