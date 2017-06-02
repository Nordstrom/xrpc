package com.xjeffrose.xrpc;

import java.security.PrivateKey;
import lombok.extern.slf4j.Slf4j;
import sun.security.x509.X509CertImpl;

@Slf4j
public final class X509Certificate {


  private final String fqdn;
  private final PrivateKey key;
  private final X509CertImpl cert;

  public X509Certificate(String fqdn, PrivateKey key, X509CertImpl cert) {

    this.fqdn = fqdn;
    this.key = key;
    this.cert = cert;
  }

  public String getFqdn() {
    return fqdn;
  }

  public PrivateKey getKey() {
    return key;
  }

  public X509CertImpl getCert() {
    return cert;
  }
}
