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

public class XConfig {
  private final Config config = ConfigFactory.load();

  public int readerIdleTimeout() {
    return config.getInt("reader_idle_timeout");
  }

  public int writerIdleTimeout() {
    return config.getInt("writer_idle_timeout");
  }

  public int requestIdleTimeout() {
    return config.getInt("request_idle_timeout");
  }

  public String workerNameFormat() {
    return config.getString("worker_name_format");
  }

  public int bossThreads() {
    return config.getInt("boss_threads");
  }

  public int workerThreads() {
    return config.getInt("worker_threads");
  }

  public int maxConnections() {
    return config.getInt("max_connections");
  }

  public float rateLimit() {
    return (float) config.getInt("req_per_sec");
  }

  public String cert() {
    return config.getString("cert");
  }

  public String key() {
    return config.getString("key");
  }

  public int port() {
    return config.getInt("server.port");
  }
}
