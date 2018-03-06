package com.nordstrom.xrpc.testing;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

/** DO NOT USE OUTSIDE OF TESTING. This class is used to help test HTTP. */
public class UnsafeHttp {
  public static OkHttpClient unsafeClient() {
    try {
      X509TrustManager trustManager = unsafeTrustManager();
      final SSLSocketFactory sslSocketFactory = unsafeSslSocketFactory(null, trustManager);

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

  public static OkHttpClient unsafeClientH2() {
    try {
      X509TrustManager trustManager = unsafeTrustManager();
      final SSLSocketFactory sslSocketFactory = unsafeSslSocketFactory(null, trustManager);

      OkHttpClient okHttpClient =
          new OkHttpClient.Builder()
              .protocols(Arrays.asList(Protocol.HTTP_1_1, Protocol.HTTP_2))
              .sslSocketFactory(sslSocketFactory, trustManager)
              .hostnameVerifier((hostname, session) -> true)
              .build();

      return okHttpClient;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static SSLSocketFactory unsafeSslSocketFactory(
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

  public static X509TrustManager unsafeTrustManager() {
    return new X509TrustManager() {
      @Override
      public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

      @Override
      public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

      @Override
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }
    };
  }
}
