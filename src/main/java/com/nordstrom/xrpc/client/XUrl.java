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

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.QueryStringDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XUrl {

  public static String getHost(String url) {
    try {
      return getDomainChecked(url);
    } catch (URISyntaxException e) {
      log.info("Malformed url: " + url);
      return null;
    }
  }

  public static int getPort(String url) {
    Preconditions.checkNotNull(url);
    Matcher matcher = URL_PROTOCOL_REGEX.matcher(url);
    url = addProtocol(url);
    try {
      URI uri = new URI(url);
      if (uri.getPort() == -1) {
        if (!matcher.find()) {
          return 80;
        } else {
          return 443;
        }
      } else {
        return uri.getPort();
      }
    } catch (URISyntaxException e) {
      log.info("Malformed url: " + url);
      return -1;
    }
  }

  public static String getDomainChecked(String url) throws URISyntaxException {
    Preconditions.checkNotNull(url);
    url = addProtocol(url);
    return new URI(url).getHost();
  }

  public static String getPath(String url) {
    Preconditions.checkNotNull(url);
    url = addProtocol(url);
    try {
      return new URI(url).getPath();
    } catch (URISyntaxException e) {
      log.info("Malformed url: " + url);
      return null;
    }
  }

  public static String stripUrlParameters(String url) {
    Preconditions.checkNotNull(url);
    int paramStartIndex = url.indexOf("?");
    if (paramStartIndex == -1) {
      return url;
    } else {
      return url.substring(0, paramStartIndex);
    }
  }

  public static String getRawQueryParameters(String url) {
    Preconditions.checkNotNull(url);
    int paramStartIndex = url.indexOf("?");
    if (paramStartIndex == -1) {
      return url;
    } else {
      return url.substring(paramStartIndex, url.length());
    }
  }

  public static String stripUrlParameters(URL url) {
    return stripUrlParameters(url.toString());
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

  public static Map<String, List<String>> decodeQueryString(String url) {
    Preconditions.checkNotNull(url);
    QueryStringDecoder decoder = new QueryStringDecoder(getRawQueryParameters(url));
    Map<String, List<String>> params = new DefaultValueMap<>(ImmutableList.of());
    params.putAll(decoder.parameters());
    return params;
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
