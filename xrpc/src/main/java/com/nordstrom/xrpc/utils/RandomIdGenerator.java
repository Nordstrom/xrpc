package com.nordstrom.xrpc.utils;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

public class RandomIdGenerator {
  public static String generate() {
    UUID uuid = UUID.randomUUID();
    ByteBuffer bytes = ByteBuffer.wrap(new byte[16]);
    bytes.putLong(uuid.getMostSignificantBits());
    bytes.putLong(uuid.getLeastSignificantBits());
    // unset most significant bit of first byte
    // to ensure id starts with [A-Za-f]
    byte[] byteArr = bytes.array();
    byteArr[0] = 0x7f;
    return Base64.getEncoder()
        .encodeToString(byteArr)
        .replace('+', '-') // Replace + with - (see RFC 4648, sec. 5)
        .replace('/', '_') // Replace / with _ (see RFC 4648, sec. 5)
        .substring(0, 22); // Drop '==' padding
  }
}
