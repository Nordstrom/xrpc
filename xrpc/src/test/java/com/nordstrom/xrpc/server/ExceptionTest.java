package com.nordstrom.xrpc.server;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.exceptions.BadRequestException;
import com.nordstrom.xrpc.exceptions.ForbiddenException;
import com.nordstrom.xrpc.exceptions.NotFoundException;
import com.nordstrom.xrpc.exceptions.UnauthorizedException;
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
  private Routes routes;
  private String endpoint;

  @BeforeEach
  void beforeEach() {
    config = ConfigFactory.load("test.conf").getConfig("xrpc");
    client = UnsafeHttp.unsafeClient();
    routes = new Routes();
    routes
        .get(
            "/bad-request",
            r -> {
              throw new BadRequestException("bad request message", "bad request detailed message");
            })
        .get(
            "/unauthorized",
            r -> {
              throw new UnauthorizedException(
                  "unauthorized message", "unauthorized detailed message");
            })
        .get(
            "/forbidden",
            r -> {
              throw new ForbiddenException("forbidden message", "forbidden detailed message");
            })
        .get(
            "/not-found",
            r -> {
              throw new NotFoundException("not found message", "not found detailed message");
            })
        .get(
            "/unhandled",
            r -> {
              throw new RuntimeException("unhandled message");
            });
  }

  @AfterEach
  void afterEach() {
    server.shutdown();
  }

  @Test
  void testBadRequestJson() throws IOException {
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
  void testBadRequestText() throws IOException {
    addConfigValue("default_content_type", fromAnyRef("text/plain"));
    start();
    Response response =
        client
            .newCall(new Request.Builder().get().url(endpoint + "/bad-request").build())
            .execute();
    assertEquals(400, response.code());
    assertEquals("text/plain", response.header("Content-Type"));
    assertEquals("[BadRequest] bad request message", response.body().string());
  }

  @Test
  void testBadRequestTextFromAccept() throws IOException {
    start();
    Response response =
        client
            .newCall(
                new Request.Builder()
                    .get()
                    .url(endpoint + "/bad-request")
                    .header("Accept", "text/plain,  some/other")
                    .build())
            .execute();
    assertEquals(400, response.code());
    assertEquals("text/plain", response.header("Content-Type"));
    assertEquals("[BadRequest] bad request message", response.body().string());
  }

  @Test
  void testUnhandled() throws IOException {
    start();
    Response response =
        client.newCall(new Request.Builder().get().url(endpoint + "/unhandled").build()).execute();
    assertEquals(500, response.code());
    assertEquals("Internal Server Error", response.body().string());
  }

  private void addConfigValue(String path, ConfigValue value) {
    config = config.withValue(path, value);
  }

  private void start() throws IOException {
    server = new Server(new XConfig(config), routes);
    server.listenAndServe();
    endpoint = "https://127.0.0.1:" + server.port();
  }
}
