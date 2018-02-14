package com.nordstrom.xrpc.server;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

public class HandlerAdapter {
  Handler adaptHandler(Handler handler, Meter meter, Timer timer) {
    return request -> {
      meter.mark();
      // TODO(jkinkead): Add a timer here per https://github.com/Nordstrom/xrpc/issues/121
      return timer.time(() -> handler.handle(request));
    };
  }
}
