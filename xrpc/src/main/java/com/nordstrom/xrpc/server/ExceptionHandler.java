package com.nordstrom.xrpc.server;

import io.netty.handler.codec.http.HttpResponse;

/** A Handler for mapping exceptions to Http Responses. */
@FunctionalInterface
public interface ExceptionHandler {
  HttpResponse handle(XrpcRequest request, Exception exception) throws Exception;
}
