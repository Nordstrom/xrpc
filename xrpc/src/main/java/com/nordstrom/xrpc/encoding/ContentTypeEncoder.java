package com.nordstrom.xrpc.encoding;

import lombok.Value;
import lombok.experimental.Accessors;

/** Contains content type and encoder registered for that content type. */
@Value
@Accessors(fluent = true)
public class ContentTypeEncoder {
  String contentType;
  Encoder encoder;
}
