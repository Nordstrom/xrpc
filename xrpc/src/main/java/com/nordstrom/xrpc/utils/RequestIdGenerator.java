package com.nordstrom.xrpc.utils;

import com.google.common.io.BaseEncoding;
import java.nio.ByteBuffer;
import java.util.UUID;

public class RequestIdGenerator {
  private static final BaseEncoding encoder = BaseEncoding.base64Url().omitPadding();

  /** Generate a URL-safe 128-bit slug. */
  public static String generate() {
    UUID uuid = UUID.randomUUID();
    ByteBuffer buffer = ByteBuffer.allocate(16);
    buffer.putLong(uuid.getMostSignificantBits());
    buffer.putLong(uuid.getLeastSignificantBits());
    // Unset most significant bit of first byte
    // to ensure id starts with [A-Za-f].
    byte[] bytes = buffer.array();
    bytes[0] &= 0x7f;
    return encoder.encode(bytes);
  }
}
