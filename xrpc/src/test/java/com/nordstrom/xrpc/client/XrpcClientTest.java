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

package com.nordstrom.xrpc.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class XrpcClientTest {
  XrpcClient client = new XrpcClient();
  CountDownLatch latch = new CountDownLatch(1);

  @BeforeEach
  void setUp() {}

  @AfterEach
  void tearDown() {}

  @Test
  void testUriString() throws Exception {
    String uriString =
        "https://localhost:8888/v1/authinit?client_id=ios&scope=REGISTERED&code=-S1l05-YI9a3yfaw5CcbxKedtiyPXkSwBBgCMzw14VQ*&method=s256&redirect_uri=nothing";
    String qs = XUrl.stripUrlParameters(uriString);
    System.out.println(XUrl.stripUrlParameters(uriString));
  }

  // This test is failing in Master.
  // TODO: Refactor this test to fail as a result of the callback.
  @Test
  @Disabled
  void newCallExecute() {
    FullHttpRequest request =
        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/people");

    ListenableFuture<FullHttpResponse> response = null;
    try {
      response = client.newCall("https://127.0.0.1:8080").get(request).execute();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }

    Futures.addCallback(
        response,
        new FutureCallback<FullHttpResponse>() {
          @Override
          public void onSuccess(FullHttpResponse result) {
            latch.countDown();
            ;
            System.out.println(result);
            assertEquals(HttpResponseStatus.OK, result.status());
          }

          @Override
          public void onFailure(Throwable t) {
            latch.countDown();
            assertEquals(true, false);
          }
        },
        MoreExecutors.directExecutor());

    try {
      latch.await(500, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
