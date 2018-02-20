package com.nordstrom.xrpc.server;

public class HandlerAdapter {

  public  Handler adaptedHandler =
    request -> {
      meter.mark();
      try {
        return timer.time(() -> userHandler.handle(request));
      } catch (Exception e) {
        return request.getConnectionContext().getExceptionHandler().handle(request, e);
      }
    };
        handlers.put(method, adaptedHandler);
}
