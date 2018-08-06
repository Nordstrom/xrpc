package com.nordstrom.xrpc.server.tls;

import static com.nordstrom.xrpc.server.tls.Tls.createSelfSigned;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TlsTest {

  private static X509Certificate x509Certificate;

  @BeforeAll
  static void setup() {
    x509Certificate = createSelfSigned();
  }

  @Test
  void shouldCreateSelfSignedThatWildcardsNordstrom() {
    assertEquals("*.nordstrom.com", x509Certificate.fqdn());
  }

  @Test
  void shouldSetCertificateToWildcardNordstromCommonName() {
    assertEquals("CN=*.nordstrom.com", x509Certificate.cert().getIssuerX500Principal().getName());
  }

  @Test
  void shouldUtiliseSha256WithRsa() {
    assertEquals("SHA256withRSA", x509Certificate.cert().getSigAlgName());
  }

  @Test
  void shouldCreateCertificateWithStartingValidityInPast() {
    assertTrue(new Date().after(x509Certificate.cert().getNotBefore()));
  }

  @Test
  void shouldCreateCertificateWithValidityUntilInFuture() {
    assertTrue(new Date().before(x509Certificate.cert().getNotAfter()));
  }

  @Test
  void shouldGenerateKeyWithRsa() {
    assertEquals("RSA", x509Certificate.key().getAlgorithm());
  }
}
