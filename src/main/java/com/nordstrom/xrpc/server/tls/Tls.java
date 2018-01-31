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

import com.nordstrom.xrpc.XrpcConstants;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Tls {
  private static final String PASSWORD = "passwordsAreGood";
  // TODO(JR): This should only be called if a cert is not provided
  private static final X509Certificate selfSignedCert = createSelfSigned();
  private final String cert;
  private final String key;
  private SslContext sslCtx;

  public Tls() {
    this.cert = null;
    this.key = null;
  }

  public Tls(String cert, String key) {
    this.cert = cert;
    this.key = key;
    this.sslCtx = buildEncryptionHandler();
  }

  public static X509Certificate createSelfSigned() {
    try {
      return SelfSignedX509CertGenerator.generate("*.nordstrom.com");
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public ChannelHandler getEncryptionHandler(ByteBufAllocator alloc) {

    ChannelHandler handler = sslCtx.newHandler(alloc);

    String[] protocols = new String[] {"TLSv1.2"};
    ((SslHandler) handler).engine().setEnabledProtocols(protocols);

    return handler;
  }

  public SslContext buildEncryptionHandler() {
    try {

      final List<java.security.cert.X509Certificate> certList = new ArrayList<>();
      final String rawCertString = cert;
      PrivateKey privateKey;
      // PublicKey publicKey; //TODO(JR): Leave code in, we should really validate the signature
      // with the public key
      X509Certificate selfSignedCert = null;

      if (key != null) {
        X509CertificateGenerator.DerKeySpec derKeySpec =
            X509CertificateGenerator.parseDerKeySpec(key);
        privateKey = X509CertificateGenerator.buildPrivateKey(derKeySpec);
        // publicKey = X509CertificateGenerator.buildPublicKey(derKeySpec); //TODO(JR): Leave code
        // in, we should really validate the signature with the public key
      } else {
        selfSignedCert = SelfSignedX509CertGenerator.generate("*.nordstrom.com");
        privateKey = selfSignedCert.getKey();
      }

      java.security.cert.X509Certificate[] chain;

      if (cert != null) {

        String[] certs = rawCertString.split("-----END CERTIFICATE-----\n");

        for (String cert : certs) {
          CertificateFactory cf = CertificateFactory.getInstance("X.509");
          java.security.cert.X509Certificate x509Certificate =
              (java.security.cert.X509Certificate)
                  cf.generateCertificate(
                      new ByteArrayInputStream(
                          (cert + "-----END CERTIFICATE-----\n")
                              .getBytes(XrpcConstants.DEFAULT_CHARSET)));
          certList.add(x509Certificate);
        }

        chain = new java.security.cert.X509Certificate[certList.size()];

        for (int i = 0; i < certList.size(); i++) {
          chain[i] = certList.get(i);
        }
      } else {
        if (selfSignedCert == null) {
          selfSignedCert = SelfSignedX509CertGenerator.generate("*.nordstrom.com");
        }
        chain = new java.security.cert.X509Certificate[1];
        chain[0] = selfSignedCert.getCert();
      }

      SslContext sslCtx = null;

      if (OpenSsl.isAvailable()) {
        log.info("Using OpenSSL");
        sslCtx =
            SslContextBuilder.forServer(privateKey, chain)
                .sslProvider(SslProvider.OPENSSL)
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(
                    new ApplicationProtocolConfig(
                        Protocol.ALPN,
                        // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK
                        // providers.
                        SelectorFailureBehavior.NO_ADVERTISE,
                        // ACCEPT is currently the only mode supported by both OpenSsl and JDK
                        // providers.
                        SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1))
                .build();
      } else {
        log.info("Using JSSE");
        final KeyStore keyStore = KeyStore.getInstance("JKS", "SUN");
        keyStore.load(null, PASSWORD.toCharArray());
        keyStore.setKeyEntry(
            chain[0].getIssuerX500Principal().getName(), privateKey, PASSWORD.toCharArray(), chain);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");

        kmf.init(keyStore, PASSWORD.toCharArray());
        sslCtx =
            SslContextBuilder.forServer(kmf)
                .sslProvider(SslProvider.JDK)
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(
                    new ApplicationProtocolConfig(
                        Protocol.ALPN,
                        // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK
                        // providers.
                        SelectorFailureBehavior.NO_ADVERTISE,
                        // ACCEPT is currently the only mode supported by both OpenSsl and JDK
                        // providers.
                        SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1))
                .build();
      }

      return sslCtx;

    } catch (NoSuchAlgorithmException
        | KeyStoreException
        | UnrecoverableKeyException
        | CertificateException
        | NoSuchProviderException
        | IllegalArgumentException
        | IOException
        | SignatureException
        | InvalidKeyException e) {
      e.printStackTrace();
    }

    return null;
  }
}
