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

package com.nordstrom.xrpc.server;

import com.nordstrom.xrpc.encoding.Decoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.Http2Headers;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/** Xprc specific Request object. */
@Slf4j
@Accessors(fluent = true)
public class XrpcRequest implements ResponseFactory {
  /** The HTTP/1.x request to handle. null if this is an HTTP/2 request. */
  private final FullHttpRequest h1Request;

  /** The HTTP/2 request headers. null if this is an HTTP/1.x request. */
  @Getter(AccessLevel.PACKAGE)
  private final Http2Headers h2Headers;

  @Getter private final EventLoop eventLoop;
  @Getter private final Channel upstreamChannel;
  @Getter private final ByteBufAllocator alloc;

  @Getter private final ServerContext connectionContext;

  /** The variables captured from the route path. */
  private final Map<String, String> groups;

  /** The parsed query string. Lazily initialized in query(). */
  private HttpQuery query;

  private ResponseFactory responseFactory;

  /** HTTP/2 request data. Null for HTTP/1.x requests. */
  private final CompositeByteBuf h2Data;

  public XrpcRequest(
      FullHttpRequest request,
      ServerContext connectionContext,
      Map<String, String> groups,
      Channel channel) {
    this.h1Request = request;
    this.h2Headers = null;
    this.connectionContext = connectionContext;
    this.groups = groups;
    this.upstreamChannel = channel;
    this.alloc = channel.alloc();
    this.eventLoop = channel.eventLoop();
    this.h2Data = null;
  }

  public XrpcRequest(
      Http2Headers headers,
      ServerContext connectionContext,
      Map<String, String> groups,
      Channel channel) {
    this.h1Request = null;
    this.h2Headers = headers;
    this.connectionContext = connectionContext;
    this.groups = groups;
    this.upstreamChannel = channel;
    this.alloc = channel.alloc();
    this.eventLoop = channel.eventLoop();
    this.h2Data = alloc.compositeBuffer();
  }

  public HttpQuery query() {
    if (query == null) {
      if (h1Request != null) {
        query = new HttpQuery(h1Request.uri());
      } else if (h2Headers != null) {
        query = new HttpQuery(h2Headers.path().toString());
      } else {
        throw new IllegalStateException("Cannot get query.  http1.1 or http2 request needed.");
      }
    }
    return query;
  }

  @Override
  public XrpcRequest request() {
    return this;
  }

  public ResponseFactory response() {
    if (responseFactory == null) {
      responseFactory = () -> XrpcRequest.this;
    }
    return responseFactory;
  }

  /** Returns the variable with the given name, or null if that variable doesn't exist. */
  public String variable(String name) {
    return groups.get(name);
  }

  /** Create a convenience function to prevent direct access to the Allocator. */
  public ByteBuf byteBuf() {
    return alloc.compositeDirectBuffer();
  }

  /**
   * Adds data to an HTTP/2 request. Normally called with each data frame read by the HTTP/2 codec.
   *
   * @return the total data size available to read after adding the provided buffer. This is the
   *     total size of the data buffer, assuming no reads have been performed.
   */
  int addData(ByteBuf dataFrame) {
    // CompositeByteBuf will take ownership of releasing the byte buf, per docs.
    h2Data.addComponent(true, dataFrame.retain());
    return h2Data.readableBytes();
  }

  /**
   * Returns the request body decoding it into an Object of the designated Class. Note that any
   * reads will consume the buffer, so the caller is responsible for copying or resetting the buffer
   * as needed.
   */
  public <T> T body(Class<T> clazz) throws IOException {
    Decoder decoder = connectionContext.decoders().decoder(contentTypeHeader());
    return decoder.decode(body(), contentTypeHeader(), clazz);
  }

  /**
   * Returns the raw request body. Note that any reads will consume the buffer, so the caller is
   * responsible for copying or resetting the buffer as needed.
   */
  public ByteBuf body() {
    if (h1Request != null) {
      // HTTP/1.x request; return content directly.
      return h1Request.content();
    }
    // HTTP/2 request; return composite of data frames.
    return h2Data;
  }

  /**
   * Returns a new string representing the request body, decoded using the appropriate charset. This
   * does NOT consume or mutate the underlying data buffer.
   */
  public String bodyText() {
    // Note that this defaults to iso-8859-1 per
    // https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7.1.
    Charset charset = HttpUtil.getCharset(contentTypeHeader());
    return body().toString(charset);
  }

  /**
   * Returns the value of the given HTTP header, or null if it's missing.
   *
   * @param name the header name, lower-cased
   */
  public CharSequence header(CharSequence name) {
    if (h1Request != null) {
      return h1Request.headers().get(name);
    } else if (h2Headers != null) {
      return h2Headers.get(name);
    } else {
      throw new IllegalStateException("Neither HTTP/1 nor HTTP/2 headers set");
    }
  }

  public List<Map.Entry<CharSequence, CharSequence>> allHeaders() {
    if (h1Request != null) {
      return h1Request
        .headers()
        .entries()
        .stream()
        .map( entry -> new AbstractMap.SimpleEntry<CharSequence, CharSequence>(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
    } else if (h2Headers != null) {
      List<Map.Entry<CharSequence, CharSequence>> list = new ArrayList<>();
      h2Headers.iterator().forEachRemaining(list::add);
      return list;
    } else {
      throw new IllegalStateException("Neither HTTP/1 nor HTTP/2 headers set");
    }
  }

  public HttpMethod method() {
    if (h1Request != null) {
      return h1Request.method();
    } else if (h2Headers != null) {
      CharSequence rawMethod = h2Headers.method();
      if (rawMethod != null) {
        return new HttpMethod(rawMethod.toString());
      }
    }

    return null;
  }

  public CharSequence acceptHeader() {
    return header(HttpHeaderNames.ACCEPT);
  }

  public CharSequence contentTypeHeader() {
    return header(HttpHeaderNames.CONTENT_TYPE);
  }

  public CharSequence acceptCharsetHeader() {
    return header(HttpHeaderNames.ACCEPT_CHARSET);
  }
}
