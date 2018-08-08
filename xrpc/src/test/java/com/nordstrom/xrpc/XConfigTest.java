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

package com.nordstrom.xrpc;

import static io.netty.handler.ssl.ApplicationProtocolConfig.Protocol.ALPN;
import static io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT;
import static io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.DatatypeConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class XConfigTest {
  private static final Set<Object> NONE = ImmutableSet.of();
  private static final List<String> SUPPORTED_PROTOCOLS =
      ImmutableList.of(
          "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
          "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
          "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
          "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
          "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
          "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
  private static final int TEN_MEGABYTES = 10485760;
  private static final int SECONDS = 1;
  private static final ImmutableList<String> SUPPORTED_PROTOCOLS_IN_PREFERENCE_ORDER =
      ImmutableList.of("h2", "http/1.1");
  private static XConfig config;
  private static TlsConfig tlsConfig;
  private static CorsConfig corsConfig;
  private static ApplicationProtocolConfig applicationProtocolConfig;
  private static Config xrpcDefaultConfig;

  @BeforeAll
  static void setup() {
    config = new XConfig();
    corsConfig = config.corsConfig();
    tlsConfig = config.tlsConfig();
    applicationProtocolConfig = tlsConfig.getAlpnConfig();
    xrpcDefaultConfig =
        ConfigFactory.parseResources(XConfigTest.class, "/com/nordstrom/xrpc/xrpc.conf");
  }

  @Test
  void getClientRateLimitOverride() {
    Config rawConfig = ConfigFactory.load("test.conf");
    XConfig config = new XConfig(rawConfig.getConfig("xrpc"));

    Map<String, List<Double>> configTest = config.getClientRateLimitOverride();

    double expected1 = Double.parseDouble("550");
    double testVal1 = configTest.get("localhost").get(0);
    assertEquals(expected1, testVal1);

    double expected2 = Double.parseDouble("1");
    double testVal2 = configTest.get("1.2.3.4").get(0);

    double expected3 = Double.parseDouble("2");
    double testVal3 = configTest.get("1.2.3.4").get(1);

    assertEquals(expected2, testVal2);
    assertEquals(expected3, testVal3);
  }

  @Test
  void shouldSetDefaultXrpcConfigValues() {
    assertEquals(TEN_MEGABYTES, config.maxPayloadBytes());
    assertEquals(200 * SECONDS, config.readerIdleTimeout());
    assertEquals(400 * SECONDS, config.writerIdleTimeout());
    assertEquals(0, config.allIdleTimeout());
    assertEquals("xrpc-worker-%d", config.workerNameFormat());
    assertEquals(4, config.bossThreadCount());
    assertEquals(40, config.workerThreadCount());
    assertEquals(0, config.asyncHealthCheckThreadCount());
    assertEquals(2000, config.maxConnections());
    assertEquals(24, config.rateLimiterPoolSize());
    assertEquals(500.0d, config.softReqPerSec());
    assertEquals(550.0d, config.hardReqPerSec());
    assertEquals(700.0d, config.globalSoftReqPerSec());
    assertEquals(750.0d, config.globalHardReqPerSec());
    assertEquals(
        ImmutableMap.of("127.0.0.1", Arrays.asList(500d, 550d)),
        config.getClientRateLimitOverride());
    assertEquals(30 * SECONDS, config.slf4jReporterPollingRate());
    assertEquals(30 * SECONDS, config.consoleReporterPollingRate());
    assertEquals(NONE, config.ipBlackList());
    assertEquals(NONE, config.ipWhiteList());
    assertEquals("application/json", config.defaultContentType());
    assertThat(config.port(), is(8080));
    assertTrue(config.adminRoutesEnableInfo());
    assertTrue(config.jmxReporter());
    assertFalse(config.slf4jReporter());
    assertFalse(config.adminRoutesEnableUnsafe());
    assertFalse(config.consoleReporter());
  }

  @Test
  void shouldSetDefaultCorsConfigValues() {
    assertEquals(NONE, corsConfig.origins());
    assertEquals(NONE, corsConfig.allowedRequestHeaders());
    assertEquals(NONE, corsConfig.allowedRequestMethods());
    assertFalse(corsConfig.isCorsSupportEnabled());
    assertFalse(corsConfig.isCredentialsAllowed());
    assertFalse(corsConfig.isShortCircuit());
  }

  @Test
  void shouldOnlySupportEngineeringStandardsDefinedCypherSuitesByDefault() {
    List<String> defaultSupportedProtocols = tlsConfig.getCiphers();
    assertEquals(6, defaultSupportedProtocols.size());
    for (String protocol : defaultSupportedProtocols) {
      assertTrue(SUPPORTED_PROTOCOLS.contains(protocol));
    }
  }

  @Test
  void shouldSetSensibleAlpnValuesByDefault() {
    assertEquals(
        SUPPORTED_PROTOCOLS_IN_PREFERENCE_ORDER, applicationProtocolConfig.supportedProtocols());
    assertEquals(NO_ADVERTISE, applicationProtocolConfig.selectorFailureBehavior());
    assertEquals(ACCEPT, applicationProtocolConfig.selectedListenerFailureBehavior());
    assertEquals(ALPN, applicationProtocolConfig.protocol());
  }

  @Test
  void shouldUseUserSuppliedCertificateWhenDefined() throws IOException, URISyntaxException {
    Config configWithCertPaths =
        ConfigFactory.parseMap(
            ImmutableMap.of(
                "tls",
                ImmutableMap.of(
                    "privateKeyPath",
                    getResourceAbsolutePath("testing-purposes-only-key.pem"),
                    "x509CertPath",
                    getResourceAbsolutePath("testing-purposes-only-certificate.crt"))));
    TlsConfig tlsConfig =
        new XConfig(configWithCertPaths.withFallback(xrpcDefaultConfig)).tlsConfig();

    String privateKey = getFileContent("testing-purposes-only-key.pem");
    String certificate = getFileContent("testing-purposes-only-certificate.crt");

    assertThat(tlsConfig.getPrivateKey(), is(parsePrivateKeyFromPem(privateKey)));
    assertThat(tlsConfig.getCertificateAndChain().length, is(1));
    assertThat(tlsConfig.getCertificateAndChain()[0], is(parseX509CertificateFromPem(certificate)));
  }

  @Test
  void shouldGenerateSelfSignedCertificateWhenNoneAreProvided()
      throws IOException, URISyntaxException {
    String privateKey = getFileContent("testing-purposes-only-key.pem");
    String certificate = getFileContent("testing-purposes-only-certificate.crt");

    assertThat(tlsConfig.getPrivateKey(), is(not(parsePrivateKeyFromPem(privateKey))));
    assertThat(tlsConfig.getCertificateAndChain().length, is(1));
    assertThat(
        tlsConfig.getCertificateAndChain()[0], is(not(parseX509CertificateFromPem(certificate))));
  }

  @Test
  void shouldLogWhenInsecureConfigurationIsUsedByDefault() {
    assertTrue(tlsConfig.isLogInsecureConfig());
  }

  @Test
  void shouldUseTlsByDefault() {
    assertTrue(tlsConfig.isUseSsl());
  }

  @Test
  void shouldSetClientAuthToOptionalByDefault() {
    assertEquals(ClientAuth.OPTIONAL, tlsConfig.getClientAuth());
  }

  @Test
  void shouldDisableOcspByDefault() {
    assertFalse(tlsConfig.isEnableOcsp());
  }

  @Test
  void shouldSetSessionTimeoutToZeroByDefault() {
    assertEquals(0, tlsConfig.getSessionTimeout());
  }

  @Test
  void shouldSetSessionCacheSizeToZeroByDefault() {
    assertEquals(0, tlsConfig.getSessionCacheSize());
  }

  @Test
  void shouldUseOpenSslByDefault() {
    assertEquals(SslProvider.OPENSSL, tlsConfig.getSslProvider());
  }

  private X509Certificate parseX509CertificateFromPem(String pemData) {
    try {
      CertificateFactory fact = CertificateFactory.getInstance("X.509");
      ByteArrayInputStream is = new ByteArrayInputStream(pemData.getBytes(StandardCharsets.UTF_8));
      X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
      return cer;
    } catch (CertificateException var4) {
      return null;
    }
  }

  private String getFileContent(String resourceName) throws URISyntaxException, IOException {
    URI url = this.getClass().getClassLoader().getResource(resourceName).toURI();
    Path resPath = Paths.get(url);
    return new String(Files.readAllBytes(resPath), "UTF8");
  }

  private String getResourceAbsolutePath(String resourceName) {
    return this.getClass().getClassLoader().getResource(resourceName).getPath();
  }

  private static PrivateKey parsePrivateKeyFromPem(String pemData) {
    PrivateKey key;
    try {
      StringBuilder builder = new StringBuilder();
      boolean inKey = false;
      for (String line : pemData.split("\n")) {
        if (!inKey) {
          if (line.startsWith("-----BEGIN ") && line.endsWith(" PRIVATE KEY-----")) {
            inKey = true;
          }
        } else {
          if (line.startsWith("-----END ") && line.endsWith(" PRIVATE KEY-----")) {
            break;
          }
          builder.append(line);
        }
      }

      byte[] encoded = DatatypeConverter.parseBase64Binary(builder.toString());
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      key = kf.generatePrivate(keySpec);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
    return key;
  }
}
