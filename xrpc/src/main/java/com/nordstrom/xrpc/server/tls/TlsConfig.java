package com.nordstrom.xrpc.server.tls;

import io.netty.handler.ssl.ClientAuth;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class TlsConfig {
  private final ClientAuth clientAuth;
  private final String certificate;
  private final String privateKey;
}
