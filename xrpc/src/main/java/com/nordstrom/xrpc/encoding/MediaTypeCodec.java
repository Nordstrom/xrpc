package com.nordstrom.xrpc.encoding;

public interface MediaTypeCodec {
  /** Content type this encoder supports. */
  CharSequence mediaType();
}
