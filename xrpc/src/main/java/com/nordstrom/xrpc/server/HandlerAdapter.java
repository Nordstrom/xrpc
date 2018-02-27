package com.nordstrom.xrpc.server;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import io.netty.handler.codec.http.HttpResponse;

public class HandlerAdapter implements Handler {
  private final Handler handler;
  private final Meter meter;
  private final Timer timer;

  public HandlerAdapter(Handler handler, Meter meter, Timer timer) {
    this.handler = handler;

    this.meter = meter;
    this.timer = timer;
  }

  public HttpResponse handle(XrpcRequest request) {
    meter.mark();
    try {
      return timer.time(() -> handler.handle(request));
    } catch (Exception e) {
      return request.connectionContext().exceptionHandler().handle(request, e);
    }
  }
}
