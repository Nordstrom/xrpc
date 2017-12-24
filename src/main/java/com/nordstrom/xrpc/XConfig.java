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
  private final double softRateLimit;
  private final double hardRateLimit;
  private final String cert;
  private final String key;
  private final int port;

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
    softRateLimit = config.getDouble("soft_req_per_sec");
    hardRateLimit = config.getDouble("hard_req_per_sec");
    cert = config.getString("cert");
    key = config.getString("key");
    port = config.getInt("server.port");
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

  public double softRateLimit() {
    return softRateLimit;
  }

  public double hardRateLimit() {
    return hardRateLimit;
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
}
