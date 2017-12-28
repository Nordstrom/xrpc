package com.nordstrom.xrpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;


public class Http2HandlerTest {
  @Test
  public void getPathFromHeaders () {
    Http2Headers mockHeaders = mock(Http2Headers.class);
    when(mockHeaders.path()).thenReturn("/foo/extracted?query1=abc&query2=123");

    Http2Handler testHandler = new Http2Handler(mock(Http2ConnectionDecoder.class), mock(Http2ConnectionEncoder.class), new Http2Settings());

    String path = testHandler.getPathFromHeaders(mockHeaders);

    assertEquals("/foo/extracted", path);
  }
}
