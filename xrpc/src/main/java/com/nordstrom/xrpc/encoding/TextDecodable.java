package com.nordstrom.xrpc.encoding;

/** Interface giving an object to encode itself into a text string. */
public interface TextDecodable {
  <T> T decode(String text);
}
