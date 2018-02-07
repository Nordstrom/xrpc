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

import io.netty.handler.codec.http.QueryStringDecoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Parsed query string. */
public class HttpQuery {
  private final QueryStringDecoder decoder;

  /** Construct a HttpQuery based on a uri. */
  public HttpQuery(String uri) {
    this.decoder = new QueryStringDecoder(uri, true);
  }

  /** Source uri. */
  public String uri() {
    return decoder.uri();
  }

  /** URI path. */
  public String path() {
    return decoder.path();
  }

  /** Map of query parameters. */
  public Map<String, List<String>> parameters() {
    return decoder.parameters();
  }

  /** Get a the first query parameter by key, ignoring any extras. */
  public Optional<String> parameter(String key) {
    return Optional.ofNullable(parameter(key, null));
  }

  /**
   * Get a the first query parameter by key, ignoring any extras. If no parameter is found, returns
   * default value.
   */
  public String parameter(String key, String defaultValue) {
    return Optional.ofNullable(parameters().get(key))
        .flatMap(list -> list.stream().findFirst())
        .orElse(defaultValue);
  }

  /** Get original (raw) path. */
  public String rawPath() {
    return decoder.rawPath();
  }

  /** Get raw query string. */
  public String rawQuery() {
    return decoder.rawQuery();
  }
}
