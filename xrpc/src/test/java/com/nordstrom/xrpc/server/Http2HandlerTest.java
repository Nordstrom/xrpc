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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.XrpcConstants;
import com.nordstrom.xrpc.server.http.Recipes;
import com.typesafe.config.ConfigFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.Optional;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class Http2HandlerTest {
  private static final int MAX_PAYLOAD = 1024;
  /** Path which has a handler registered. */
  private static final String OK_PATH = "/foo";
  /** Handler for OK_PATH and /bar/{param}. Echoes any request body back. */
  private static final Handler OK_HANDLER =
      request -> {
        ByteBuf data = request.getData();
        if (data.readableBytes() > 0) {
          return Recipes.newResponse(
              HttpResponseStatus.OK, data, Recipes.ContentType.Application_Octet_Stream);
        } else {
          return Recipes.newResponse(HttpResponseStatus.OK);
        }
      };
  /** Path prefix which has a handler with paramter. */
  private static final String PARAM_PATH_PREFIX = "/bar";
  /** Group name for the path with a group. */
  private static final String PARAM_NAME = "param";
  /** Stream ID used in most tests. */
  private static final int STREAM_ID = 33;

  private MetricRegistry metricRegistry = new MetricRegistry();

  private Meter requestMeter = metricRegistry.meter("requests");

  private EmbeddedChannel channel = new EmbeddedChannel();

  private Http2Headers headers = new DefaultHttp2Headers();

  private XrpcConnectionContext xrpcContext;

  private Http2Handler testHandler;

  @Mock private ChannelHandlerContext mockContext;

  @Mock private Http2Connection mockConnection;

  @Mock private Http2ConnectionEncoder mockEncoder;

  @BeforeEach
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    when(mockEncoder.connection()).thenReturn(mockConnection);
    when(mockContext.channel()).thenReturn(channel);
  }

  @BeforeEach
  public void initContext() {
    XrpcConnectionContext.Builder contextBuilder =
        XrpcConnectionContext.builder().requestMeter(requestMeter);
    Server.addResponseCodeMeters(contextBuilder, metricRegistry);
    RouteBuilder routeBuilder = new RouteBuilder();
    routeBuilder
        .get(OK_PATH, OK_HANDLER)
        .get(String.format("%s/{%s}", PARAM_PATH_PREFIX, PARAM_NAME), OK_HANDLER);
    contextBuilder.routes(routeBuilder.compile(metricRegistry));
    xrpcContext = contextBuilder.build();

    channel.attr(XrpcConstants.CONNECTION_CONTEXT).set(xrpcContext);
  }

  /** Helper which verifies that no response was written to the mock encoder. */
  void verifyNoResponse() {
    verify(mockEncoder, never())
        .writeHeaders(any(), anyInt(), any(), anyInt(), anyBoolean(), any());
    verify(mockEncoder, never()).writeData(any(), anyInt(), any(), anyInt(), anyBoolean(), any());
  }

  /** Helper which verifies that the given response data was written to the mock encoder. */
  void verifyResponse(HttpResponseStatus status, Optional<ByteBuf> responseBody, int streamId) {
    ArgumentMatcher<Http2Headers> matchesStatus =
        new ArgumentMatcher<Http2Headers>() {
          @Override
          public boolean matches(Http2Headers headers) {
            return status.codeAsText().equals(headers.status());
          }

          @Override
          public String toString() {
            return String.format("Http2Headers[:status: %s]", status.codeAsText());
          }
        };
    if (responseBody.isPresent()) {
      // Both headers and data should've been sent.
      verify(mockEncoder, times(1))
          .writeHeaders(
              eq(mockContext), eq(streamId), argThat(matchesStatus), anyInt(), eq(false), any());
      verify(mockEncoder, times(1))
          .writeData(
              eq(mockContext), eq(streamId), eq(responseBody.get()), anyInt(), eq(true), any());
    } else {
      // Only headers should've been sent.
      verify(mockEncoder, times(1))
          .writeHeaders(
              eq(mockContext), eq(streamId), argThat(matchesStatus), anyInt(), eq(true), any());
      verify(mockEncoder, never()).writeData(any(), anyInt(), any(), anyInt(), anyBoolean(), any());
    }

    // Verify that the response meter was marked.
    assertEquals(
        1L,
        xrpcContext.metersByStatusCode().get(status).getCount(),
        "meter " + status.codeAsText() + " should have been marked");
  }

  /** Helper for testing CORS requests. */
  public CorsConfig corsConfig() {
    val config = ConfigFactory.load("test.conf").getConfig("xrpc");
    XConfig xconfig = new XConfig(config);
    return xconfig.corsConfig();
  }

  /** Matcher for preflight headers. */
  ArgumentMatcher<Http2Headers> matchesPreflightHeaders(CorsConfig corsConfig) {
    return new ArgumentMatcher<Http2Headers>() {
      @Override
      public boolean matches(Http2Headers headers) {
        return HttpResponseStatus.OK.codeAsText().equals(headers.status())
            && corsConfig
                .allowedRequestMethods()
                .toString()
                .equals(headers.get("access-control-allow-methods"));
      }

      @Override
      public String toString() {
        return String.format(
            "Http2Headers[:access-control-allow-methods: %s, :status: %s]",
            corsConfig.allowedRequestMethods().toString(), HttpResponseStatus.OK.codeAsText());
      }
    };
  }

  /** Matcher for access control headers. */
  ArgumentMatcher<Http2Headers> matchesAllowOriginsHeaders(CorsConfig corsConfig) {
    return new ArgumentMatcher<Http2Headers>() {
      @Override
      public boolean matches(Http2Headers headers) {
        return HttpResponseStatus.OK.codeAsText().equals(headers.status())
            && corsConfig.origin().equals(headers.get("access-control-allow-origin"));
      }

      @Override
      public String toString() {
        return String.format(
            "Http2Headers[:access-control-allow-origin: %s, :status: %s]",
            "test.domain", HttpResponseStatus.OK.codeAsText());
      }
    };
  }

  /** Matcher for forbidden response headers. */
  ArgumentMatcher<Http2Headers> matchesForbiddenHeaders() {
    return new ArgumentMatcher<Http2Headers>() {
      @Override
      public boolean matches(Http2Headers headers) {
        return HttpResponseStatus.FORBIDDEN.codeAsText().equals(headers.status());
      }

      @Override
      public String toString() {
        return String.format(
            "Http2Headers[:status: %s]", HttpResponseStatus.FORBIDDEN.codeAsText());
      }
    };
  }

  @Test
  public void getPathFromHeaders_withQueryString() {
    headers.path("/foo/extracted?query1=abc&query2=123");

    String path = Http2Handler.getPathFromHeaders(headers);

    assertEquals("/foo/extracted", path);
  }

  @Test
  public void getPathFromHeaders_withNoQueryString() {
    headers.path("/foo/extracted");

    String path = Http2Handler.getPathFromHeaders(headers);

    assertEquals("/foo/extracted", path);
  }

  @Test
  public void constructorRegistersListener() {
    testHandler = new Http2Handler(mockEncoder, MAX_PAYLOAD);
    verify(mockConnection).addListener(testHandler);
  }

  /**
   * Tests that a XRPC_SOFT_RATE_LIMITED attribute being set will trigger a TOO_MANY_REQUESTS
   * response.
   */
  @Test
  public void testOnHeadersRead_softRateLimited() {
    testHandler = new Http2Handler(mockEncoder, MAX_PAYLOAD);

    channel.attr(XrpcConstants.XRPC_SOFT_RATE_LIMITED).set(Boolean.TRUE);

    testHandler.onHeadersRead(mockContext, STREAM_ID, headers, 1, false);

    // Verify that a request was counted.
    assertEquals(1L, requestMeter.getCount());
    // Verify a TOO_MANY_REQUESTS response.
    verifyResponse(
        HttpResponseStatus.TOO_MANY_REQUESTS,
        Optional.of(Unpooled.wrappedBuffer(XrpcConstants.RATE_LIMIT_RESPONSE)),
        STREAM_ID);
  }

  /** Test that a headers-only request to a good path is handled appropriately. */
  @Test
  public void testOnHeadersRead_fullRequestGoodPath() {
    testHandler = new Http2Handler(mockEncoder, MAX_PAYLOAD);

    headers.method("GET").path(OK_PATH);

    // Call with endOfStream set to true.
    testHandler.onHeadersRead(mockContext, STREAM_ID, headers, 1, true);

    assertEquals(1L, requestMeter.getCount());
    // Verify an OK response.
    verifyResponse(HttpResponseStatus.OK, Optional.empty(), STREAM_ID);
  }

  /** Test that headers with data expected is handled appropriately. */
  @Test
  public void testOnHeadersRead_headersStreamContinuing() throws Exception {
    testHandler = new Http2Handler(mockEncoder, MAX_PAYLOAD);

    headers.method("GET").path(PARAM_PATH_PREFIX + "/group");

    // Call with endOfStream set to false; we shouldn't see a response.
    testHandler.onHeadersRead(mockContext, STREAM_ID, headers, 1, false);

    assertEquals(1L, requestMeter.getCount());
    verifyNoResponse();

    // Verify that the request and handler were queued up.
    XrpcRequest xrpcRequest = testHandler.requests.get(STREAM_ID);
    assertNotNull(xrpcRequest);
    assertEquals(headers, xrpcRequest.h2Headers());
    assertEquals("group", xrpcRequest.variable(PARAM_NAME));
    Handler handler = testHandler.handlers.get(STREAM_ID);
    assertNotNull(handler);
    assertEquals(HttpResponseStatus.OK, handler.handle(xrpcRequest).status());
  }

  /** Test that headers with too-large content-length gets a REQUEST_ENTITY_TOO_LARGE response. */
  @Test
  public void testOnHeadersRead_contentLengthTooLarge() {
    testHandler = new Http2Handler(mockEncoder, MAX_PAYLOAD);

    headers.method("GET").path(OK_PATH).addLong(HttpHeaderNames.CONTENT_LENGTH, MAX_PAYLOAD + 10L);

    testHandler.onHeadersRead(mockContext, STREAM_ID, headers, 1, false);

    assertEquals(1L, requestMeter.getCount());
    verifyResponse(
        HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE,
        Optional.of(Unpooled.wrappedBuffer(XrpcConstants.PAYLOAD_EXCEEDED_RESPONSE)),
        STREAM_ID);
  }

  /** Test that malformed too-large content-length is ignored. */
  @Test
  public void testOnHeadersRead_contentLengthMalformed() {
    testHandler = new Http2Handler(mockEncoder, MAX_PAYLOAD);

    headers.method("GET").path(OK_PATH).add(HttpHeaderNames.CONTENT_LENGTH, "abc");

    testHandler.onHeadersRead(mockContext, STREAM_ID, headers, 1, true);

    // Expect an OK response.
    assertEquals(1L, requestMeter.getCount());
    verifyResponse(HttpResponseStatus.OK, Optional.empty(), STREAM_ID);
  }

  /** Test that trailer-part headers are handled correctly. */
  @Test
  public void testOnHeadersRead_trailerPart() {
    testHandler = new Http2Handler(mockEncoder, MAX_PAYLOAD);

    // Fake the initial request + handler.
    Http2Headers initialHeaders = new DefaultHttp2Headers().method("GET").path(OK_PATH);
    XrpcRequest fakeRequest = new XrpcRequest(initialHeaders, null, null, channel);
    testHandler.requests.put(STREAM_ID, fakeRequest);
    testHandler.handlers.put(STREAM_ID, OK_HANDLER);

    headers.add("some-header", "some-value");
    testHandler.onHeadersRead(mockContext, STREAM_ID, headers, 1, true);

    // Expect an OK response, but DON'T expect a request count.
    assertEquals(0L, requestMeter.getCount());
    verifyResponse(HttpResponseStatus.OK, Optional.empty(), STREAM_ID);
    // Assert that the request's headers were updated.
    assertEquals("some-value", fakeRequest.h2Headers().get("some-header"));
  }

  /** Test that several data frames will be aggregated into a response. */
  @Test
  public void testOnDataRead_dataAggregated() {
    testHandler = new Http2Handler(mockEncoder, MAX_PAYLOAD);

    // Create a fake request to aggregate data into.
    XrpcRequest fakeRequest = new XrpcRequest((Http2Headers) null, null, null, channel);
    testHandler.requests.put(STREAM_ID, fakeRequest);

    // Append several data frames.
    testHandler.onDataRead(
        mockContext, STREAM_ID, Unpooled.wrappedBuffer(new byte[] {1}), 0, false);
    testHandler.onDataRead(
        mockContext, STREAM_ID, Unpooled.wrappedBuffer(new byte[] {2}), 0, false);
    testHandler.onDataRead(
        mockContext, STREAM_ID, Unpooled.wrappedBuffer(new byte[] {3}), 0, false);

    // Assert that the request has all the data needed.
    assertEquals(Unpooled.wrappedBuffer(new byte[] {1, 2, 3}), fakeRequest.getData());
  }

  /** Test that end-of-stream data frames execute a handler. */
  @Test
  public void testOnDataRead_endOfStreamExecutes() {
    testHandler = new Http2Handler(mockEncoder, MAX_PAYLOAD);

    // Create a fake request and handler.
    XrpcRequest fakeRequest = new XrpcRequest((Http2Headers) null, null, null, channel);
    testHandler.requests.put(STREAM_ID, fakeRequest);
    testHandler.handlers.put(STREAM_ID, OK_HANDLER);

    // Send a mostly-empty data frame.
    testHandler.onDataRead(
        mockContext, STREAM_ID, Unpooled.wrappedBuffer(new byte[] {0x20}), 0, true);

    // Verify an OK response.
    assertEquals(0L, requestMeter.getCount());
    verifyResponse(
        HttpResponseStatus.OK, Optional.of(Unpooled.wrappedBuffer(new byte[] {0x20})), STREAM_ID);
  }

  /** Test that getting too much data will return a REQUEST_ENTITY_TOO_LARGE response. */
  @Test
  public void testOnDataRead_payloadTooLarge() {
    testHandler = new Http2Handler(mockEncoder, MAX_PAYLOAD);

    // Create a fake request.
    XrpcRequest fakeRequest = new XrpcRequest((Http2Headers) null, null, null, channel);
    testHandler.requests.put(STREAM_ID, fakeRequest);

    // Start with a small payload, then add a bigger one.
    testHandler.onDataRead(mockContext, STREAM_ID, Unpooled.wrappedBuffer(new byte[10]), 0, false);
    verifyNoResponse();
    testHandler.onDataRead(
        mockContext, STREAM_ID, Unpooled.wrappedBuffer(new byte[MAX_PAYLOAD - 5]), 0, true);

    // Verify a TOO_MANY_REQUESTS response.
    assertEquals(0L, requestMeter.getCount());
    verifyResponse(
        HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE,
        Optional.of(Unpooled.wrappedBuffer(XrpcConstants.PAYLOAD_EXCEEDED_RESPONSE)),
        STREAM_ID);
  }

  /** Test that OPTIONS request short circuit to preflight response. */
  @Test
  public void testOnHeadersRead_preflightOptionsRequest() {
    CorsConfig corsConfig = corsConfig();
    Http2CorsHandler corsHandler = new Http2CorsHandler(corsConfig);

    testHandler = new Http2Handler(mockEncoder, MAX_PAYLOAD, corsHandler);

    headers
        .method("OPTIONS")
        .add("origin", "test.domain")
        .add("access-control-request-method", "GET")
        .path(OK_PATH);

    testHandler.onHeadersRead(mockContext, STREAM_ID, headers, 1, false);
    assertEquals(1L, requestMeter.getCount());
    verify(mockEncoder, times(1))
        .writeHeaders(
            eq(mockContext),
            eq(STREAM_ID),
            argThat(matchesAllowOriginsHeaders(corsConfig)),
            anyInt(),
            eq(true),
            any());
    verify(mockEncoder, times(1))
        .writeHeaders(
            eq(mockContext),
            eq(STREAM_ID),
            argThat(matchesPreflightHeaders(corsConfig)),
            anyInt(),
            eq(true),
            any());
    verify(mockEncoder, never()).writeData(any(), anyInt(), any(), anyInt(), anyBoolean(), any());
  }

  /** Test that OPTIONS request short circuit to preflight response. */
  @Test
  public void testOnHeadersRead_corsShortCircuit() {
    CorsConfig corsConfig = corsConfig();
    Http2CorsHandler corsHandler = new Http2CorsHandler(corsConfig);

    testHandler = new Http2Handler(mockEncoder, MAX_PAYLOAD, corsHandler);

    headers.method("GET").add("origin", "illegal.domain").path(OK_PATH);

    testHandler.onHeadersRead(mockContext, STREAM_ID, headers, 1, false);
    assertEquals(1L, requestMeter.getCount());
    verify(mockEncoder, times(1))
        .writeHeaders(
            eq(mockContext),
            eq(STREAM_ID),
            argThat(matchesForbiddenHeaders()),
            anyInt(),
            eq(true),
            any());
    verify(mockEncoder, never()).writeData(any(), anyInt(), any(), anyInt(), anyBoolean(), any());
  }
}
