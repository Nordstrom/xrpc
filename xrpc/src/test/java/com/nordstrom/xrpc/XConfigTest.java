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

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.core.Is.is;
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
    assertThat(config.port(), is(8080));
    assertThat(config.adminRoutesEnableInfo(), is(true));
    assertThat(config.adminRoutesEnableUnsafe(), is(false));
    assertThat(config.maxPayloadBytes(), is(TEN_MEGABYTES));
    assertThat(config.readerIdleTimeout(), is(200 * SECONDS));
    assertThat(config.writerIdleTimeout(), is(400 * SECONDS));
    assertThat(config.allIdleTimeout(), is(0));
    assertThat(config.workerNameFormat(), is("xrpc-worker-%d"));
    assertThat(config.bossThreadCount(), is(4));
    assertThat(config.workerThreadCount(), is(40));
    assertThat(config.asyncHealthCheckThreadCount(), is(0));
    assertThat(config.maxConnections(), is(2000));
    assertThat(config.rateLimiterPoolSize(), is(24));
    assertThat(config.softReqPerSec(), is(500.0d));
    assertThat(config.hardReqPerSec(), is(550.0d));
    assertThat(config.globalSoftReqPerSec(), is(700.0d));
    assertThat(config.globalHardReqPerSec(), is(750.0d));
    assertThat(
        config.getClientRateLimitOverride(),
        is(ImmutableMap.of("127.0.0.1", Arrays.asList(500d, 550d))));
    assertThat(config.slf4jReporter(), is(false));
    assertThat(config.slf4jReporterPollingRate(), is(30 * SECONDS));
    assertThat(config.consoleReporter(), is(false));
    assertThat(config.consoleReporterPollingRate(), is(30 * SECONDS));
    assertThat(config.jmxReporter(), is(true));
    assertThat(config.ipBlackList(), is(NONE));
    assertThat(config.ipWhiteList(), is(NONE));
    assertThat(config.defaultContentType(), is("application/json"));
  }

  @Test
  void shouldSetDefaultCorsConfigValues() {
    assertThat(config.corsConfig().isCorsSupportEnabled(), is(false));
    assertThat(config.corsConfig().origins(), is(NONE));
    assertThat(config.corsConfig().allowedRequestHeaders(), is(NONE));
    assertThat(config.corsConfig().allowedRequestMethods(), is(NONE));
    assertThat(config.corsConfig().isCredentialsAllowed(), is(false));
    assertThat(config.corsConfig().isShortCircuit(), is(false));
  }

  @Test
  void shouldSetDefaultTlsConfigValues() {
    assertThat(config.tlsConfig().clientAuth(), is(ClientAuth.NONE));
    assertThat(config.tlsConfig().certificate(), matchesPattern(CERTIFICATE_REGEX));
    assertThat(config.tlsConfig().privateKey(), matchesPattern(PRIVATE_KEY_REGEX));
  }
}
