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
package com.nordstrom.xrpc.client;

/*
 * Copyright 2017 Nordstrom, Inc.
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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.util.concurrent.ThreadFactory;
import javax.net.ssl.SSLException;
import lombok.Getter;

public class XrpcClient {
  private static final int MAX_PAYLOAD_SIZE = 1 * 1024 * 1024;

  @Getter private final Bootstrap bootstrap;
  private final SslContext sslCtx;
  // TODO(JR): These should be configurable
  private static final String workerNameFormat = "xrpc-client-%d";
  private static final int workerThreadCount = 4;

  private EventLoopGroup workerGroup;
  private Class<? extends SocketChannel> channelClass;

  public XrpcClient() {
    this.sslCtx = buildSslCtx();
    this.bootstrap = buildBootstrap();
  }

  public XrpcClient(EventLoopGroup elg) {
    this.workerGroup = elg;
    this.sslCtx = buildSslCtx();
    this.bootstrap = buildBootstrap();
  }

  private static ThreadFactory threadFactory(String nameFormat) {
    return new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
  }

  public Call newCall(String uri) {
    return new Call(this, uri);
  }

  private SslContext buildSslCtx() {
    SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
    try {
      return SslContextBuilder.forClient()
          .sslProvider(provider)
          .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
          .trustManager(InsecureTrustManagerFactory.INSTANCE)
          // TODO(JR): Make a seperate Handler Class for http2 as opposed to autoneg
          //        .applicationProtocolConfig(new ApplicationProtocolConfig(
          //          ApplicationProtocolConfig.Protocol.ALPN,
          //          // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK
          //             providers.
          //          ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
          //          // ACCEPT is currently the only mode supported by both OpenSsl and JDK
          //             providers.
          //          ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
          //          ApplicationProtocolNames.HTTP_2,
          //          ApplicationProtocolNames.HTTP_1_1))
          .build();
    } catch (SSLException e) {
      e.printStackTrace();
    }

    return null;
  }

  private Bootstrap buildBootstrap() {
    Bootstrap b = new Bootstrap();
    if (workerGroup == null) {
      if (Epoll.isAvailable()) {
        workerGroup = new EpollEventLoopGroup(workerThreadCount, threadFactory(workerNameFormat));
        channelClass = EpollSocketChannel.class;
      } else if (KQueue.isAvailable()) {
        workerGroup = new KQueueEventLoopGroup(workerThreadCount, threadFactory(workerNameFormat));
        channelClass = KQueueSocketChannel.class;
      } else {
        workerGroup = new NioEventLoopGroup(workerThreadCount, threadFactory(workerNameFormat));
        channelClass = NioSocketChannel.class;
      }
    }

    b.group(workerGroup)
        .channel(channelClass)
        .option(
            ChannelOption.CONNECT_TIMEOUT_MILLIS,
            500) // TODO(JR): This timeout value should be configurable
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.TCP_NODELAY, true)
        .handler(
            new ChannelInitializer<SocketChannel>() {
              @Override
              protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline cp = ch.pipeline();
                cp.addLast("tls", sslCtx.newHandler(ch.alloc()));
                // cp.addLast("protocolNeg", new Http2OrHttpHandler());
                cp.addLast("codec", new HttpClientCodec());
                cp.addLast("aggregator", new HttpObjectAggregator(MAX_PAYLOAD_SIZE));
                cp.addLast("responseHandler", new HttpResponseHandler());
              }
            });

    return b;
  }
}
