package com.nordstrom.xrpc.encoding;

import static org.junit.Assert.assertSame;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DecodersTest {
  private JsonDecoder jsonDecoder =
      new JsonDecoder(new ObjectMapper(), new ProtoDefaultInstances());

  private Decoder fooBarDecoder =
      new Decoder() {
        @Override
        public <T> T decode(ByteBuf body, CharSequence contentType, Class<T> clazz) {
          return null;
        }

        @Override
        public CharSequence mediaType() {
          return "foo/bar";
        }
      };

  private Decoders decoders;

  @BeforeEach
  void beforeEach() {
    decoders =
        Decoders.builder()
            .defaultContentType("application/json")
            .decoder(jsonDecoder)
            .decoder(fooBarDecoder)
            .build();
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
  void testDecoderWithoutParam() {
    assertSame(fooBarDecoder, decoders.decoder("foo/bar"));
  }

  @Test
  void testDecoderWithParam() {
    assertSame(fooBarDecoder, decoders.decoder("foo/bar ; charset=utf-8"));
  }
}
