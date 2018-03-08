package com.nordstrom.xrpc.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nordstrom.xrpc.encoding.dino.proto.Dino;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ProtoDecoderTest {
  private ProtoDecoder decoder = new ProtoDecoder(new ProtoDefaultInstances());
  private Dino dino = Dino.newBuilder().setName("test-name").setFavColor("test-color").build();

  @Test
  void testDecode() throws IOException {
    Dino result = decoder.decode(Unpooled.wrappedBuffer(dino.toByteArray()), "", Dino.class);
    assertEquals(dino, result);
  }
}
