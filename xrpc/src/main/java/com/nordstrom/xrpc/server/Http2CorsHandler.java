package com.nordstrom.xrpc.server;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Initializes CORS support for this HTTP2 request.
 *
 * <ul>
 *   <li>Request headers are processed by the inbound method, which responds to forbidden origins or
 *       pre-flight requests.
 *   <li>Response headers for cors requests are written with the outbound method.
 *   <li>When reading inbound headers, the inbound method saves the request origin for use in
 *       outbound headers.
 * </ul>
 *
 * Adapted from from {@link io.netty.handler.codec.http.cors.CorsHandler}.
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
   * Processes inbound HTTP2 CORS requests.
   *
   * <ul>
   *   <li>if shortCircuit is set to true, will write a forbidden response
   *   <li>will respond to pre-flight requests
   *   <li>stores request origin for use in outbound headers
   * </ul>
   *
   * @param headers for the inbound request
   * @param responseWriter used to respond to forbidden origins or pre-flight requests
   * @return false if the pipeline should continue handling the request.
   */
  public boolean inbound(
      ChannelHandlerContext ctx,
      int streamId,
      Http2Headers headers,
      Http2ResponseWriter responseWriter) {
    if (!config.isCorsSupportEnabled()) {
      return false;
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
      return false;
    }

    responseWriter.write(ctx, streamId, responseHeaders, Optional.of(Unpooled.EMPTY_BUFFER));
    return true;
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
   * @param responseHeaders
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
