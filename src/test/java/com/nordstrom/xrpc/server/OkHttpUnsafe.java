package com.nordstrom.xrpc.server;

import com.xjeffrose.xio.SSL.TlsConfig;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;

public class OkHttpUnsafe {

  public static KeyManager[] getKeyManagers(TlsConfig config) throws Exception {
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    keystore.load(null, "".toCharArray());
    keystore.setKeyEntry(
        "server", config.getPrivateKey(), "".toCharArray(), config.getCertificateAndChain());
    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keystore, "".toCharArray());
    return keyManagerFactory.getKeyManagers();
  }

  public static KeyManager[] getEmptyKeyManager() throws Exception {
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    keystore.load(null, "".toCharArray());
    KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keystore, "".toCharArray());
    return keyManagerFactory.getKeyManagers();
  }

  public static X509TrustManager unsafeTrustManager() {
    return new X509TrustManager() {
      @Override
      public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
          throws CertificateException {}

      @Override
      public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
          throws CertificateException {}

      @Override
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }
    };
  }

  public static SSLSocketFactory getUnsafeSslSocketFactory(
      KeyManager[] keyManagers, X509TrustManager trustManager)
      throws NoSuchAlgorithmException, KeyManagementException {
    // Create a trust manager that does not validate certificate chains
    final TrustManager[] trustAllCerts = new TrustManager[] {trustManager};

    // Install the all-trusting trust manager
    final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(keyManagers, trustAllCerts, new java.security.SecureRandom());
    // Create an ssl socket factory with our all-trusting manager
    return sslContext.getSocketFactory();
  }

  public static OkHttpClient getUnsafeClient() {
    try {
      X509TrustManager trustManager = unsafeTrustManager();
      final SSLSocketFactory sslSocketFactory = getUnsafeSslSocketFactory(null, trustManager);

      OkHttpClient okHttpClient =
          new OkHttpClient.Builder()
              .sslSocketFactory(sslSocketFactory, trustManager)
              .hostnameVerifier((hostname, session) -> true)
              .build();

      return okHttpClient;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static MockWebServer getSslMockWebServer(TlsConfig config) {
    try {
      MockWebServer server = new MockWebServer();
      server.useHttps(
          OkHttpUnsafe.getUnsafeSslSocketFactory(getKeyManagers(config), unsafeTrustManager()),
          false);
      return server;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
