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

package com.nordstrom.xrpc.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.test.OkHttpUnsafe;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
class MetricsTest {
  private OkHttpClient h11client;
  private OkHttpClient h2client;
  private Config config;
  private Server server;
  private String endpoint;

  @BeforeEach
  void beforeEach() throws Exception {
    config = ConfigFactory.load("test.conf");
    server = new Server(config.getConfig("xrpc"));
    server.listenAndServe();
    endpoint = server.localEndpoint();
    h11client = OkHttpUnsafe.getUnsafeClient();
    h2client = OkHttpUnsafe.getUnsafeClient(Protocol.HTTP_2, Protocol.HTTP_1_1);
  }

  @AfterEach
  void afterEach() {
    server.shutdown();
  }

  @Test
  void testHttp2Metrics() throws IOException {
    Request request =
        new Request.Builder().url(endpoint + "/metrics").get().header("Accept", "*/*").build();
    Response response = h2client.newCall(request).execute();
    assertEquals(200, response.code());
    assertThat(response.body().string(), containsString("responseCodes.ok"));
  }

  @Test
  void testHttp11Metrics() throws IOException {
    Request request = new Request.Builder().url(endpoint + "/metrics").get().build();
    Response response = h11client.newCall(request).execute();
    assertEquals(200, response.code());
    assertThat(response.body().string(), containsString("responseCodes.ok"));
  }
}
