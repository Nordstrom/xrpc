package com.nordstrom.xrpc.server.tls;

import io.netty.handler.ssl.ClientAuth;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TlsConfig {
  private final ClientAuth clientAuth;
  private final String certificate;
  private final String privateKey;
}
