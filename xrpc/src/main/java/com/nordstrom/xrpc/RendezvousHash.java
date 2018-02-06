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

package com.nordstrom.xrpc;

import com.google.common.collect.Sets;
import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RendezvousHash<N> {

  private final HashFunction hasher;
  private final Funnel<N> nodeFunnel;
  private final Map<Long, N> hashMap = new ConcurrentSkipListMap<>();
  private final Set<N> nodeList = Sets.newConcurrentHashSet();

  public RendezvousHash(Funnel<N> nodeFunnel, Collection<? extends N> init) {
    this.hasher = Hashing.murmur3_128();
    this.nodeFunnel = nodeFunnel;

    nodeList.addAll(init);
  }

  public void add(N node) {
    nodeList.add(node);
  }

  public void remove(N node) {
    nodeList.remove(node);
  }

  public void refresh(List<N> list) {
    nodeList.clear();
    nodeList.addAll(list);
  }

  public N getOne(byte[] key) {
    hashMap.clear();

    nodeList.forEach(
        xs -> {
          hashMap.put(
              hasher.newHasher().putBytes(key).putObject(xs, nodeFunnel).hash().asLong(), xs);
        });

    return hashMap.remove(hashMap.keySet().stream().max(Long::compare).orElse(null));
  }

  public List<N> get(byte[] key, int listSize) {
    hashMap.clear();
    List<N> nodes = new ArrayList<>(listSize);

    nodeList.forEach(
        xs ->
            hashMap.put(
                hasher.newHasher().putBytes(key).putObject(xs, nodeFunnel).hash().asLong(), xs));

    TreeSet<Long> set = Sets.newTreeSet(hashMap.keySet());

    for (int i = 0; i < listSize; i++) {
      Long x = set.first();
      nodes.add(i, hashMap.remove(x));
      set.remove(x);
    }

    return nodes;
  }
}
