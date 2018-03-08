package com.nordstrom.xrpc.server;

import static io.netty.handler.codec.http.HttpMethod.OPTIONS;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * A helper class to handle a <a href="http://www.w3.org/TR/cors/">Cross Origin Resource Sharing</a>
 * (CORS) HTTP2 request. Ported from {@link io.netty.handler.codec.http.cors.CorsHandler}.
 *
 * <p>This handler can be configured using a {@link CorsConfig}, please refer to this class for
 * details about the configuration options available.
 */
@Slf4j
public class Http2CorsHandler {
  private static final String ANY_ORIGIN = "*";
  private static final String NULL_ORIGIN = "null";
  private final CorsConfig config;

  private String origin;

  public boolean handleHeaders(
      Http2Headers headers,
      ChannelHandlerContext ctx,
      int streamId,
      Http2ResponseWriter responseWriter) {
    if (!isCorsSupportEnabled()) {
      return false;
    }

    setOrigin(headers);
    Http2Headers responseHeaders = preflightHeaders();
    HttpResponseStatus status =
        isShortCircuit() && !validateOrigin()
            ? HttpResponseStatus.FORBIDDEN
            : HttpResponseStatus.OK;
    responseHeaders.status(status.codeAsText());

    if (!isPreflightRequest(headers) && !status.equals(HttpResponseStatus.FORBIDDEN)) {
      return false;
    }

    responseWriter.write(ctx, streamId, responseHeaders, Optional.of(Unpooled.EMPTY_BUFFER));
    return true;
  }

  public Http2CorsHandler() {
    CorsConfig config = CorsConfigBuilder.forAnyOrigin().disable().build();
    this.config = config;
  }

  /** Creates a new instance with the specified {@link CorsConfig}. */
  public Http2CorsHandler(CorsConfig config) {
    this.config = checkNotNull(config, "config");
  }

  private boolean isCorsSupportEnabled() {
    return config.isCorsSupportEnabled();
  }

  private boolean isPreflightRequest(final Http2Headers headers) {
    return headers.method().toString().equals(OPTIONS.name())
        && headers.contains(HttpHeaderNames.ORIGIN)
        && headers.contains(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD);
  }

  private Http2Headers preflightHeaders() {
    final Http2Headers headers = new DefaultHttp2Headers(true);
    if (setAccessAllowOriginHeader(headers)) {
      setAllowMethods(headers);
      setAllowHeaders(headers);
      setAllowCredentials(headers);
      setMaxAge(headers);
      setExposeHeaders(headers);
    }
    if (!headers.contains(HttpHeaderNames.CONTENT_LENGTH)) {
      headers.set(HttpHeaderNames.CONTENT_LENGTH, HttpHeaderValues.ZERO);
    }
    return headers;
  }

  private void setAllowMethods(final Http2Headers headers) {
    headers.add(
        HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, config.allowedRequestMethods().toString());
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

  private void setOrigin(Http2Headers headers) {
    this.origin = headers.get("origin").toString();
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

    if (origin == null) {
      return true;
    }

    if ("null".equals(origin) && config.isNullOriginAllowed()) {
      return true;
    }

    return config.origins().contains(origin);
  }

  // outbound
  protected boolean setAccessAllowOriginHeader(final Http2Headers headers) {

    if (origin != null) {
      if (NULL_ORIGIN.equals(origin) && config.isNullOriginAllowed()) {
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

      if (config.origins().contains(origin)) {
        setOrigin(headers, origin);
        setVaryHeader(headers);
        return true;
      }
      log.debug(
          "Request origin [{}]] was not among the configured origins [{}]",
          origin,
          config.origins());
    }
    return false;
  }

  protected void setOutBoundHeaders(final Http2Headers headers) {
    if (!isCorsSupportEnabled()) {
      return;
    }
    if (setAccessAllowOriginHeader(headers)) {
      setAllowCredentials(headers);
      setExposeHeaders(headers);
    }
  }

  private void echoRequestOrigin(final Http2Headers headers) {
    setOrigin(headers, origin);
  }

  private static void setVaryHeader(final Http2Headers headers) {
    headers.set(HttpHeaderNames.VARY, HttpHeaderNames.ORIGIN);
  }
}
