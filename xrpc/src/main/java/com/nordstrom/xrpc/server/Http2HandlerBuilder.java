/*
 * Copyright 2018 Nordstrom, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nordstrom.xrpc.server;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Http2HandlerBuilder
    extends AbstractHttp2ConnectionHandlerBuilder<
        Http2HandlerBuilder.ConnectionHandler, Http2HandlerBuilder> {

  private static final String FRAME_LOGGER_NAME = Http2HandlerBuilder.class.getName() + ".frames";
  /** Logger to query for configuration for HTTP2 frame logging. */
  private static final Logger FRAME_LOGGER = LoggerFactory.getLogger(FRAME_LOGGER_NAME);

  public Http2HandlerBuilder() {
    if (FRAME_LOGGER.isDebugEnabled()) {
      frameLogger(new Http2FrameLogger(LogLevel.DEBUG, FRAME_LOGGER_NAME));
    }
  }

  @Override
  public ConnectionHandler build() {
    return super.build();
  }

  @Override
  protected ConnectionHandler build(
      Http2ConnectionDecoder decoder,
      Http2ConnectionEncoder encoder,
      Http2Settings initialSettings) {

    decoder.frameListener(new Http2Handler(encoder));

    ConnectionHandler handler = new ConnectionHandler(decoder, encoder, initialSettings);
    return handler;
  }

  /** Trivial extension of Http2ConnectionHandler to expose a public constructor. */
  public static class ConnectionHandler extends Http2ConnectionHandler {
    ConnectionHandler(
        Http2ConnectionDecoder decoder,
        Http2ConnectionEncoder encoder,
        Http2Settings initialSettings) {
      super(decoder, encoder, initialSettings);
    }
  }
}
