package com.nordstrom.xrpc.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class XrpcClientTest {
  XrpcClient client = new XrpcClient();

  @BeforeEach
  void setUp() {}

  @AfterEach
  void tearDown() {}

  @Test
  void newCallExecute() {
    FullHttpRequest request =
        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/people");

    ListenableFuture<FullHttpResponse> response =
        client.newCall("https://127.0.0.1:8080").get(request).execute();

    Futures.addCallback(
        response,
        new FutureCallback<FullHttpResponse>() {
          @Override
          public void onSuccess(FullHttpResponse result) {
            System.out.println(result);
            assertEquals(HttpResponseStatus.OK, result.status());
          }

          @Override
          public void onFailure(Throwable t) {}
        });

    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
