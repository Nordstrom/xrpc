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
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.Http2Headers;
import java.nio.charset.Charset;
import java.util.Map;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/** Xprc specific Request object. */
@Slf4j
@Accessors(fluent = true)
public class XrpcRequest {
  /** The request to handle. */
  @Getter private final FullHttpRequest h1Request;

  @Getter private final Http2Headers h2Headers;
  @Getter private final EventLoop eventLoop;
  @Getter private final Channel upstreamChannel;
  @Getter private final ByteBufAllocator alloc;

  @Getter private final XrpcConnectionContext connectionContext;
  /** The variables captured from the route path. */
  private final Map<String, String> groups;

  /** The parsed query string. */
  private HttpQuery query;

  private ResponseFactory responseFactory;

  private final int streamId;
  private ByteBuf body;

  public XrpcRequest(
      FullHttpRequest request,
      XrpcConnectionContext connectionContext,
      Map<String, String> groups,
      Channel channel) {
    this.h1Request = request;
    this.h2Headers = null;
    this.connectionContext = connectionContext;
    this.groups = groups;
    this.upstreamChannel = channel;
    this.alloc = channel.alloc();
    this.eventLoop = channel.eventLoop();
    this.streamId = -1;
  }

  public XrpcRequest(
      Http2Headers headers,
      XrpcConnectionContext connectionContext,
      Map<String, String> groups,
      Channel channel,
      int streamId) {
    this.h1Request = null;
    this.h2Headers = headers;
    this.connectionContext = connectionContext;
    this.groups = groups;
    this.upstreamChannel = channel;
    this.alloc = channel.alloc();
    this.eventLoop = channel.eventLoop();
    this.streamId = streamId;
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

  public ResponseFactory response() {
    if (responseFactory == null) {
      responseFactory = new ResponseFactory(this);
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

  public void writeBody(byte[] bytes) {
    if (body == null) {
      body = byteBuf();
    }

    body.writeBytes(bytes);
  }

  public void writeBody(ByteBuf buff) {
    if (body == null) {
      body = byteBuf();
    }

    body.writeBytes(buff);
  }

  @SneakyThrows
  public <T> T body(Class<T> clazz) {
    Decoder decoder = connectionContext.decoders().decoder(contentType());
    return decoder.decode(body(), contentType(), clazz);
  }

  public ByteBuf body() {
    if (body == null) {
      return byteBuf();
    }

    return body;
  }

  /**
   * Returns a new string representing the request writeBody, decoded using the appropriate charset.
   */
  public String bodyText() {
    // Note that this defaults to iso-8859-1 per
    // https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7.1.
    Charset charset = HttpUtil.getCharset(contentType());
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

  public CharSequence acceptHeader() {
    return header(HttpHeaderNames.ACCEPT);
  }

  public CharSequence contentType() {
    return header(HttpHeaderNames.CONTENT_TYPE);
  }
}
