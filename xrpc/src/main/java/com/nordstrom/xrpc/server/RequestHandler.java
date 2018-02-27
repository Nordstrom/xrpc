package com.nordstrom.xrpc.server;

import io.netty.handler.codec.http.HttpResponse;
import java.io.IOException;

/** A handler for an HTTP route. */
@FunctionalInterface
public interface RequestHandler extends Handler {
  HttpResponse handle(XrpcRequest request) throws IOException;
}
