package com.nordstrom.xrpc.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nordstrom.xrpc.exceptions.proto.Error;
import com.nordstrom.xrpc.server.Server;
import com.nordstrom.xrpc.testing.UnsafeHttp;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExceptionTest {
  private OkHttpClient client;
  private Config config;
  private Server server;
  private String endpoint;

  @BeforeEach
  void beforeEach() {
    config = ConfigFactory.load("test.conf").getConfig("xrpc");
    client = UnsafeHttp.unsafeHttp11Client();
  }

  @AfterEach
  void afterEach() {
    server.shutdown();
  }

  @Test
  void testBadRequestJson() throws IOException {
    init();
    server.get(
        "/bad-request",
        r -> {
          throw new BadRequestException("bad request message");
        });

    start();
    Response response =
        client
            .newCall(new Request.Builder().get().url(endpoint + "/bad-request").build())
            .execute();
    assertEquals(400, response.code());
    assertEquals("application/json", response.header("Content-Type"));
    assertEquals(
        "{\"errorCode\":\"BadRequest\",\"message\":\"bad request message\"}",
        response.body().string());
  }

  @Test
  void testNotFoundJson() throws IOException {
    init();
    server.get(
        "/not-found",
        r -> {
          throw new NotFoundException("not found message");
        });
    start();
    Response response =
        client.newCall(new Request.Builder().get().url(endpoint + "/not-found").build()).execute();
    assertEquals(404, response.code());
    assertEquals("application/json", response.header("Content-Type"));
    assertEquals(
        "{\"errorCode\":\"NotFound\",\"message\":\"not found message\"}", response.body().string());
  }

  @Test
  void testUnauthorizedJson() throws IOException {
    init();
    server.get(
        "/unauthorized",
        r -> {
          throw new UnauthorizedException("unauthorized message");
        });
    start();
    Response response =
        client
            .newCall(new Request.Builder().get().url(endpoint + "/unauthorized").build())
            .execute();
    assertEquals(401, response.code());
    assertEquals("application/json", response.header("Content-Type"));
    assertEquals(
        "{\"errorCode\":\"Unauthorized\",\"message\":\"unauthorized message\"}",
        response.body().string());
  }

  @Test
  void testForbiddenJson() throws IOException {
    init();
    server.get(
        "/forbidden",
        r -> {
          throw new ForbiddenException("forbidden message");
        });
    start();
    Response response =
        client.newCall(new Request.Builder().get().url(endpoint + "/forbidden").build()).execute();
    assertEquals(403, response.code());
    assertEquals("application/json", response.header("Content-Type"));
    assertEquals(
        "{\"errorCode\":\"Forbidden\",\"message\":\"forbidden message\"}",
        response.body().string());
  }

  @Test
  void testBadRequestProtoFromAccept() throws IOException {
    init();
    server.get(
        "/bad-request",
        r -> {
          throw new BadRequestException("bad request message");
        });
    start();
    Response response =
        client
            .newCall(
                new Request.Builder()
                    .get()
                    .url(endpoint + "/bad-request")
                    .header("Accept", "application/protobuf,  some/other")
                    .build())
            .execute();
    assertEquals(400, response.code());
    assertEquals("application/protobuf", response.header("Content-Type"));
    assertEquals(
        Error.newBuilder().setErrorCode("BadRequest").setMessage("bad request message").build(),
        Error.parseFrom(response.body().bytes()));
  }

  @Test
  void testUnhandled() throws IOException {
    init();
    server.get(
        "/unhandled",
        r -> {
          throw new RuntimeException("unhandled message");
        });
    start();
    Response response =
        client.newCall(new Request.Builder().get().url(endpoint + "/unhandled").build()).execute();
    assertEquals(500, response.code());
    assertEquals(
        "{\"errorCode\":\"InternalServerError\",\"message\":\"Internal Server Error\"}",
        response.body().string());
  }

  private void addConfigValue(String path, ConfigValue value) {
    config = config.withValue(path, value);
  }

  private void init() {
    server = new Server(config, 0);
  }

  private void start() throws IOException {
    server.listenAndServe();
    endpoint = server.localEndpoint();
  }
}
