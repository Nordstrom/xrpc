package com.nordstrom.xrpc.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nordstrom.xrpc.encoding.dino.proto.Dino;
import com.nordstrom.xrpc.server.Server;
import com.nordstrom.xrpc.testing.UnsafeHttp;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.beans.ConstructorProperties;
import java.io.IOException;
import lombok.Getter;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EncodingServerTest {
  static class Person {
    @Getter private String name;

    @ConstructorProperties("name")
    public Person(String name) {
      this.name = name;
    }
  }

  private OkHttpClient client;
  private Config config = ConfigFactory.load("test.conf").getConfig("xrpc");
  private Server server;
  private String endpoint;
  private Person person;
  private Dino dino;

  @BeforeEach
  void beforeEach() throws IOException {
    server = new Server(config);
    server.post(
        "/person",
        r -> {
          person = r.body(Person.class);
          return r.response().ok();
        });
    server.get("/person", r -> r.ok(person));

    server.post(
        "/dino",
        r -> {
          dino = r.body(Dino.class);
          return r.response().ok();
        });

    server.get("/dino", r -> r.ok(dino));
    server.listenAndServe();
    endpoint = server.localEndpoint();
    client = UnsafeHttp.unsafeClient();
  }

  @AfterEach
  void afterEach() {
    server.shutdown();
  }

  @Test
  void testJsonDecoding() throws IOException {
    Response response =
        client
            .newCall(
                new Request.Builder()
                    .url(endpoint + "/person")
                    .post(
                        RequestBody.create(
                            MediaType.parse("application/json"), "{\"name\":\"bob\"}"))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .build())
            .execute();

    assertEquals(200, response.code());
    assertEquals("bob", person.name);
  }

  @Test
  void testJsonProtoDecoding() throws IOException {
    Response response =
        client
            .newCall(
                new Request.Builder()
                    .url(endpoint + "/dino")
                    .post(
                        RequestBody.create(
                            MediaType.parse("application/json"), "{\"name\":\"bob\"}"))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .build())
            .execute();

    assertEquals(200, response.code());
    assertEquals("bob", dino.getName());
  }

  @Test
  void testProtoDecoding() throws IOException {
    Dino data = Dino.newBuilder().setName("bob").build();
    Response response =
        client
            .newCall(
                new Request.Builder()
                    .url(endpoint + "/dino")
                    .post(
                        RequestBody.create(
                            MediaType.parse("application/protobuf"), data.toByteArray()))
                    .header("Content-Type", "application/protobuf; proto=nordstrom.dino.Dino")
                    .build())
            .execute();

    assertEquals(200, response.code());
    assertEquals("bob", dino.getName());
  }

  @Test
  void testJsonEncoding() throws IOException {
    person = new Person("bob");
    Response response =
        client
            .newCall(
                new Request.Builder()
                    .url(endpoint + "/person")
                    .get()
                    .header("Accept", "application/json, text/plain")
                    .build())
            .execute();

    assertEquals(200, response.code());
    assertEquals("{\"name\":\"bob\"}", response.body().string());
  }

  @Test
  void testJsonEncodingUtf16() throws IOException {
    person = new Person("bob");
    Response response =
        client
            .newCall(
                new Request.Builder()
                    .url(endpoint + "/person")
                    .get()
                    .header("Accept", "application/json;q=1, text/plain")
                    .header("Accept-Charset", "utf-16")
                    .build())
            .execute();

    assertEquals(200, response.code());
    assertEquals(30, response.body().bytes().length); // double wide for utf 16
  }

  @Test
  void testJsonProtoEncoding() throws IOException {
    dino = Dino.newBuilder().setName("bob").build();
    Response response =
        client
            .newCall(
                new Request.Builder()
                    .url(endpoint + "/dino")
                    .get()
                    .header("Accept", "application/json, text/plain")
                    .build())
            .execute();

    assertEquals(200, response.code());
    assertEquals("{\"name\":\"bob\"}", response.body().string());
  }

  @Test
  void testProtoEncoding() throws IOException {
    dino = Dino.newBuilder().setName("bob").build();
    Response response =
        client
            .newCall(
                new Request.Builder()
                    .url(endpoint + "/dino")
                    .get()
                    .header("Accept", "application/protobuf, text/plain")
                    .build())
            .execute();

    assertEquals(200, response.code());
    assertEquals("bob", Dino.parseFrom(response.body().bytes()).getName());
  }
}
