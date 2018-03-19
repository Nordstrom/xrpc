package com.nordstrom.xrpc.server;

import io.netty.channel.embedded.EmbeddedChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

class CustomEmbeddedChannel extends EmbeddedChannel {

  private InetSocketAddress socketAddress;

  public CustomEmbeddedChannel() {
    super();
    socketAddress = new InetSocketAddress("localhost", 0);
  }

  @Override
  protected SocketAddress remoteAddress0() {
    return this.socketAddress;
  }
}
