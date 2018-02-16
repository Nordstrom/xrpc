package com.nordstrom.xrpc.server;

import com.nordstrom.xrpc.XrpcConstants;
import com.nordstrom.xrpc.server.http.Recipes;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultExceptionHandler implements ExceptionHandler {
  @Override
  public HttpResponse handle(XrpcRequest request, Exception exception) {
    // TODO (AD): Use configured response content/type and response specific exceptions to default
    // to meaningful responses here
    log.error("Handler Exception:", exception);
    return Recipes.newResponse(
        HttpResponseStatus.INTERNAL_SERVER_ERROR,
        Unpooled.wrappedBuffer(XrpcConstants.INTERNAL_SERVER_ERROR_RESPONSE),
        Recipes.ContentType.Text_Plain);
  }
}
