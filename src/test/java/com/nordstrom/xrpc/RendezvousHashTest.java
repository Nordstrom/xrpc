package com.nordstrom.xrpc;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Funnels;
import io.netty.util.internal.PlatformDependent;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RendezvousHashTest {
  @Test
  public void get() throws Exception {
    List<String> nodeList = new ArrayList<>();
    Map<String, List<String>> mm = PlatformDependent.newConcurrentHashMap();
    for (int i = 0; i < 100; i++) {
      nodeList.add(("Host" + i));
      mm.put(("Host" + i), new ArrayList<>());
    }

    RendezvousHash rendezvousHash =
        new RendezvousHash(Funnels.stringFunnel(Charset.defaultCharset()), nodeList, 3);
    Random r = new Random();
    for (int i = 0; i < 100000; i++) {
      String thing = (Integer.toString(r.nextInt(123456789)));
      List<String> hosts = rendezvousHash.get(thing.getBytes());
      hosts.forEach(
          xs -> {
            mm.get(xs).add(thing);
          });
    }

    List<Integer> xx = new ArrayList<>();
    mm.keySet()
        .forEach(
            xs -> {
              xx.add(mm.get(xs).size());
            });

    Double xd = xx.stream().mapToInt(x -> x).average().orElse(-1);
    assertEquals(3000, xd.intValue());
  }

  @Test void simpleGet() {
    Map<String, String> map = new ImmutableMap.Builder<String, String>().put("a", "1").put("b", "2").put("c", "3").build();
    RendezvousHash hasher = new RendezvousHash(Funnels.stringFunnel(XrpcConstants.DEFAULT_CHARSET), map.keySet(), 1);
    String k1 = "foo";
    String k2 = "bar";
    String k3 = "baz";
    String k4 = "biz";

    assertEquals(hasher.get(k1.getBytes()), hasher.get(k1.getBytes()));
    assertEquals(hasher.get(k2.getBytes()), hasher.get(k2.getBytes()));
    assertEquals(hasher.get(k3.getBytes()), hasher.get(k3.getBytes()));
    assertEquals(hasher.get(k4.getBytes()), hasher.get(k4.getBytes()));

    assertNotEquals(hasher.get(k1.getBytes()), hasher.get(k4.getBytes()));



  }
}
