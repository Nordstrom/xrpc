/*
 * Copyright 2017 Nordstrom, Inc.
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

package com.xjeffrose.xrpc.tls;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TLS  {
  private static final String PASSWORD = "passwordsAreGood";

  private final String cert;
  private final String key;
  //TODO(JR): This should only be called if a cert is not provided
  private final static X509Certificate selfSignedCert = createSelfSigned();

  private SslContext sslCtx;

  public static X509Certificate createSelfSigned(){
    try{
      return SelfSignedX509CertGenerator.generate("*.xjeffrose.com");
    }catch (Exception e){
      e.printStackTrace();
    }
    return null;
  }

  public TLS() {
    this.cert = null;
    this.key = null;
  }

  public TLS(String cert, String key) {
    this.cert = cert;
    this.key = key;
    this.sslCtx = buildEncryptionHandler();
  }

    public ChannelHandler getEncryptionHandler() {

        ChannelHandler handler = sslCtx.newHandler(new PooledByteBufAllocator());

        String[] protocols = new String[] {"TLSv1.2"};
        ((SslHandler) handler).engine().setEnabledProtocols(protocols);

        return handler;

    }

  public SslContext buildEncryptionHandler() {
    try {

      final List<java.security.cert.X509Certificate> certList = new ArrayList<>();
      final String rawCertString = cert;
      PrivateKey privateKey;
      PublicKey publicKey;
      X509Certificate selfSignedCert = null;

      if (key != null) {
        X509CertificateGenerator.DERKeySpec derKeySpec = X509CertificateGenerator.parseDERKeySpec(key);
        privateKey = X509CertificateGenerator.buildPrivateKey(derKeySpec);
        publicKey = X509CertificateGenerator.buildPublicKey(derKeySpec);
      } else {
        selfSignedCert = SelfSignedX509CertGenerator.generate("*.xjeffrose.com");
        privateKey = selfSignedCert.getKey();
      }

      java.security.cert.X509Certificate[] chain;

      if (cert != null) {

        String[] certs = rawCertString.split("-----END CERTIFICATE-----\n");

        for (String cert : certs) {
          CertificateFactory cf = CertificateFactory.getInstance("X.509");
          java.security.cert.X509Certificate x509Certificate =
              (java.security.cert.X509Certificate) cf.generateCertificate(
                  new ByteArrayInputStream((cert + "-----END CERTIFICATE-----\n").getBytes()));
          certList.add(x509Certificate);
        }

        chain = new java.security.cert.X509Certificate[certList.size()];

        for (int i = 0; i < certList.size(); i++) {
          chain[i] = certList.get(i);
        }
      } else {
        if (selfSignedCert == null) {
          selfSignedCert = SelfSignedX509CertGenerator.generate("*.xjeffrose.com");
        }
        chain = new java.security.cert.X509Certificate[1];
        chain[0] = selfSignedCert.getCert();
      }

      SslContext _sslCtx = null;

        if (OpenSsl.isAvailable()) {
          log.info("Using OpenSSL");
          _sslCtx = SslContextBuilder
              .forServer(privateKey, chain)
              .sslProvider(SslProvider.OPENSSL)
              .build();
        } else {
          log.info("Using JSSE");
          final KeyStore keyStore = KeyStore.getInstance("JKS", "SUN");
          keyStore.load(null, PASSWORD.toCharArray());
          keyStore.setKeyEntry(chain[0].getIssuerX500Principal().getName(), privateKey,
              PASSWORD.toCharArray(), chain);
          KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");

          kmf.init(keyStore, PASSWORD.toCharArray());
          _sslCtx = SslContextBuilder
              .forServer(kmf)
              .sslProvider(SslProvider.JDK)
              .build();
        }

        return _sslCtx;

    } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException | CertificateException | NoSuchProviderException | IllegalArgumentException | IOException | SignatureException | InvalidKeyException e) {
      e.printStackTrace();
    }

    return null;
  }
}
