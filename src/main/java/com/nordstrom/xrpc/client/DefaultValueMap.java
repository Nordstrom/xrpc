package com.nordstrom.xrpc.client;

import java.util.LinkedHashMap;

/**
 * A map that returns a default value instead of null when a given key is absent.
 *
 * <p>This is the data structure returned for decoded query string parameters. It is meant to soften
 * the edges around null-pointer errors.
 */
public class DefaultValueMap<K, V> extends LinkedHashMap<K, V> {
  private final V defaultValue;

  public DefaultValueMap(V defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public V get(Object k) {
    return containsKey(k) ? super.get(k) : defaultValue;
  }
}
