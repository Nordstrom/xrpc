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

package com.nordstrom.xrpc.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;

class XUrlTest {

  String url1 = "https://api.nordstrom.com/foo/v1?foo=bar";
  String url2 = "https://api.nordstrom.com:8080/foo/v1?foo=bar";

  @Test
  void getHost() {
    assertEquals("api.nordstrom.com", XUrl.host(url1));
  }

  @Test
  void getHost_withPort() {
    assertEquals("api.nordstrom.com", XUrl.host(url2));
  }

  @Test
  void getPort() {
    assertEquals(443, XUrl.port(url1));
    assertEquals(8080, XUrl.port(url2));
    assertEquals(8080, XUrl.port("https://api.nordstrom.com:8080"));
  }

  @Test
  void getPath() {
    assertEquals("/foo/v1", XUrl.path(url1));
  }

  @Test
  void getPath_withUrlEncoding() {
    String path = XUrl.path("https://api.nordstrom.com:8080/foo/bar%20baz?foo=bar");
    assertEquals("/foo/bar baz", path);
  }

  @Test
  void stripUrlParameters() {
    assertEquals("https://api.nordstrom.com/foo/v1", XUrl.stripUrlParameters(url1));
  }

  @Test
  void stripQueryParameters() {
    assertEquals("?foo=bar", XUrl.rawQueryParameters(url1));
  }

  @Test
  void stripUrlParameters1() {}

  @Test
  void addProtocol() {}

  @Test
  void decodeQueryString1() {
    String query = "https://n.com?param1=value1&param2=value2&param3=value3";
    assertEquals("value1", XUrl.decodeQueryString(query).get("param1").get(0));
    assertEquals("value2", XUrl.decodeQueryString(query).get("param2").get(0));
    assertEquals("value3", XUrl.decodeQueryString(query).get("param3").get(0));
  }

  @Test
  void decodeQueryString2() {
    String query = "https://n.com?param1=value1&param2=&param3=value3&param3";
    assertEquals("value1", XUrl.decodeQueryString(query).get("param1").get(0));
    assertEquals("", XUrl.decodeQueryString(query).get("param2").get(0));
    assertEquals("value3", XUrl.decodeQueryString(query).get("param3").get(0));
    assertEquals("", XUrl.decodeQueryString(query).get("param3").get(1));
  }

  @Test
  void decodeQueryString_withUrlEncoding() {
    String query = "https://n.com?param1=value%201&param2=&param3=value%203&param3";
    assertEquals("value 1", XUrl.decodeQueryString(query).get("param1").get(0));
    assertEquals("", XUrl.decodeQueryString(query).get("param2").get(0));
    assertEquals("value 3", XUrl.decodeQueryString(query).get("param3").get(0));
    assertEquals("", XUrl.decodeQueryString(query).get("param3").get(1));
  }

  @Test
  void decodeQueryString3() {
    String noPath = "https://api.nordstrom.com";
    String noPathTrailingSlash = "https://api.nordstrom.com/";

    assertThrows(
        IndexOutOfBoundsException.class, () -> XUrl.decodeQueryString(noPath).get("param1").get(0));
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> XUrl.decodeQueryString(noPathTrailingSlash).get("param2").get(0));
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> XUrl.decodeQueryString(noPathTrailingSlash).get("param2").get(0));
    String noPathQuery = "https://api.nordstrom.com?foo=bar";
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> XUrl.decodeQueryString(noPathQuery).get("param3").get(0));
    String withPathNoQuery = "https://api.nordstrom.com/v1";
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> XUrl.decodeQueryString(withPathNoQuery).get("param3").get(1));
    String withPathTrailingSlashNoQuery = "https://api.nordstrom.com/v1/";
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> XUrl.decodeQueryString(withPathTrailingSlashNoQuery).get("param3").get(1));
    String withPathTrailingSlash = "https://api.nordstrom.com/v1/?foo=bar";
    assertThrows(
        IndexOutOfBoundsException.class,
        () -> XUrl.decodeQueryString(withPathTrailingSlash).get("param3").get(1));
  }

  @Test
  void getInetSocket() throws java.net.URISyntaxException {
    InetSocketAddress result = XUrl.inetSocket(url2);
    assertEquals("api.nordstrom.com", result.getHostString());
    assertEquals(8080, result.getPort());
  }

  @Test
  void getInetSocket_withNoPort() throws java.net.URISyntaxException {
    InetSocketAddress result = XUrl.inetSocket(url1);
    assertEquals("api.nordstrom.com", result.getHostString());
    assertEquals(443, result.getPort());
  }

  @Test
  void getInetSocket_withNoProtocol() throws java.net.URISyntaxException {
    InetSocketAddress result = XUrl.inetSocket("api.nordstrom.com/foo/v1?foo=bar");
    assertEquals("api.nordstrom.com", result.getHostString());
    assertEquals(80, result.getPort());
  }
}
