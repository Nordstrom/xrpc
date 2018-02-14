package com.nordstrom.xrpc.server;

import io.netty.handler.codec.http.HttpResponse;

/** A Handler for mapping exceptions to Http Responses. */
@FunctionalInterface
public interface ExceptionHandler {
  /**
   * Handle exception. This is used to handle an exception thrown by the registered Route Handler.
   * It converts the Exception to an appropriate HttpResponse.
   *
   * @param request Request for which this Exception was generated
   * @param exception Thrown exception
   * @return HttpResponse appropriate for this Exception.
   * @throws Exception Exception for any unhandled Exception passed in or during the conversion
   *     process. This should be avoided if possible.
   */
  HttpResponse handle(XrpcRequest request, Exception exception) throws Exception;
}
