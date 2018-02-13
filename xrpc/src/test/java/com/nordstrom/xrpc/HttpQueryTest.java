package com.nordstrom.xrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nordstrom.xrpc.server.HttpQuery;
import com.nordstrom.xrpc.server.XrpcRequest;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HttpQueryTest {

  @Test
  void testQueryParameters() {
    HttpQuery q = new HttpQuery("?p1=v1&p2=v2");
    assertEquals("v1", q.parameter("p1").get());
  }

  @Test
  void testQueryParametersEmpty() {
    HttpQuery q = new HttpQuery("");
    assertEquals(Optional.empty(), q.parameter("foo"));
  }

  @Test
  void testQueryParameterWithDefault() {
    HttpQuery q = new HttpQuery("?p1=v1&p2=v2");
    assertEquals("v2", q.parameter("p2", "default"));
  }

  @Test
  void testQueryParameterWithDefaultEmpty() {
    HttpQuery q = new HttpQuery("");
    assertEquals("default", q.parameter("p2", "default"));
  }

  @Test
  void testH1Query() {
    FullHttpRequest rawReq =
        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "?p1=v1");
    XrpcRequest r = new XrpcRequest(rawReq, null, null, new EmbeddedChannel());
    assertNotNull(r);
    assertEquals("v1", r.query().parameter("p1").get());
  }

  @Test
  void testH2Query() {
    DefaultHttp2Headers headers = new DefaultHttp2Headers();
    headers.path("?p1=v1");
    XrpcRequest r = new XrpcRequest(headers, null, null, new EmbeddedChannel(), 0);
    assertNotNull(r);
    assertEquals("v1", r.query().parameter("p1").get());
  }

  @Test
  void testQueryWithIllegalState() {
    assertThrows(
        IllegalStateException.class,
        () -> new XrpcRequest(null, null, null, new EmbeddedChannel()).query());
  }
}
