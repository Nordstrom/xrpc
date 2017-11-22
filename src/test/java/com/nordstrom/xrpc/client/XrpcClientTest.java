package com.nordstrom.xrpc.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.handler.codec.http.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XrpcClientTest {
  XrpcClient client = new XrpcClient();

  @BeforeEach
  void setUp() {
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void testURIString() throws Exception {
    String uriString = "https://localhost:8888/v1/authinit?client_id=ios&scope=REGISTERED&code=-S1l05-YI9a3yfaw5CcbxKedtiyPXkSwBBgCMzw14VQ*&method=s256&redirect_uri=nothing";
    String qs = XUrl.stripUrlParameters(uriString);
    System.out.println(XUrl.stripUrlParameters(uriString));
  }

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
        public void onFailure(Throwable t) {
        }
      });

    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
