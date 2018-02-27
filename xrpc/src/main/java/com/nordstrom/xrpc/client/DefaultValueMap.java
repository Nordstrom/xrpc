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

import java.util.LinkedHashMap;

/**
 * A map that returns a default value instead of null when a given key is absent.
 *
 * <p>This is the writeBody structure returned for decoded query string parameters. It is meant to
 * soften the edges around null-pointer errors.
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
