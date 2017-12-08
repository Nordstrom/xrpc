package com.nordstrom.xrpc.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

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
  void getDomainChecked() {}

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
  void stripUrlParameters1() {}

  @Test
  void addProtocol() {}

  @Test
  void decodeQString1() {
    String qString = "https://n.com?param1=value1&param2=value2&param3=value3";
    assertEquals("value1", XUrl.decodeQueryString(qString).get("param1").get(0));
    assertEquals("value2", XUrl.decodeQueryString(qString).get("param2").get(0));
    assertEquals("value3", XUrl.decodeQueryString(qString).get("param3").get(0));
  }

  @Test
  void decodeQString2() {
    String qString = "https://n.com?param1=value1&param2=&param3=value3&param3";
    assertEquals("value1", XUrl.decodeQueryString(qString).get("param1").get(0));
    assertEquals(null, XUrl.decodeQueryString(qString).get("param2").get(0));
    assertEquals("value3", XUrl.decodeQueryString(qString).get("param3").get(0));
    assertEquals(null, XUrl.decodeQueryString(qString).get("param3").get(1));
  }

  @Test
  void decodeQString3() {
    String noPath = "https://api.nordstrom.com";
    String noPathTrailingSlash = "https://api.nordstrom.com/";
    String noPathQuery = "https://api.nordstrom.com?foo=bar";
    String withPathNoQuery = "https://api.nordstrom.com/v1";
    String withPathTrailingSlashNoQuery = "https://api.nordstrom.com/v1/";
    String withPathTrailingSlash = "https://api.nordstrom.com/v1/?foo=bar";

    //TODO(JR): These use cases currently throw NPEs. We need to do a better job of making these edges less sharp
    //    assertEquals("value1", XUrl.decodeQueryString(noPath).get("param1").get(0));
    //    assertEquals(null, XUrl.decodeQueryString(noPathTrailingSlash).get("param2").get(0));
    //    assertEquals("value3", XUrl.decodeQueryString(noPathQuery).get("param3").get(0));
    //    assertEquals(null, XUrl.decodeQueryString(withPathNoQuery).get("param3").get(1));
    //    assertEquals(null, XUrl.decodeQueryString(withPathTrailingSlashNoQuery).get("param3").get(1));
    //    assertEquals(null, XUrl.decodeQueryString(withPathTrailingSlash).get("param3").get(1));

  }
}
