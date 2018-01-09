package com.nordstrom.xrpc;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import io.netty.util.internal.PlatformDependent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RendezvousHash<N> {

  private final HashFunction hasher;
  private final Funnel<N> nodeFunnel;
  private final int listSize;
  private final Map<Long, N> hashMap = PlatformDependent.newConcurrentHashMap();

  private ImmutableSet<N> nodeList;
  private final List<N> _nodeList = new ArrayList<>();

  public RendezvousHash(Funnel<N> nodeFunnel, Collection<N> init, int listSize) {
    this.hasher = Hashing.murmur3_128();
    this.nodeFunnel = nodeFunnel;
    this.nodeList = new ImmutableSet.Builder<N>().addAll(init).build();
    this.listSize = listSize;
  }

  boolean remove(N node) {
    nodeList = Sets.difference(nodeList, ImmutableSet.of(node)).immutableCopy();
    return true;
  }

  boolean add(N node) {
    nodeList = new ImmutableSet.Builder<N>().addAll(nodeList).add(node).build();
    return true;
  }

  public List<N> get(byte[] key) {
    /* This is intentional as we are reusing the same data containers
     * but want to limit the number of new objects created
     */
    _nodeList.clear();
    hashMap.clear();

    nodeList.forEach(
        xs -> {
          hashMap.put(
              hasher.newHasher().putBytes(key).putObject(xs, nodeFunnel).hash().asLong(), xs);
        });

    for (int i = 0; i < listSize; i++) {
      _nodeList.add(i, hashMap.remove(hashMap.keySet().stream().max(Long::compare).orElse(null)));
    }

    return Lists.newArrayList(_nodeList);
  }

  public void refresh(List<N> list) {
    nodeList = new ImmutableSet.Builder<N>().addAll(list).build();
  }
}
