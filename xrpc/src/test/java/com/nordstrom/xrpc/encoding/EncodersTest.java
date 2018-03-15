package com.nordstrom.xrpc.encoding;

import static org.junit.Assert.assertSame;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.JsonFormat;
import io.netty.buffer.ByteBuf;
import java.beans.ConstructorProperties;
import java.io.IOException;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EncodersTest {
  static class Person {
    @Getter private String name;

    @ConstructorProperties("name")
    public Person(String name) {
      this.name = name;
    }
  }

  private JsonEncoder jsonEncoder =
      new JsonEncoder(
          new ObjectMapper().writer(),
          new ObjectMapper().writerWithDefaultPrettyPrinter(),
          JsonFormat.printer().omittingInsignificantWhitespace(),
          JsonFormat.printer());
  private Encoder fooBarEncoder =
      new Encoder() {
        @Override
        public ByteBuf encode(
            ByteBuf buf, CharSequence acceptCharset, Object object, boolean pretty)
            throws IOException {
          return null;
        }

        @Override
        public CharSequence mediaType() {
          return "foo/bar";
        }
      };

  private Encoders encoders;

  @BeforeEach
  void beforeEach() {
    encoders =
        Encoders.builder()
            .defaultContentType("application/json")
            .encoder(jsonEncoder)
            .encoder(fooBarEncoder)
            .build();
  }

  @Test
  void testDefaultEncoder() {
    assertSame(jsonEncoder, encoders.acceptedEncoder(""));
  }

  @Test
  void testDefaultEncoderForNull() {
    assertSame(jsonEncoder, encoders.acceptedEncoder(null));
  }

  @Test
  void testEncoderWithoutParam() {
    assertSame(fooBarEncoder, encoders.acceptedEncoder("text/plain, foo/bar"));
  }

  @Test
  void testEncoderWithParam() {
    assertSame(fooBarEncoder, encoders.acceptedEncoder("foo/bar; q=6, text/plain; q=5"));
  }
}
