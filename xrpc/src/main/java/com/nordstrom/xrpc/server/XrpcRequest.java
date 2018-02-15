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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.nordstrom.xrpc.server.http.Recipes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** Xprc specific Request object. */
@Slf4j
public class XrpcRequest {
  /** The request to handle. */
  @Getter private final FullHttpRequest h1Request;

  @Getter private final Http2Headers h2Headers;
  @Getter private final EventLoop eventLoop;
  @Getter private final Channel upstreamChannel;
  @Getter private final ByteBufAllocator alloc;

  @Getter(AccessLevel.PACKAGE)
  private final XrpcConnectionContext connectionContext;
  /** The variables captured from the route path. */
  private final Map<String, String> groups;

  /** The parsed query string. */
  private HttpQuery query;

  private final int streamId;
  private ByteBuf data;

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

  /** Returns the variable with the given name, or null if that variable doesn't exist. */
  public String variable(String name) {
    return groups.get(name);
  }

  /** Create a convenience function to prevent direct access to the Allocator. */
  public ByteBuf getByteBuf() {
    return alloc.compositeDirectBuffer();
  }

  public void setData(byte[] bytes) {
    if (data == null) {
      data = getByteBuf();
    }

    data.writeBytes(bytes);
  }

  public void setData(ByteBuf buff) {
    if (data == null) {
      data = getByteBuf();
    }

    data.writeBytes(buff);
  }

  public ByteBuf getData() {
    if (data == null) {
      return getByteBuf();
    }

    return data;
  }

  /** Returns a new string representing the request data, decoded using the appropriate charset. */
  public String getDataAsString() {
    // Note that this defaults to iso-8859-1 per
    // https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7.1.
    Charset charset = HttpUtil.getCharset(getHeader(HttpHeaderNames.CONTENT_TYPE));
    return getData().toString(charset);
  }

  /**
   * Returns the value of the given HTTP header, or null if it's missing.
   *
   * @param name the header name, lower-cased
   */
  public CharSequence getHeader(CharSequence name) {
    if (h1Request != null) {
      return h1Request.headers().get(name);
    } else if (h2Headers != null) {
      return h2Headers.get(name);
    } else {
      throw new IllegalStateException("Neither HTTP/1 nor HTTP/2 headers set");
    }
  }

  public ListeningExecutorService getExecutor() {
    // For more info see https://github.com/google/guava/wiki/ListenableFutureExplained
    return MoreExecutors.listeningDecorator(eventLoop);
  }

  public FullHttpRequest getHttpRequest() {
    if (h1Request != null) {
      return h1Request;
    }

    if (h2Headers != null) {
      try {
        // Fake out a full HTTP request.
        FullHttpRequest synthesizedRequest =
            HttpConversionUtil.toFullHttpRequest(0, h2Headers, alloc, true);
        if (data != null) {
          synthesizedRequest.replace(data);
        }

        return synthesizedRequest;
      } catch (Http2Exception e) {
        // TODO(JR): Do something more meaningful with this exception
        e.printStackTrace();
      }
    }

    throw new IllegalStateException("Cannot get the http request for an empty XrpcRequest");
  }

  public FullHttpResponse jsonResponse(HttpResponseStatus status, Object body) throws IOException {
    return Recipes.newResponse(
        status, encodeJsonBody(body), Recipes.ContentType.Application_Json, Collections.emptyMap());
  }

  public FullHttpResponse okResponse() {
    return Recipes.newResponseOk();
  }

  public FullHttpResponse okJsonResponse(Object body) throws IOException {

    return jsonResponse(HttpResponseStatus.OK, body);
  }

  public FullHttpResponse notFoundJsonResponse(Object body) throws IOException {
    return jsonResponse(HttpResponseStatus.NOT_FOUND, body);
  }

  public FullHttpResponse badRequestJsonResponse(Object body) throws IOException {
    return jsonResponse(HttpResponseStatus.BAD_REQUEST, body);
  }

  private ByteBuf encodeJsonBody(Object body) throws IOException {
    ByteBuf buf = getAlloc().directBuffer();
    OutputStream stream = new ByteBufOutputStream(buf);
    connectionContext.getMapper().writeValue(stream, body);
    return buf;
  }
}
