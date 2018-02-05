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

  /** URI path */
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

  /** Get original (raw) path */
  public String rawPath() {
    return decoder.rawPath();
  }

  /** Get raw query string */
  public String rawQuery() {
    return decoder.rawQuery();
  }
}
