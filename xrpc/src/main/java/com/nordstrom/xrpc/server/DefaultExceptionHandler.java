package com.nordstrom.xrpc.server;

import com.nordstrom.xrpc.XrpcConstants;
import com.nordstrom.xrpc.encoding.ContentTypeEncoder;
import com.nordstrom.xrpc.encoding.Encoder;
import com.nordstrom.xrpc.exceptions.HttpResponseException;
import com.nordstrom.xrpc.server.http.Recipes;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultExceptionHandler implements ExceptionHandler {
  @Override
  public HttpResponse handle(XrpcRequest request, Exception exception) {
    try {
      // TODO (AD): Use configured response content/type and response specific exceptions to default
      // to meaningful responses here
      log.error("Handler Exception:", exception);

      final CharSequence accept = request.getHeader(HttpHeaderNames.ACCEPT);
      final ContentTypeEncoder contentTypeEncoder =
          request.getConnectionContext().getEncoders().acceptedEncoder(accept);
      final CharSequence contentType = contentTypeEncoder.contentType();
      final Encoder encoder = contentTypeEncoder.encoder();
      if (exception instanceof HttpResponseException) {
        HttpResponseException responseException = (HttpResponseException) exception;
        return Recipes.newResponse(
            HttpResponseStatus.valueOf(responseException.getStatusCode()),
            encoder.encode(request, responseException),
            contentType);
      }
      // TODO (AD): Handle other exceptions that can reasonably be converted to HTTP Responses.
      // For example IllegalArgumentException could become HTTP 400 Bad Request
    } catch (Exception e) {
      log.error("Error Handling Exception:", e);
    }
    return Recipes.newResponse(
        HttpResponseStatus.INTERNAL_SERVER_ERROR,
        Unpooled.wrappedBuffer(XrpcConstants.INTERNAL_SERVER_ERROR_RESPONSE),
        Recipes.ContentType.Text_Plain);
  }
}
