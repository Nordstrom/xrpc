package com.nordstrom.xrpc.encoding;

import com.google.common.collect.ImmutableSortedMap;
import lombok.Value;

@Value
public class ResponseEncoders {
  private final ImmutableSortedMap<String, ResponseEncoder> map;
}
