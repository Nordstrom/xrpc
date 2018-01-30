package com.nordstrom.xrpc.server;

import io.netty.handler.codec.http.QueryStringDecoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.val;

/** Parsed query string */
public class HttpQuery {
  private String uri;
  private QueryStringDecoder decoder;

  /** Construct a HttpQuery based on a uri */
  public HttpQuery(String uri) {
    this.uri = uri;
  }

  /** Source uri */
  public String uri() {
    return decoder().uri();
  }

  /** URI path */
  public String path() {
    return decoder().path();
  }

  /** Map of query parameters */
  public Map<String, List<String>> parameters() {
    return decoder().parameters();
  }

  /** Get a query parameter by key */
  public Optional<String> parameter(String key) {
    return parameter(key, null);
  }

  /** Get a query parameter by key with a default value */
  public Optional<String> parameter(String key, String defaultValue) {
    val value = parameters().get(key);
    if (value == null || value.size() < 1) return Optional.of(defaultValue);
    return Optional.of(value.get(0));
  }

  /** Get original (raw) path */
  public String rawPath() {
    return decoder().rawPath();
  }

  /** Get raw query string */
  public String rawQuery() {
    return decoder().rawQuery();
  }

  private QueryStringDecoder decoder() {
    if (decoder == null) {
      decoder = new QueryStringDecoder(uri, true);
    }
    return decoder;
  }
}
