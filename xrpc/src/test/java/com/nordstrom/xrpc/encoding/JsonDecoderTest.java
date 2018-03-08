package com.nordstrom.xrpc.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.nio.charset.Charset;
import lombok.Getter;
import org.junit.jupiter.api.Test;

class JsonDecoderTest {
  static class Person {
    @Getter private String name;

    @ConstructorProperties("name")
    public Person(String name) {
      this.name = name;
    }
  }

  private JsonDecoder jsonDecoder =
      new JsonDecoder(new ObjectMapper(), new ProtoDefaultInstances());

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
