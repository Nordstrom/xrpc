/*
 * Copyright 2018 Nordstrom, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nordstrom.xrpc.server.tls;

import com.xjeffrose.xio.SSL.SslContextFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Tls {
  private final TlsConfig tlsConfig;
  private final SslContext sslCtx;

  public Tls(TlsConfig tlsConfig) {
    this.tlsConfig = tlsConfig;
    this.sslCtx = buildEncryptionHandler();
  }

  public static X509Certificate createSelfSigned() {
    try {
      return SelfSignedX509CertGenerator.generate("*.nordstrom.com");
    } catch (Exception e) {
      log.error("Failed to generate self signed certificate", e);
      throw new RuntimeException(e);
    }
  }

  public ChannelHandler encryptionHandler(ByteBufAllocator alloc) {

    ChannelHandler handler = sslCtx.newHandler(alloc);

    String[] protocols = new String[] {"TLSv1.2"};
    ((SslHandler) handler).engine().setEnabledProtocols(protocols);

    return handler;
  }

  private SslContext buildEncryptionHandler() {
    return SslContextFactory.buildServerContext(tlsConfig);
  }
}
