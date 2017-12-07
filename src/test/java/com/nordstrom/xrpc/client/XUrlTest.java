package com.nordstrom.xrpc.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XUrlTest {

  String url1 = "https://api.nordstrom.com/foo/v1?foo=bar";
  String url2 = "https://api.nordstrom.com:8080/foo/v1?foo=bar";

  @Test
  void getHost() {
    assertEquals("api.nordstrom.com", XUrl.getHost(url1));
  }

  @Test
  void getPort() {
    assertEquals(443, XUrl.getPort(url1));
    assertEquals(8080, XUrl.getPort(url2));

  }

  @Test
  void getDomainChecked() {
  }

  @Test
  void getPath() {
    assertEquals("/foo/v1", XUrl.getPath(url1));
  }

  @Test
  void stripUrlParameters() {
    assertEquals("https://api.nordstrom.com/foo/v1", XUrl.stripUrlParameters(url1));
  }

  @Test
  void stripQueryParameters() {
    assertEquals("foo=bar", XUrl.stripQueryParameters(url1));
  }

  @Test
  void stripUrlParameters1() {
  }

  @Test
  void addProtocol() {
  }

}
