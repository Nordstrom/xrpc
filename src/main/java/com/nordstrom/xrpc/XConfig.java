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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.HashSet;
import java.util.List;

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
  private final List<String> ipBlackList;
  private final List<String> ipWhiteList;
  private boolean slf4jReporter;
  private boolean jmxReporter;
  private boolean consoleReporter;
  private int slf4jReporterPollingRate;
  private int consoleReporterPollingRate;

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

    ipBlackList = config.getStringList("ip_black_list");
    ipWhiteList = config.getStringList("ip_black_list");
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

  public HashSet<String> ipBlacklist() {
    return new HashSet<>(ipBlackList);
  }

  public HashSet<String> ipWhitelist() {
    return new HashSet<>(ipWhiteList);
  }
}
