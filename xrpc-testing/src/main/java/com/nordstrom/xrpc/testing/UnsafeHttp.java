package com.nordstrom.xrpc.testing;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

/** DO NOT USE OUTSIDE OF TESTING. This class is used to help test HTTP. */
public class UnsafeHttp {
  public static OkHttpClient unsafeHttp11Client() {
    return unsafeHttp2Client(Protocol.HTTP_1_1);
  }

  public static OkHttpClient unsafeHttp2Client(Protocol... protocols) {
    try {
      X509TrustManager trustManager = unsafeTrustManager();
      final SSLSocketFactory sslSocketFactory = unsafeSslSocketFactory(trustManager);

      OkHttpClient.Builder builder =
          new OkHttpClient.Builder()
              .sslSocketFactory(sslSocketFactory, trustManager)
              .hostnameVerifier((hostname, session) -> true);

      if (protocols.length > 0) {
        // This defaults to http2, http1.1
        builder.protocols(Arrays.asList(protocols));
      }
      return builder.build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static SSLSocketFactory unsafeSslSocketFactory(X509TrustManager trustManager)
      throws NoSuchAlgorithmException, KeyManagementException {
    // Create a trust manager that does not validate certificate chains
    final TrustManager[] trustAllCerts = new TrustManager[] {trustManager};

    // Install the all-trusting trust manager
    final SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustAllCerts, null);
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
