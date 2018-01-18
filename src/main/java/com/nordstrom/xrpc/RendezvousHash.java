package com.nordstrom.xrpc;

import com.google.common.collect.Sets;
import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RendezvousHash<N> {

  private final HashFunction hasher;
  private final Funnel<N> nodeFunnel;
  private final Map<Long, N> hashMap = new ConcurrentSkipListMap<>();
  private final Set<N> nodeList = Sets.newConcurrentHashSet();

  public RendezvousHash(Funnel<N> nodeFunnel, Collection<N> init) {
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
    List<N> _nodeList = new ArrayList<>(listSize);

    nodeList.forEach(
        xs -> {
          hashMap.put(
              hasher.newHasher().putBytes(key).putObject(xs, nodeFunnel).hash().asLong(), xs);
        });

    TreeSet<Long> set = Sets.newTreeSet(hashMap.keySet());

    for (int i = 0; i < listSize; i++) {
      Long x = set.first();
      _nodeList.add(i, hashMap.remove(x));
      set.remove(x);
    }

    return _nodeList;
  }
}
