package com.nordstrom.xrpc.server;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Initializes CORS support for this HTTP2 request.
 *
 * <ul>
 *   <li>Request headers are processed by the inbound method, which returns response headers for
 *       forbidden origins or pre-flight requests.
 *   <li>Response headers for cors requests are written with the outbound method.
 *   <li>When reading inbound headers, the inbound method saves the request origin for use in
 *       outbound headers.
 * </ul>
 *
 * <p>Adapted from from {@link io.netty.handler.codec.http.cors.CorsHandler}.
 *
 * <p>This handler can be configured using a {@link CorsConfig}, please refer to that class for
 * details about the configuration options available.
 */
@Slf4j
public class Http2CorsHandler {
  private static final String ANY_ORIGIN = "*";
  private static final String NULL_ORIGIN = "null";
  private final CorsConfig config;

  private String requestOrigin;

  /** Creates a new instance with the specified {@link CorsConfig}. */
  public Http2CorsHandler(CorsConfig config) {
    this.config = checkNotNull(config, "config");
  }

  /**
   * Processes inbound HTTP2 CORS requests. Saves the request origin for use in outbound headers.
   *
   * @param headers from the request
   * @return Optional response headers for requests that do not need to continue down the pipeline.
   */
  public Optional<HttpResponse> inbound(Http2Headers headers, int streamId) {
    if (!config.isCorsSupportEnabled()) {
      return Optional.empty();
    }

    boolean isPreflight = isPreflight(headers);

    this.requestOrigin = headers.get(HttpHeaderNames.ORIGIN).toString();

    HttpMethod requestMethod = requestMethod(headers, isPreflight);

    Http2Headers responseHeaders = preflightHeaders(requestMethod);
    HttpResponseStatus status =
        isShortCircuit() && !validateOrigin()
            ? HttpResponseStatus.FORBIDDEN
            : HttpResponseStatus.OK;
    responseHeaders.status(status.codeAsText());

    if (!isPreflight && !status.equals(HttpResponseStatus.FORBIDDEN)) {
      return Optional.empty();
    }

    HttpResponse response;

    try {
      response = HttpConversionUtil.toHttpResponse(streamId, responseHeaders, true);
    } catch (Http2Exception e) {
      log.error("Error in handling CORS headers.", e);

      response =
          new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    return Optional.of(response);
  }

  private HttpMethod requestMethod(Http2Headers headers, boolean isPreflight) {
    return isPreflight
        ? HttpMethod.valueOf(headers.get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD).toString())
        : HttpMethod.OPTIONS;
  }

  private boolean isPreflight(final Http2Headers headers) {
    return headers.method().toString().equals(HttpMethod.OPTIONS.toString())
        && headers.contains(HttpHeaderNames.ORIGIN)
        && headers.contains(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD);
  }

  private Http2Headers preflightHeaders(HttpMethod requestMethod) {
    final Http2Headers responseHeaders = new DefaultHttp2Headers(true);
    if (setAccessAllowOriginHeader(responseHeaders)) {
      setAllowMethods(responseHeaders, requestMethod);
      setAllowHeaders(responseHeaders);
      setAllowCredentials(responseHeaders);
      setMaxAge(responseHeaders);
      setExposeHeaders(responseHeaders);
    }
    if (!responseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)) {
      responseHeaders.set(HttpHeaderNames.CONTENT_LENGTH, HttpHeaderValues.ZERO);
    }
    return responseHeaders;
  }

  private void setAllowMethods(final Http2Headers headers, HttpMethod requestMethod) {
    if (config.allowedRequestMethods().contains(requestMethod)) {
      headers.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, requestMethod.toString());
    }
  }

  private void setAllowHeaders(final Http2Headers headers) {
    headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, config.allowedRequestHeaders());
  }

  private void setMaxAge(final Http2Headers headers) {
    headers.set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, String.valueOf(config.maxAge()));
  }

  private void setAllowCredentials(final Http2Headers headers) {
    if (config.isCredentialsAllowed()
        && !headers.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN).equals(ANY_ORIGIN)) {
      headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }
  }

  private void setExposeHeaders(final Http2Headers headers) {
    if (!config.exposedHeaders().isEmpty()) {
      headers.set(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS, config.exposedHeaders());
    }
  }

  private static void setAnyOrigin(final Http2Headers headers) {
    setOrigin(headers, ANY_ORIGIN);
  }

  private static void setNullOrigin(final Http2Headers headers) {
    setOrigin(headers, NULL_ORIGIN);
  }

  private static void setOrigin(final Http2Headers headers, final String origin) {
    headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
  }

  private boolean isShortCircuit() {
    return config.isShortCircuit();
  }

  private boolean validateOrigin() {
    if (config.isAnyOriginSupported()) {
      return true;
    }

    if (requestOrigin == null) {
      return true;
    }

    if ("null".equals(requestOrigin) && config.isNullOriginAllowed()) {
      return true;
    }

    return config.origins().contains(requestOrigin);
  }

  /**
   * Applies appropriate CORS Headers to outbound response headers.
   *
   * @param responseHeaders outbound response headers
   */
  protected void outbound(final Http2Headers responseHeaders) {
    if (!config.isCorsSupportEnabled()) {
      return;
    }
    if (setAccessAllowOriginHeader(responseHeaders)) {
      setAllowCredentials(responseHeaders);
      setExposeHeaders(responseHeaders);
    }
  }

  private boolean setAccessAllowOriginHeader(final Http2Headers headers) {

    if (requestOrigin != null) {
      if (NULL_ORIGIN.equals(requestOrigin) && config.isNullOriginAllowed()) {
        setNullOrigin(headers);
        return true;
      }

      if (config.isAnyOriginSupported()) {
        if (config.isCredentialsAllowed()) {
          echoRequestOrigin(headers);
          setVaryHeader(headers);
        } else {
          setAnyOrigin(headers);
        }
        return true;
      }

      if (config.origins().contains(requestOrigin)) {
        setOrigin(headers, requestOrigin);
        setVaryHeader(headers);
        return true;
      }
      log.debug(
          "Request origin [{}]] was not among the configured origins [{}]",
          requestOrigin,
          config.origins());
    }
    return false;
  }

  private void echoRequestOrigin(final Http2Headers headers) {
    setOrigin(headers, requestOrigin);
  }

  private static void setVaryHeader(final Http2Headers headers) {
    headers.set(HttpHeaderNames.VARY, HttpHeaderNames.ORIGIN);
  }
}
