package com.nordstrom.xrpc.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nordstrom.xrpc.encoding.dino.proto.Dino;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class ProtoEncoderTest {
  private ProtoEncoder encoder = new ProtoEncoder();
  private Dino dino = Dino.newBuilder().setName("test-name").setFavColor("test-color").build();

  @Test
  void testEncode() throws IOException {
    ByteBuf buf = encoder.encode(Unpooled.directBuffer(), "", dino, false);
    assertEquals(dino, Dino.parseFrom(bufBytes(buf)));
  }

  private byte[] bufBytes(ByteBuf buf) {
    byte[] bytes = new byte[buf.readableBytes()];
    buf.readBytes(bytes);
    return bytes;
  }
}
