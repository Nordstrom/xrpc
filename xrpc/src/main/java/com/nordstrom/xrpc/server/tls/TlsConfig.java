package com.nordstrom.xrpc.server.tls;

import io.netty.handler.ssl.ClientAuth;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;

@Getter
@Value
@Accessors(fluent = true)
public class TlsConfig {
  private final ClientAuth clientAuth;
  private final String certificate;
  private final String privateKey;
}
