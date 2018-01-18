/*
 * Copyright 2017 Nordstrom, Inc.
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

import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import io.netty.util.internal.PlatformDependent;
import java.util.*;

/**
 * A configuration object for the xrpc framework. This can be left with defaults, or provided with a
 * configuration object for overrides.
 *
 * <p>See <a
 * href="https://github.com/Nordstrom/xrpc/blob/master/src/main/resources/com/nordstrom/xrpc/xrpc.conf">the
 * embedded config</a> for defaults and documentation.
 */
public class XConfig {
  private final int readerIdleTimeout;
  private final int writerIdleTimeout;
  private final int allIdleTimeout;
  private final String workerNameFormat;
  private final int bossThreadCount;
  private final int workerThreadCount;
  private final int maxConnections;
  private final double softReqPerSec;
  private final double hardReqPerSec;
  private final String cert;
  private final String key;
  private final int port;
  private final double gloablSoftReqPerSec;
  private final double globalHardReqPerSec;
  private final ImmutableSet<String> ipBlackList;
  private final ImmutableSet<String> ipWhiteList;
  private final boolean slf4jReporter;
  private final boolean jmxReporter;
  private final boolean consoleReporter;
  private final int slf4jReporterPollingRate;
  private final int consoleReporterPollingRate;

  private final Map<String, List<Double>> clientRateLimitOverride =
      PlatformDependent.newConcurrentHashMap();
  private final boolean enableWhiteList;
  private final boolean enableBlackList;
  private final int rateLimiterPoolSize;

  /**
   * Construct a config object using the default configuration values <a
   * href="https://github.com/Nordstrom/xrpc/blob/master/src/main/resources/com/nordstrom/xrpc/xrpc.conf">here</a>.
   */
  public XConfig() {
    this(ConfigFactory.empty());
  }

  /**
   * Construct a config object using the provided configuration, falling back on the default
   * configuration values <a
   * href="https://github.com/Nordstrom/xrpc/blob/master/src/main/resources/com/nordstrom/xrpc/xrpc.conf">here</a>.
   */
  public XConfig(Config configOverrides) {
    Config defaultConfig = ConfigFactory.parseResources(this.getClass(), "xrpc.conf");
    Config config = configOverrides.withFallback(defaultConfig);

    readerIdleTimeout = config.getInt("reader_idle_timeout_seconds");
    writerIdleTimeout = config.getInt("writer_idle_timeout_seconds");
    allIdleTimeout = config.getInt("all_idle_timeout_seconds");
    workerNameFormat = config.getString("worker_name_format");
    bossThreadCount = config.getInt("boss_thread_count");
    workerThreadCount = config.getInt("worker_thread_count");
    maxConnections = config.getInt("max_connections");
    rateLimiterPoolSize = config.getInt("rate_limiter_pool_size");
    softReqPerSec = config.getDouble("soft_req_per_sec");
    hardReqPerSec = config.getDouble("hard_req_per_sec");
    gloablSoftReqPerSec = config.getDouble("global_soft_req_per_sec");
    globalHardReqPerSec = config.getDouble("global_hard_req_per_sec");
    cert = config.getString("cert");
    key = config.getString("key");
    port = config.getInt("server.port");
    slf4jReporter = config.getBoolean("slf4j_reporter");
    jmxReporter = config.getBoolean("jmx_reporter");
    consoleReporter = config.getBoolean("console_reporter");
    slf4jReporterPollingRate = config.getInt("slf4j_reporter_polling_rate");
    consoleReporterPollingRate = config.getInt("console_reporter_polling_rate");

    enableWhiteList = config.getBoolean("enable_white_list");
    enableBlackList = config.getBoolean("enable_black_list");

    ipBlackList =
        ImmutableSet.<String>builder().addAll(config.getStringList("ip_black_list")).build();
    ipWhiteList =
        ImmutableSet.<String>builder().addAll(config.getStringList("ip_white_list")).build();

    populateClientOverrideList(config.getObjectList("req_per_second_override"));
  }

  private void populateClientOverrideList(List<? extends ConfigObject> req_per_second_override) {
    req_per_second_override.forEach(
        xs -> {
          xs.forEach(
              (key, value) -> {
                List<String> valString = Arrays.asList(value.unwrapped().toString().split(":"));
                List<Double> val = new ArrayList();
                valString.forEach(v -> val.add(Double.parseDouble(v)));

                clientRateLimitOverride.put(key, val);
              });
        });
  }

  public Map<String, List<Double>> getClientRateLimitOverride() {
    return clientRateLimitOverride;
  }

  public int readerIdleTimeout() {
    return readerIdleTimeout;
  }

  public int writerIdleTimeout() {
    return writerIdleTimeout;
  }

  public int allIdleTimeout() {
    return allIdleTimeout;
  }

  public String workerNameFormat() {
    return workerNameFormat;
  }

  public int bossThreadCount() {
    return bossThreadCount;
  }

  public int workerThreadCount() {
    return workerThreadCount;
  }

  public int maxConnections() {
    return maxConnections;
  }

  public double softReqPerSec() {
    return softReqPerSec;
  }

  public double hardReqPerSec() {
    return hardReqPerSec;
  }

  public String cert() {
    return cert;
  }

  public String key() {
    return key;
  }

  public int port() {
    return port;
  }

  public boolean slf4jReporter() {
    return slf4jReporter;
  }

  public boolean jmxReporter() {
    return jmxReporter;
  }

  public boolean consoleReporter() {
    return consoleReporter;
  }

  public long slf4jReporterPollingRate() {
    return (long) slf4jReporterPollingRate;
  }

  public long consoleReporterPollingRate() {
    return (long) consoleReporterPollingRate;
  }

  public double globalHardReqPerSec() {
    return globalHardReqPerSec;
  }

  public double globalSoftReqPerSec() {
    return gloablSoftReqPerSec;
  }

  public ImmutableSet<String> ipBlackList() {
    return ipBlackList;
  }

  public ImmutableSet<String> ipWhiteList() {
    return ipWhiteList;
  }

  public boolean enableWhiteList() {
    return enableWhiteList;
  }

  public boolean enableBlackList() {
    return enableBlackList;
  }

  public int getRateLimiterPoolSize() {
    return rateLimiterPoolSize;
  }
}
