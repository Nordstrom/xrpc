package com.nordstrom.xrpc.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nordstrom.xrpc.server.Server;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.buffer.Unpooled;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.nio.charset.Charset;
import lombok.Getter;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

class JsonDecoderTest {
  static class Person {
    @Getter private String name;

    @ConstructorProperties("name")
    public Person(String name) {
      this.name = name;
    }
  }

  private JsonDecoder jsonDecoder = new JsonDecoder(new ObjectMapper());

  private OkHttpClient client;
  private Config config = ConfigFactory.load("test.conf").getConfig("xrpc");
  private Server server;
  private String endpoint;
  private Person person;

  @Test
  void testDecodeJsonUtf16() throws IOException {
    assertEquals(
        "bob",
        jsonDecoder.decode(
                Unpooled.copiedBuffer("{\"name\":\"bob\"}", Charset.forName("UTF-16")),
                "application/json; charset=utf-16",
                Person.class)
            .name);
  }
}
