package com.nordstrom.xrpc.server;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import com.nordstrom.xrpc.XrpcConstants;
import com.nordstrom.xrpc.encoding.Encoder;
import com.nordstrom.xrpc.exceptions.HttpResponseException;
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
    return create(HttpResponseStatus.OK, Unpooled.buffer(0), HttpHeaderValues.TEXT_PLAIN);
  }

  public <T> HttpResponse ok(T body) throws IOException {
    return create(HttpResponseStatus.OK, body);
  }

  public <T> HttpResponse badRequest(T body) throws IOException {
    return create(HttpResponseStatus.BAD_REQUEST, body);
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

  public HttpResponse exception(Exception exception) {
    try {
      // TODO (AD): Use configured response content/type and response specific exceptions to default
      // to meaningful responses here
      log.error("Handler Exception:", exception);

      if (exception instanceof HttpResponseException) {
        HttpResponseException responseException = (HttpResponseException) exception;
        return create(
            HttpResponseStatus.valueOf(responseException.getStatusCode()), responseException);
      }
      // TODO (AD): Handle other exceptions that can reasonably be converted to HTTP Responses.
      // For example IllegalArgumentException could become HTTP 400 Bad Request
    } catch (Exception e) {
      log.error("Error Handling Exception:", e);
    }
    return create(
        HttpResponseStatus.INTERNAL_SERVER_ERROR,
        Unpooled.wrappedBuffer(XrpcConstants.INTERNAL_SERVER_ERROR_RESPONSE),
        HttpHeaderValues.TEXT_PLAIN);
  }

  public <T> HttpResponse create(HttpResponseStatus status, T body) throws IOException {
    final Encoder encoder =
        request.connectionContext().encoders().acceptedEncoder(request.acceptHeader());
    ByteBuf buf = request.byteBuf();
    return create(status, encoder.encode(buf, body), encoder.contentType());
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
