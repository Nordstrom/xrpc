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
 *   <li>Request headers are processed by the inbound method, which returns an http1 response for
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
   * @return Optional http1 response for requests that do not need to continue down the pipeline.
   */
  public Optional<HttpResponse> inbound(Http2Headers headers, int streamId) {
    try {
      if (!config.isCorsSupportEnabled()) {
        return Optional.empty();
      }

      boolean isPreflight = isPreflight(headers);

      // Save value for use in outbound header generation.
      CharSequence origin = headers.get(HttpHeaderNames.ORIGIN);
      this.requestOrigin = origin == null ? null : origin.toString();

      HttpResponseStatus status =
          config.isShortCircuit() && !validateOrigin()
              ? HttpResponseStatus.FORBIDDEN
              : HttpResponseStatus.OK;

      if (!isPreflight && !status.equals(HttpResponseStatus.FORBIDDEN)) {
        return Optional.empty();
      }

      HttpMethod requestMethod = requestMethod(headers, isPreflight);
      Http2Headers responseHeaders = preflightHeaders(requestMethod);
      responseHeaders.status(status.codeAsText());

      HttpResponse response;

      try {
        // TODO: This gets converted to back to Http2 for status logging, there has to be a better
        // way.
        response = HttpConversionUtil.toHttpResponse(streamId, responseHeaders, true);
      } catch (Http2Exception e) {
        log.error("Error in handling CORS headers.", e);

        response =
            new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
      }

      return Optional.of(response);
    } catch (Exception e) {
      // TODO: (AD) once we've got exception handling in Http2Handler, this can be removed.
      log.error("Error handling CORS inbound", e);
      return Optional.of(
          new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR));
    }
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

    responseHeaders.set(HttpHeaderNames.CONTENT_LENGTH, HttpHeaderValues.ZERO);

    if (!setAccessAllowOriginHeader(responseHeaders)) {
      return responseHeaders;
    }

    if (config.allowedRequestMethods().contains(requestMethod)) {
      responseHeaders.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, requestMethod.toString());
    }

    if (config.isCredentialsAllowed()
        && !responseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN).equals(ANY_ORIGIN)) {
      responseHeaders.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    if (!config.exposedHeaders().isEmpty()) {
      responseHeaders.set(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS, config.exposedHeaders());
    }

    responseHeaders.set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, String.valueOf(config.maxAge()));
    responseHeaders.set(
        HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, config.allowedRequestHeaders());

    return responseHeaders;
  }

  /** True if the given origin is allowed based on the CORS configuration. */
  private boolean validateOrigin() {
    if (config.isAnyOriginSupported()) {
      return true;
    }

    if (requestOrigin == null) {
      return true;
    }

    if (NULL_ORIGIN.equals(requestOrigin) && config.isNullOriginAllowed()) {
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
    if (!config.isCorsSupportEnabled() || !setAccessAllowOriginHeader(responseHeaders)) {
      return;
    }

    if (config.isCredentialsAllowed()
        && !responseHeaders.get(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN).equals(ANY_ORIGIN)) {
      responseHeaders.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    if (!config.exposedHeaders().isEmpty()) {
      responseHeaders.set(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS, config.exposedHeaders());
    }
  }

  /**
   * If the origin is allowed, sets the outbound access-allow-origin header to be the request origin
   * and returns true.
   *
   * @param headers for the response.
   * @return true if the request origin is allowed by the CORS configuration.
   */
  private boolean setAccessAllowOriginHeader(final Http2Headers headers) {

    if (requestOrigin != null) {
      if (NULL_ORIGIN.equals(requestOrigin) && config.isNullOriginAllowed()) {
        headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, NULL_ORIGIN);
        return true;
      }

      if (config.isAnyOriginSupported()) {
        if (config.isCredentialsAllowed()) {
          headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
          headers.set(HttpHeaderNames.VARY, HttpHeaderNames.ORIGIN);
        } else {
          headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, ANY_ORIGIN);
        }
        return true;
      }

      if (config.origins().contains(requestOrigin)) {
        headers.set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
        headers.set(HttpHeaderNames.VARY, HttpHeaderNames.ORIGIN);
        return true;
      }
      log.debug(
          "Request origin [{}]] was not among the configured origins [{}]",
          requestOrigin,
          config.origins());
    }
    return false;
  }
}
