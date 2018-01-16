package com.nordstrom.xrpc;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class RendezvousHash<N> {

  private final HashFunction hasher;
  private final Funnel<N> nodeFunnel;
  private final int listSize;
  private final Map<Long, N> hashMap = new ConcurrentSkipListMap<>();
  private final Set<N> nodeList = Sets.newConcurrentHashSet();

  public RendezvousHash(Funnel<N> nodeFunnel, Collection<N> init, int listSize) {
    this.hasher = Hashing.murmur3_128();
    this.nodeFunnel = nodeFunnel;
    this.listSize = listSize;

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

  public List<N> get(byte[] key) {
    hashMap.clear();
    List<N> _nodeList = new ArrayList<>(listSize);

    nodeList.forEach(
      xs -> {
        hashMap.put(
          hasher.newHasher().putBytes(key).putObject(xs, nodeFunnel).hash().asLong(), xs);
      });

    for (int i = 0; i < listSize; i++) {
      _nodeList.add(i, hashMap.remove(hashMap.keySet().stream().max(Long::compare).get()));
    }

    return _nodeList;
  }

}
