package com.nordstrom.xrpc.client;

import java.util.LinkedHashMap;

public class QueryStringMap<K, V> extends LinkedHashMap<K, V> {
  protected V defaultValue;
  public QueryStringMap(V defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public V get(Object k) {
    return containsKey(k) ? super.get(k) : defaultValue;
  }
}
