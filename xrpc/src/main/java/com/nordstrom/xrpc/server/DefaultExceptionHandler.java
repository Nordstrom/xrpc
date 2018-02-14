package com.nordstrom.xrpc.server;

import io.netty.handler.codec.http.HttpResponse;

public class DefaultExceptionHandler implements ExceptionHandler {
  @Override
  public HttpResponse handle(XrpcRequest request, Exception exception) throws Exception {
    // TODO (AD): Use configured response content/type and response specific exceptions to default
    // to meaningful responses here
    throw exception;
  }
}
