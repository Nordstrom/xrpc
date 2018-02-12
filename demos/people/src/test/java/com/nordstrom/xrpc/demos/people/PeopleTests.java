package com.nordstrom.xrpc.demos.people;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nordstrom.xrpc.testing.UnsafeHttp;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PeopleTests {
  // don't use 8080 here as it may conflict with a running local app server
  private static final Config config = ConfigFactory.load("test.conf");
  private static final String endpoint = "https://127.0.0.1:" + config.getInt("xrpc.server.port");
  private final Application app = new Application(config.getConfig("xrpc"));
  private final OkHttpClient client = UnsafeHttp.unsafeClient();

  @BeforeEach
  void beforeEach() throws IOException {
    app.start();
  }

  @AfterEach
  void afterEach() {
    app.stop();
  }

  @Test
  void testPostPeople() throws IOException {
    Response response =
        client
            .newCall(
                new Request.Builder()
                    .post(RequestBody.create(MediaType.parse("application/json"), "bob"))
                    .url(endpoint + "/people")
                    .build())
            .execute();

    assertEquals(200, response.code());
  }

  @Test
  void testGetPeople() throws IOException {
    client
        .newCall(
            new Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/json"), "bob"))
                .url(endpoint + "/people")
                .build())
        .execute();

    Response response =
        client.newCall(new Request.Builder().get().url(endpoint + "/people").build()).execute();

    assertEquals("[{\"name\":\"bob\"}]", response.body().string());
  }

  @Test
  void testGetPerson() throws IOException {
    client
        .newCall(
            new Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/json"), "bob"))
                .url(endpoint + "/people")
                .build())
        .execute();

    Response response =
        client.newCall(new Request.Builder().get().url(endpoint + "/people/bob").build()).execute();

    assertEquals("{\"name\":\"bob\"}", response.body().string());
  }

  @Test
  void testGetPersonNotFound() throws IOException {
    Response response =
        client.newCall(new Request.Builder().get().url(endpoint + "/people/bob").build()).execute();

    assertEquals(404, response.code());
  }
}
