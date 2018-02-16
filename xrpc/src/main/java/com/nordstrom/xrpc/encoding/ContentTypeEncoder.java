package com.nordstrom.xrpc.encoding;

import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class ContentTypeEncoder {
  String contentType;
  Encoder encoder;
}
