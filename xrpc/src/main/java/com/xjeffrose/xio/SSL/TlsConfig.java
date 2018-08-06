package com.xjeffrose.xio.SSL;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.nordstrom.xrpc.server.tls.Tls;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.DatatypeConverter;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Strings.*;

@Slf4j
public class TlsConfig {

  @Getter private final boolean useSsl;
  // used internally
  private final boolean useOpenSsl;
  @Getter private final boolean logInsecureConfig;
  @Getter private final PrivateKey privateKey;
  // custom getter
  private final X509Certificate certificate;
  @Getter private final List<X509Certificate> x509TrustedCerts;
  private final ImmutableList<X509Certificate> x509CertChain;
  @Getter private final ApplicationProtocolConfig alpnConfig;
  // custom getter
  private final List<String> ciphers;
  @Getter private final ClientAuth clientAuth;
  @Getter private final boolean enableOcsp;
  // custom getter
  private final List<String> protocols;
  @Getter private final long sessionCacheSize;
  @Getter private final long sessionTimeout;

  private static String readPath(String path) {
    try {
      return new String(Files.readAllBytes(Paths.get(path).toAbsolutePath()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String readUrlResource(URL url) {
    try {
      URLConnection connection = url.openConnection();

      connection.connect();

      InputStream stream = connection.getInputStream();

      return CharStreams.toString(new InputStreamReader(stream, Charsets.UTF_8));
    } catch (IOException e) {
      return null;
    }
  }

  private static String readClasspathResource(String resource) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) {
      throw new RuntimeException("null class loader");
    }

    URL url = loader.getResource(resource);
    if (url == null) {
      throw new RuntimeException("resource not found on classpath: " + resource);
    }

    return readUrlResource(url);
  }

  private static String readPathFromValue(String value) {
    if (value.startsWith("classpath:")) {
      return readClasspathResource(value.replace("classpath:", ""));
    }
    if (value.startsWith("url:")) {
      try {
        URL url = new URL(value.replace("url:", ""));
        return readUrlResource(url);
      } catch (MalformedURLException e) {
        return null;
      }
    }
    return readPath(value);
  }

  private static String readPathFromKey(String key, Config config) {
    return readPathFromValue(config.getString(key));
  }

  private static PrivateKey parsePkcs8FormattedPrivateKeyFromPem(String pemData) {
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

  private static X509Certificate parseX509CertificateFromPem(String pemData) {
    try {
      CertificateFactory fact = CertificateFactory.getInstance("X.509");
      ByteArrayInputStream is = new ByteArrayInputStream(pemData.getBytes(StandardCharsets.UTF_8));
      return (X509Certificate) fact.generateCertificate(is);
    } catch (CertificateException e) {
      log.error("Failed to parse X509 certificate from PEM", e);
      throw new RuntimeException(e);
    }
  }

  private static ApplicationProtocolConfig buildAlpnConfig(Config config) {
    ApplicationProtocolConfig.Protocol protocol =
      config.getEnum(ApplicationProtocolConfig.Protocol.class, "protocol");
    ApplicationProtocolConfig.SelectorFailureBehavior selectorBehavior =
      config.getEnum(ApplicationProtocolConfig.SelectorFailureBehavior.class, "selectorBehavior");
    ApplicationProtocolConfig.SelectedListenerFailureBehavior selectedBehavior =
      config.getEnum(
        ApplicationProtocolConfig.SelectedListenerFailureBehavior.class, "selectedBehavior");
    List<String> supportedProtocols = config.getStringList("supportedProtocols");
    return new ApplicationProtocolConfig(
      protocol, selectorBehavior, selectedBehavior, supportedProtocols);
  }

  private static List<X509Certificate> buildCerts(List<String> paths) {
    List<X509Certificate> certificates = new ArrayList<>();

    for (String path : paths) {
      certificates.add(parseX509CertificateFromPem(readPathFromValue(path)));
    }

    return certificates;
  }

  public TlsConfig(Config config) {
    if (isNullOrEmpty(config.getString("privateKeyPath")) || isNullOrEmpty(config.getString("x509CertPath"))) {
      log.info("Private key path or x509 certificate path not defined. Generating self signed certificate.");
      com.nordstrom.xrpc.server.tls.X509Certificate selfSignedCertificate = Tls.createSelfSigned();
      certificate = selfSignedCertificate.cert();
      privateKey = selfSignedCertificate.key();
    } else {
      log.info("Using provided certificate located at {}", config.getString("x509CertPath"));
      log.info("Using provided private key located at {}", config.getString("privateKeyPath"));
      certificate = parseX509CertificateFromPem(readPathFromKey("x509CertPath", config));
      privateKey = parsePkcs8FormattedPrivateKeyFromPem(readPathFromKey("privateKeyPath", config));
    }
    useSsl = config.getBoolean("useSsl");
    logInsecureConfig = config.getBoolean("logInsecureConfig");
    x509TrustedCerts = buildCerts(config.getStringList("x509TrustedCertPaths"));
    x509CertChain =
      new ImmutableList.Builder<X509Certificate>()
        .add(certificate)
        .addAll(buildCerts(config.getStringList("x509CertChainPaths")))
        .build();
    useOpenSsl = config.getBoolean("useOpenSsl");
    alpnConfig = buildAlpnConfig(config.getConfig("alpn"));
    ciphers = config.getStringList("ciphers");
    clientAuth = config.getEnum(ClientAuth.class, "clientAuth");
    enableOcsp = config.getBoolean("enableOcsp");
    protocols = config.getStringList("protocols");
    sessionCacheSize = config.getLong("sessionCacheSize");
    sessionTimeout = config.getLong("sessionTimeout");
  }

  public static TlsConfig fromConfig(String key, Config config) {
    return new TlsConfig(config.getConfig(key));
  }

  public static TlsConfig fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }

  public List<String> getCiphers() {
    if (ciphers.size() == 0) {
      return null;
    }
    return ciphers;
  }

  public String[] getProtocols() {
    if (protocols.size() == 0) {
      return null;
    }
    return protocols.toArray(new String[0]);
  }

  public X509Certificate[] getCertificateAndChain() {
    return x509CertChain.toArray(new X509Certificate[0]);
  }

  public X509Certificate[] getTrustedCerts() {
    return x509TrustedCerts.toArray(new X509Certificate[0]);
  }

  public SslProvider getSslProvider() {
    if (useOpenSsl) {
      if (!OpenSsl.isAvailable()) {
        throw new IllegalStateException("useOpenSsl = true and OpenSSL is not available");
      }
      return SslProvider.OPENSSL;
    }
    return SslProvider.JDK;
  }
}
