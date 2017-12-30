package com.nordstrom.xrpc.client;

/*
 * Copyright 2017 Nordstrom, Inc.
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

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.lang.Integer.parseInt;
import lombok.extern.slf4j.Slf4j;

import io.netty.handler.codec.http.QueryStringDecoder;

//    Stopwatch stopwatch = Stopwatch.createStarted();
//    // codes
//    stopwatch.stop();
//    stopwatch.elapsed(TimeUnit.MILLISECONDS);
//    log.info("My Code took this much time " + stopwatch );

@Slf4j
public class XUrl {

  public static String getHost(String url) {
    Preconditions.checkNotNull(url);

    QueryStringDecoder decoder = new QueryStringDecoder(url);
    String intermediaryHost = decoder.path();
    intermediaryHost = stripProtocol(intermediaryHost);

    int portStart = intermediaryHost.indexOf(":");
    if (portStart != -1) {
      return intermediaryHost.substring(0, portStart);
    }

    int pathStart = intermediaryHost.indexOf("/");
    return intermediaryHost.substring(0, pathStart);
  }

  public static int getPort(String url) {
    Preconditions.checkNotNull(url);
    Matcher matcher = URL_PROTOCOL_REGEX.matcher(url);

    url = stripProtocol(url);
    url = stripQueryString(url);

    int portStart = url.indexOf(":");
    int pathStart = url.indexOf("/");

    if (portStart == -1) {
      if (!matcher.find()) {
        return 80;
      } else {
        return 443;
      }
    } else {
      return parseInt(url.substring(portStart + 1, pathStart));
    }
  }

  public static String getPath(String url) {
    Preconditions.checkNotNull(url);
    QueryStringDecoder decoder = new QueryStringDecoder(url);
    String intermediaryPath = decoder.path();
    intermediaryPath = stripProtocol(intermediaryPath);
    int pathStart = intermediaryPath.indexOf("/");
    return intermediaryPath.substring(pathStart);
  }

  public static String stripQueryString(String url) {
    Preconditions.checkNotNull(url);
    int paramStartIndex = url.indexOf("?");
    if (paramStartIndex == -1) {
      return url;
    } else {
      return url.substring(0, paramStartIndex);
    }
  }

  public static String getRawQueryString(String url) {
    Preconditions.checkNotNull(url);
    int paramStartIndex = url.indexOf("?");
    if (paramStartIndex == -1) {
      return url;
    } else {
      return url.substring(paramStartIndex + 1, url.length());
    }
  }

  public static Map<String, List<String>> decodeQueryString(String url) {
    QueryStringDecoder decoder = new QueryStringDecoder(url);
    Map<String, List<String>> params = new QueryStringMap<>(new ArrayList<String>());
    params.putAll(decoder.parameters());
    return params;
  }

  private static final Pattern URL_PROTOCOL_REGEX =
      Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE);

  public static String addProtocol(String url) {
    Preconditions.checkNotNull(url);

    Matcher matcher = URL_PROTOCOL_REGEX.matcher(url);
    if (!matcher.find()) {
      url = "http://" + url;
    }
    return url;
  }

  public static String stripProtocol(String url) {
    Preconditions.checkNotNull(url);

    Matcher matcher = URL_PROTOCOL_REGEX.matcher(url);
    if (!matcher.find()) {
      return url;
    }
    return matcher.replaceFirst("");
  }

  public static InetSocketAddress getInetSocket(String url) throws URISyntaxException {
    Preconditions.checkNotNull(url);
    Matcher matcher = URL_PROTOCOL_REGEX.matcher(url);
    url = addProtocol(url);
    URI uri = new URI(url);
    if (uri.getPort() == -1) {
      if (!matcher.find()) {
        return new InetSocketAddress(uri.getHost(), 80);
      } else {
        return new InetSocketAddress(uri.getHost(), 443);
      }
    } else {
      return new InetSocketAddress(uri.getHost(), uri.getPort());
    }
  }
}
