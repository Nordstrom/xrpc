package com.nordstrom.xrpc.server;

import io.netty.handler.codec.http.HttpResponse;

/** A Handler for mapping exceptions to Http Responses. */
@FunctionalInterface
public interface ExceptionHandler {
  /**
   * Handle exception. This is used to handle an exception thrown by the registered Route Handler.
   * It converts the Exception to an appropriate HttpResponse.
   *
   * @param request request for which this Exception was generated
   * @param exception thrown exception
   * @return HttpResponse appropriate for this Exception
   */
  HttpResponse handle(XrpcRequest request, Exception exception);
}
