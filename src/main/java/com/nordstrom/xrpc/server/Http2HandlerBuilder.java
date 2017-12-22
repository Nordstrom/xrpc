package com.nordstrom.xrpc.server;

import io.netty.handler.codec.http2.*;
import io.netty.handler.logging.LogLevel;

public final class Http2HandlerBuilder
    extends AbstractHttp2ConnectionHandlerBuilder<Http2Handler, Http2HandlerBuilder> {

  private final Http2FrameLogger logger = new Http2FrameLogger(LogLevel.INFO, Http2Handler.class);

  public Http2HandlerBuilder(XrpcConnectionContext xctx) {
    frameLogger(logger);
  }

  @Override
  public Http2Handler build() {
    return super.build();
  }

  @Override
  protected Http2Handler build(
      Http2ConnectionDecoder decoder,
      Http2ConnectionEncoder encoder,
      Http2Settings initialSettings) {
    Http2Handler handler = new Http2Handler(decoder, encoder, initialSettings);
    frameListener(handler);
    return handler;
  }
}
