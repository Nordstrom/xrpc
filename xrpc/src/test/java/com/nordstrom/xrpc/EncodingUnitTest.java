package com.nordstrom.xrpc;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nordstrom.xrpc.encoding.Decoder;
import com.nordstrom.xrpc.encoding.Decoders;
import com.nordstrom.xrpc.encoding.Encoders;
import com.nordstrom.xrpc.encoding.JsonDecoder;
import com.nordstrom.xrpc.encoding.JsonEncoder;
import com.nordstrom.xrpc.encoding.TextEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.nio.charset.Charset;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EncodingUnitTest {
  static class Person {
    @Getter private String name;

    @ConstructorProperties("name")
    public Person(String name) {
      this.name = name;
    }
  }

  private Person person;
  private JsonEncoder jsonEncoder = new JsonEncoder(new ObjectMapper());
  private TextEncoder textEncoder = new TextEncoder();
  private JsonDecoder jsonDecoder = new JsonDecoder(new ObjectMapper());
  private Decoder fooBarDecoder =
      new Decoder() {
        @Override
        public <T> T decode(ByteBuf body, CharSequence contentType, Class<T> clazz)
            throws IOException {
          return null;
        }

        @Override
        public CharSequence mediaType() {
          return "foo/bar";
        }
      };

  private Encoders encoders;
  private Decoders decoders;

  @BeforeEach
  void beforeEach() {
    encoders =
        Encoders.builder()
            .defaultContentType("application/json")
            .encoder(jsonEncoder)
            .encoder(textEncoder)
            .build();

    decoders =
        Decoders.builder()
            .defaultContentType("application/json")
            .decoder(jsonDecoder)
            .decoder(fooBarDecoder)
            .build();
  }

  @Test
  void testDefaultEncoder() {
    assertSame(jsonEncoder, encoders.acceptedEncoder(""));
  }

  @Test
  void testDefaultEncoderForNul() {
    assertSame(jsonEncoder, encoders.acceptedEncoder(null));
  }

  @Test
  void testDefaultDecoder() {
    assertSame(jsonDecoder, decoders.decoder(""));
  }

  @Test
  void testDefaultDecoderForNul() {
    assertSame(jsonDecoder, decoders.decoder(null));
  }

  @Test
  void testEncoderWithoutParam() {
    assertSame(textEncoder, encoders.acceptedEncoder("text/plain, foo/bar"));
  }

  @Test
  void testEncoderWithParam() {
    assertSame(textEncoder, encoders.acceptedEncoder("foo/bar , text/plain; q=5"));
  }

  @Test
  void testDecoderWithoutParam() {
    assertSame(fooBarDecoder, decoders.decoder("foo/bar"));
  }

  @Test
  void testDecoderWithParam() {
    assertSame(fooBarDecoder, decoders.decoder("foo/bar ; charset=utf-8"));
  }

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

  @Test
  void testEncodeTextUtf16() throws IOException {
    assertEquals(
        Unpooled.copiedBuffer("foobar", Charset.forName("UTF-16")),
        textEncoder.encode(Unpooled.directBuffer(), "utf-16, foo-bar", "foobar"));
  }

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
