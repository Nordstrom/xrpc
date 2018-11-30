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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Funnels;
import io.netty.util.internal.PlatformDependent;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RendezvousHashTest {

  public static final int MAX_RANDOM_NUMBER = 123456789;

  @Test
  public void get_shouldReturnExpectedNumberOfHashesOnAllRuns() throws Exception {
    List<String> hostList = new ArrayList<>();
    Map<String, List<String>> hostToMatchingHashes = PlatformDependent.newConcurrentHashMap();
    int totalHosts = 100;
    for (int i = 0; i < totalHosts; i++) {
      hostList.add(("Host" + i));
      hostToMatchingHashes.put(("Host" + i), new ArrayList<>());
    }

    RendezvousHash<CharSequence> rendezvousHash =
        new RendezvousHash<>(Funnels.stringFunnel(Charset.defaultCharset()), hostList);

    int totalGetsToRun = 100000;
    int hashesToMatch = 3;
    Random random = new Random();
    for (int i = 0; i < totalGetsToRun; i++) {
      String randomNumberString = (Integer.toString(random.nextInt(MAX_RANDOM_NUMBER)));
      List<CharSequence> hosts = rendezvousHash.get(randomNumberString.getBytes(), hashesToMatch);
      hosts.forEach(host -> hostToMatchingHashes.get(host).add(randomNumberString));
    }

    Double averageMatchingHashesPerHost = hostToMatchingHashes.values().stream()
        .mapToInt(List::size)
        .average()
        .orElse(-1);

    int expectedAverage = hashesToMatch * totalGetsToRun / totalHosts;
    assertEquals(expectedAverage, averageMatchingHashesPerHost.intValue());
  }

  @Test
  void shouldNotNullPointerWhileUsedConcurrently() throws InterruptedException {
    RendezvousHash<CharSequence> rendezvousHash = new RendezvousHash<>(
        Funnels.stringFunnel(XrpcConstants.DEFAULT_CHARSET), Arrays.asList("hash1", "hash2")
    );

    List<Object> errors = Collections.synchronizedList(new ArrayList<>());

    ExecutorService executorService = Executors.newFixedThreadPool(10);

    IntStream.range(0, 100).forEach(i -> {
      executorService.submit(() -> {
        try {
          rendezvousHash.get("hash1".getBytes(), 2);
        } catch (NullPointerException e) {
          errors.add(e);
        }
      });

      executorService.submit(() -> {
        try {
          rendezvousHash.getOne("hash1".getBytes());
        } catch (NullPointerException e) {
          errors.add(e);
        }
      });
    });

    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.SECONDS);

    assertTrue(errors.isEmpty());
  }

  @Test
  void simpleGet() {
    Map<String, String> map =
        new ImmutableMap.Builder<String, String>()
            .put("a", "1")
            .put("b", "2")
            .put("c", "3")
            .put("d", "4")
            .put("e", "5")
            .build();
    RendezvousHash<CharSequence> hasher =
        new RendezvousHash<>(Funnels.stringFunnel(XrpcConstants.DEFAULT_CHARSET), map.keySet());
    String k1 = "foo";
    String k2 = "bar";
    String k3 = "baz";

    assertEquals(hasher.getOne(k1.getBytes()), hasher.getOne(k1.getBytes()));
    assertEquals(hasher.getOne(k2.getBytes()), hasher.getOne(k2.getBytes()));
    assertEquals(hasher.getOne(k3.getBytes()), hasher.getOne(k3.getBytes()));
    String k4 = "biz";
    assertEquals(hasher.getOne(k4.getBytes()), hasher.getOne(k4.getBytes()));

    System.out.println(hasher.getOne(k1.getBytes()));
    System.out.println(hasher.getOne(k2.getBytes()));
    System.out.println(hasher.getOne(k3.getBytes()));
    System.out.println(hasher.getOne(k4.getBytes()));

    System.out.println(hasher.getOne(k1.getBytes()));
    System.out.println(hasher.getOne(k2.getBytes()));
    System.out.println(hasher.getOne(k3.getBytes()));
    System.out.println(hasher.getOne(k4.getBytes()));

    assertNotEquals(hasher.getOne(k1.getBytes()), hasher.getOne(k4.getBytes()));
  }
}
