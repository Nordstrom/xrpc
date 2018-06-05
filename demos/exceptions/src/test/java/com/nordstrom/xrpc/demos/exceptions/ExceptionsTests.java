package com.nordstrom.xrpc.demos.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nordstrom.xrpc.server.Server;
import com.xjeffrose.xio.test.OkHttpUnsafe;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExceptionsRoutesTests {
  private Server server;
  private OkHttpClient client;

  @BeforeEach
  void beforeEach() throws Exception {
    server = new Server(0);
    Application.configure(server);
    server.listenAndServe();
    client = OkHttpUnsafe.getUnsafeClient();
  }

  @AfterEach
  void afterEach() {
    server.shutdown();
  }

  @Test
  void testGetPersonNotFound() throws IOException {
    Response response =
        client
            .newCall(
                new Request.Builder().get().url(server.localEndpoint() + "/people/bob").build())
            .execute();

    assertEquals(404, response.code());
  }

  @Test
  void testBadRequestException() throws IOException {
    Response response =
        client
            .newCall(
                new Request.Builder()
                    .get()
                    .url(server.localEndpoint() + "/show-badrequest")
                    .build())
            .execute();

    assertEquals(400, response.code());
    assertEquals("", response.body().string());
  }

  @Test
  void testBadRequestExceptionWithBody() throws IOException {
    Response response =
        client
            .newCall(
                new Request.Builder()
                    .get()
                    .url(server.localEndpoint() + "/show-badrequest-withbody")
                    .build())
            .execute();

    assertEquals(400, response.code());
    assertEquals(
        "{\"businessReason\":\"Some bad request\",\"businessStatusCode\":\"4.2.3000\"}",
        response.body().string());
  }

  @Test
  void testCustomException() throws IOException {
    Response response =
        client
            .newCall(
                new Request.Builder()
                    .get()
                    .url(server.localEndpoint() + "/show-customexception")
                    .build())
            .execute();

    assertEquals(400, response.code());
    assertEquals(
        "{\"businessReason\":\"Some business reason\",\"businessStatusCode\":\"4.2.3000\"}",
        response.body().string());
  }
}
