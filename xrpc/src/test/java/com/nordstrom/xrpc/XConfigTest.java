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

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.handler.ssl.ClientAuth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class XConfigTest {

  private static final String CERTIFICATE_REGEX =
      "[\n]-----BEGIN CERTIFICATE-----[\\s\\S]*-----END CERTIFICATE-----[\n]";
  private static final String PRIVATE_KEY_REGEX =
      "[\n]-----BEGIN RSA PRIVATE KEY-----[\\s\\S]*-----END RSA PRIVATE KEY-----[\n]";
  private static final ImmutableSet<Object> NONE = ImmutableSet.of();
  private static final int TEN_MEGABYTES = 10485760;
  private static final int SECONDS = 1;
  private XConfig config = new XConfig();

  @Test
  void getClientRateLimitOverride() {
    Config rawConfig = ConfigFactory.load("test.conf");
    XConfig config = new XConfig(rawConfig.getConfig("xrpc"));

    Map<String, List<Double>> configTest = config.getClientRateLimitOverride();

    double expected1 = Double.parseDouble("550");
    double testVal1 = configTest.get("localhost").get(0);
    assertEquals(expected1, testVal1);

    double expected2 = Double.parseDouble("1");
    double testVal2 = configTest.get("1.2.3.4").get(0);

    double expected3 = Double.parseDouble("2");
    double testVal3 = configTest.get("1.2.3.4").get(1);

    assertEquals(expected2, testVal2);
    assertEquals(expected3, testVal3);
  }

  @Test
  void shouldSetDefaultXrpcConfigValues() {
    assertEquals(TEN_MEGABYTES, config.maxPayloadBytes());
    assertEquals(200 * SECONDS, config.readerIdleTimeout());
    assertEquals(400 * SECONDS, config.writerIdleTimeout());
    assertEquals(0, config.allIdleTimeout());
    assertEquals("xrpc-worker-%d", config.workerNameFormat());
    assertEquals(4, config.bossThreadCount());
    assertEquals(40, config.workerThreadCount());
    assertEquals(0, config.asyncHealthCheckThreadCount());
    assertEquals(2000, config.maxConnections());
    assertEquals(24, config.rateLimiterPoolSize());
    assertEquals(500.0d, config.softReqPerSec());
    assertEquals(550.0d, config.hardReqPerSec());
    assertEquals(700.0d, config.globalSoftReqPerSec());
    assertEquals(750.0d, config.globalHardReqPerSec());
    assertEquals(
        ImmutableMap.of("127.0.0.1", Arrays.asList(500d, 550d)),
        config.getClientRateLimitOverride());
    assertEquals(30 * SECONDS, config.slf4jReporterPollingRate());
    assertEquals(30 * SECONDS, config.consoleReporterPollingRate());
    assertEquals(NONE, config.ipBlackList());
    assertEquals(NONE, config.ipWhiteList());
    assertEquals("application/json", config.defaultContentType());
    assertThat(config.port(), is(8080));
    assertTrue(config.adminRoutesEnableInfo());
    assertTrue(config.jmxReporter());
    assertFalse(config.slf4jReporter());
    assertFalse(config.adminRoutesEnableUnsafe());
    assertFalse(config.consoleReporter());
  }

  @Test
  void shouldSetDefaultCorsConfigValues() {
    assertEquals(NONE, config.corsConfig().origins());
    assertEquals(NONE, config.corsConfig().allowedRequestHeaders());
    assertEquals(NONE, config.corsConfig().allowedRequestMethods());
    assertFalse(config.corsConfig().isCorsSupportEnabled());
    assertFalse(config.corsConfig().isCredentialsAllowed());
    assertFalse(config.corsConfig().isShortCircuit());
  }

  @Test
  void shouldSetDefaultTlsConfigValues() {
    assertEquals(ClientAuth.NONE, config.tlsConfig().clientAuth());
    assertThat(config.tlsConfig().certificate(), matchesPattern(CERTIFICATE_REGEX));
    assertThat(config.tlsConfig().privateKey(), matchesPattern(PRIVATE_KEY_REGEX));
  }
}
