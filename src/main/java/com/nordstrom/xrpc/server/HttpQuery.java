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

  /** Get a query parameter by key. */
  public Optional<String> parameter(String key) {
    return parameter(key, null);
  }

  /** Get a query parameter by key with a default value. */
  public Optional<String> parameter(String key, String defaultValue) {
    return Optional.of(
        Optional.of(parameters().get(key))
            .flatMap(list -> list.stream().findFirst())
            .orElse(defaultValue));
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
