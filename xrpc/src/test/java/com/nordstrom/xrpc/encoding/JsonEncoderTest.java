package com.nordstrom.xrpc.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.JsonFormat;
import io.netty.buffer.Unpooled;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.nio.charset.Charset;
import lombok.Getter;
import org.junit.jupiter.api.Test;

class JsonEncoderTest {
  static class Person {
    @Getter private String name;

    @ConstructorProperties("name")
    public Person(String name) {
      this.name = name;
    }
  }

  private JsonEncoder jsonEncoder =
      new JsonEncoder(new ObjectMapper(), JsonFormat.printer().omittingInsignificantWhitespace());

  @Test
  void testEncodeJsonUtf16() throws IOException {
    assertEquals(
        Unpooled.copiedBuffer("{\"name\":\"foo\"}", Charset.forName("UTF-16")),
        jsonEncoder.encode(Unpooled.directBuffer(), "utf-16, foo-bar", new Person("foo")));
  }

  @Test
  void testEncodeJsonDefaultCharset() throws IOException {
    assertEquals(
        Unpooled.copiedBuffer("{\"name\":\"foo\"}", Charset.forName("UTF-8")),
        jsonEncoder.encode(Unpooled.directBuffer(), "baz-bar, foo-bar", new Person("foo")));
  }
}
