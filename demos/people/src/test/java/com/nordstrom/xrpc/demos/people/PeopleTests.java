package com.nordstrom.xrpc.demos.people;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nordstrom.xrpc.testing.Http;
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
  private static final String endpoint = "https:127.0.0.1:8014";
  private final Application app = new Application(ConfigFactory.load("test.conf"));
  private final OkHttpClient client = Http.unsafeClient();

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
  void testGetPeople() {}

  @Test
  void testGetPerson() {}
}
