package com.nordstrom.xrpc.server;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import com.nordstrom.xrpc.encoding.Encoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.io.IOException;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class ResponseFactory {
  private final XrpcRequest request;

  public HttpResponse ok() {
    return create(HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER, HttpHeaderValues.TEXT_PLAIN);
  }

  public <T> HttpResponse ok(T body) throws IOException {
    return create(HttpResponseStatus.OK, body);
  }

  public <T> HttpResponse badRequest(T body) throws IOException {
    return create(HttpResponseStatus.BAD_REQUEST, body);
  }

  public <T> HttpResponse notFound(T body) throws IOException {
    return create(HttpResponseStatus.NOT_FOUND, body);
  }

  public <T> HttpResponse forbidden(T body) throws IOException {
    return create(HttpResponseStatus.FORBIDDEN, body);
  }

  public <T> HttpResponse unauthorized(T body) throws IOException {
    return create(HttpResponseStatus.UNAUTHORIZED, body);
  }

  public <T> HttpResponse internalServerError(T body) throws IOException {
    return create(HttpResponseStatus.INTERNAL_SERVER_ERROR, body);
  }

  public <T> HttpResponse create(HttpResponseStatus status, T body) throws IOException {
    final Encoder encoder =
        request.connectionContext().encoders().acceptedEncoder(request.acceptHeader());
    ByteBuf buf = request.byteBuf();
    return create(
        status, encoder.encode(buf, request.acceptCharsetHeader(), body), encoder.mediaType());
  }

  public HttpResponse create(HttpResponseStatus status, ByteBuf body, CharSequence contentType) {
    return create(status, body, contentType, null);
  }

  public HttpResponse create(
      HttpResponseStatus status,
      ByteBuf body,
      CharSequence contentType,
      Map<String, String> customHeaders) {
    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, body);

    if (customHeaders != null) {
      for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
        response.headers().set(entry.getKey(), entry.getValue());
      }
    }

    response.headers().set(CONTENT_TYPE, contentType);
    response.headers().setInt(CONTENT_LENGTH, body.readableBytes());

    return response;
  }
}
