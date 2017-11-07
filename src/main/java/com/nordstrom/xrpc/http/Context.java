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

import java.util.Map;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.Getter;
import lombok.Setter;

/** Request context. */
public class Context {
  /** The request to handle. */
  @Getter private final FullHttpRequest h1Request;
  @Getter private final Http2Request<Http2Headers> h2Request;
  @Getter private final ByteBufAllocator alloc;
  @Setter private Http2Request<Http2DataFrame> data;
  /** The variables captured from the route path. */
  private final Map<String, String> groups;

  public Context(FullHttpRequest request, Map<String, String> groups, ByteBufAllocator alloc) {
    this.h1Request = request;
    this.h2Request = null;
    this.groups = groups;
    this.alloc = alloc;
  }

  public Context(Http2Request<Http2Headers> request, Map<String, String> groups, ByteBufAllocator alloc) {
    this.h1Request = null;
    this.h2Request = request;
    this.groups = groups;
    this.alloc = alloc;
  }

  /** Returns the variable with the given name, or null if that variable doesn't exist. */
  public String variable(String name) {
    return groups.get(name);
  }
}
