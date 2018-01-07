package com.nordstrom.xrpc;

import com.google.common.collect.ImmutableSet;
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
    List<N> _nodeList = new ArrayList<>();

    nodeList
        .parallelStream()
        .filter(xs -> !_nodeList.contains(xs))
        .forEach(
            xs -> {
              hashMap.put(
                  hasher.newHasher().putBytes(key).putObject(xs, nodeFunnel).hash().asLong(), xs);
            });

    for (int i = 0; i < listSize; i++) {
      _nodeList.add(hashMap.remove(hashMap.keySet().stream().max(Long::compare).orElse(null)));
    }

    hashMap.clear();

    return _nodeList;
  }

  public void refresh(List<N> list) {
    nodeList = new ImmutableSet.Builder<N>().addAll(list).build();
  }
}
