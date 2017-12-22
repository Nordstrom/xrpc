package com.nordstrom.xrpc.server;

import static io.netty.channel.ChannelOption.SO_KEEPALIVE;
import static io.netty.channel.ChannelOption.TCP_NODELAY;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.util.concurrent.ThreadFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XrpcBootstrapFactory {

  private XrpcBootstrapFactory() {};

  private static ThreadFactory threadFactory(String nameFormat) {
    return new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
  }

  public static ServerBootstrap buildBootstrap(
      int bossThreadCount, int workerThreadCount, String workerNameFormat) {
    ServerBootstrap b = new ServerBootstrap();

    EventLoopGroup bossGroup;
    EventLoopGroup workerGroup;
    Class<? extends ServerChannel> channelClass;

    if (Epoll.isAvailable()) {
      log.info("Using Epoll");
      bossGroup = new EpollEventLoopGroup(bossThreadCount, threadFactory(workerNameFormat));
      workerGroup = new EpollEventLoopGroup(workerThreadCount, threadFactory(workerNameFormat));
      channelClass = EpollServerSocketChannel.class;
    } else if (KQueue.isAvailable()) {
      log.info("Using KQueue");
      bossGroup = new KQueueEventLoopGroup(bossThreadCount, threadFactory(workerNameFormat));
      workerGroup = new KQueueEventLoopGroup(workerThreadCount, threadFactory(workerNameFormat));
      channelClass = KQueueServerSocketChannel.class;
      b.option(EpollChannelOption.SO_REUSEPORT, true);
    } else {
      log.info("Using NIO");
      bossGroup = new NioEventLoopGroup(bossThreadCount, threadFactory(workerNameFormat));
      workerGroup = new NioEventLoopGroup(workerThreadCount, threadFactory(workerNameFormat));
      channelClass = NioServerSocketChannel.class;
    }

    b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    b.option(ChannelOption.SO_BACKLOG, 8192);
    b.option(ChannelOption.SO_REUSEADDR, true);

    b.childOption(ChannelOption.SO_REUSEADDR, true);
    b.childOption(SO_KEEPALIVE, true);
    b.childOption(TCP_NODELAY, true);

    b.group(bossGroup, workerGroup);
    b.channel(channelClass);

    return b;
  }
}
