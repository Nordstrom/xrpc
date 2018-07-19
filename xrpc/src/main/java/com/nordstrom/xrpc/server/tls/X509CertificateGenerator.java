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
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.x509.X509CertImpl;

@Slf4j
public final class X509CertificateGenerator {

  private X509CertificateGenerator() {}

  public static DerKeySpec parseDerKeySpec(Path path) {
    String rawKeyString = null;
    try {
      rawKeyString =
          new String(Files.readAllBytes(path.toAbsolutePath()), XrpcConstants.DEFAULT_CHARSET);
    } catch (IOException e) {
      // TODO(JR): This is bad practice, we should fix this more elegantly
      throw new RuntimeException(
          new GeneralSecurityException("Could not parse a PKCS1 private key."));
    }

    return parseDerKeySpec(rawKeyString);
  }

  public static DerKeySpec parseDerKeySpec(String rawKeyString) {
    try {
      // Base64 decode the data
      Base64.Decoder b64decoder = Base64.getDecoder();
      byte[] encoded =
          b64decoder.decode(
              rawKeyString
                  .replace("-----BEGIN RSA PRIVATE KEY-----\n", "")
                  .replace("-----END RSA PRIVATE KEY-----\n", "")
                  .replace("\n", ""));

      DerInputStream derReader = new DerInputStream(encoded);
      DerValue[] seq = derReader.getSequence(0);

      if (seq.length != 9) {
        throw new RuntimeException(
            new GeneralSecurityException("Could not parse a PKCS1 private key."));
      }

      DerKeySpec ks = new DerKeySpec();

      ks.version = seq[0].getBigInteger();
      ks.modulus = seq[1].getBigInteger();
      ks.publicExp = seq[2].getBigInteger();
      ks.privateExp = seq[3].getBigInteger();
      ks.prime1 = seq[4].getBigInteger();
      ks.prime2 = seq[5].getBigInteger();
      ks.exp1 = seq[6].getBigInteger();
      ks.exp2 = seq[7].getBigInteger();
      ks.crtCoef = seq[8].getBigInteger();

      return ks;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static PrivateKey buildPrivateKey(DerKeySpec ks) {
    try {
      RSAPrivateCrtKeySpec keySpec = ks.rsaPrivateCrtKeySpec();

      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePrivate(keySpec);
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static PrivateKey parsePrivateKeyFromPem(String path) {
    DerKeySpec ks = parseDerKeySpec(path);
    return buildPrivateKey(ks);
  }

  public static PublicKey buildPublicKey(DerKeySpec ks) {
    try {
      RSAPublicKeySpec keySpec = ks.rsaPublicKeySpec();

      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePublic(keySpec);
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static PublicKey parsePublicKeyFromPem(String path) {
    DerKeySpec ks = parseDerKeySpec(path);
    return buildPublicKey(ks);
  }

  public static X509Certificate generate(String keyPath, String certPath) {
    FileInputStream certInputStream = null;

    try {

      DerKeySpec ks = parseDerKeySpec(Paths.get(keyPath));
      PrivateKey privateKey = buildPrivateKey(ks);
      PublicKey publicKey = buildPublicKey(ks);

      // Sign the cert to identify the algorithm that's used.
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      certInputStream = new FileInputStream(certPath);
      java.security.cert.X509Certificate x509Certificate =
          (java.security.cert.X509Certificate) cf.generateCertificate(certInputStream);
      X509CertImpl cert = (X509CertImpl) x509Certificate;

      try {
        cert.sign(privateKey, "SHA2withRSA");
        cert.verify(publicKey);

      } catch (NoSuchAlgorithmException
          | InvalidKeyException
          | NoSuchProviderException
          | SignatureException e) {
        // TODO(JR): Do something more useful if the key cannot be verified
        e.printStackTrace();
      }

      return new X509Certificate(cert.getIssuerX500Principal().getName(), privateKey, cert);
    } catch (FileNotFoundException | CertificateException e) {
      log.error("Failed to import x509 cert", e);
      throw new RuntimeException(e);
    } finally {
      if (certInputStream != null) {
        try {
          certInputStream.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  public static java.security.cert.X509Certificate[] parseX509Certificates(String rawCertString)
      throws CertificateException {
    java.security.cert.X509Certificate[] chain;
    final List<java.security.cert.X509Certificate> certList = new ArrayList<>();
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
    return chain;
  }

  public static class DerKeySpec {
    private BigInteger version;
    private BigInteger modulus;
    private BigInteger publicExp;
    private BigInteger privateExp;
    private BigInteger prime1;
    private BigInteger prime2;
    private BigInteger exp1;
    private BigInteger exp2;
    private BigInteger crtCoef;

    private RSAPrivateCrtKeySpec rsaPrivateCrtKeySpec() {
      return new RSAPrivateCrtKeySpec(
          modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef);
    }

    private RSAPublicKeySpec rsaPublicKeySpec() {
      return new RSAPublicKeySpec(modulus, publicExp);
    }
  }
}
