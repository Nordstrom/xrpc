package com.nordstrom.xrpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.netty.handler.codec.http2.Http2Headers;
import org.junit.jupiter.api.Test;

public class Http2HandlerTest {
  @Test
  public void getPathFromHeaders_withQueryString() {
    Http2Headers mockHeaders = mock(Http2Headers.class);
    when(mockHeaders.path()).thenReturn("/foo/extracted?query1=abc&query2=123");

    String path = Http2Handler.getPathFromHeaders(mockHeaders);

    assertEquals("/foo/extracted", path);
  }

  @Test
  public void getPathFromHeaders_withNoQueryString() {
    Http2Headers mockHeaders = mock(Http2Headers.class);
    when(mockHeaders.path()).thenReturn("/foo/extracted");

    String path = Http2Handler.getPathFromHeaders(mockHeaders);

    assertEquals("/foo/extracted", path);
  }
}
