package com.xjeffrose.xrpc;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Router {
  private static final int NO_READER_IDLE_TIMEOUT = 200;
  private static final int NO_WRITER_IDLE_TIMEOUT = 500;
  private static final int NO_ALL_IDLE_TIMEOUT = 800;

  private final String workerNameFormat = "xrpc_router-";

  private final int bossThreads;
  private final int workerThreads;
  private final Map<Route, Map<String, String>> var_map = new HashMap<>();
  private final Map<Route, Function<HttpRequest, HttpResponse>> s_routes = new HashMap<>();
  private final Map<Route, BiFunction<HttpRequest, Map<String, String>, HttpResponse>> b_routes = new HashMap<>();

  private Channel channel;
  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  private Class<? extends ServerChannel> channelClass;

  public Router(int bthreads, int wthreads) {
    this.bossThreads = bthreads;
    this.workerThreads = wthreads;
  }

  static private ThreadFactory threadFactory(String nameFormat) {
    return new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
  }

  public void addRoute(String route, BiFunction<HttpRequest, Map<String, String>, HttpResponse> handler) {
    Route r = Route.build(route);
    Map<String, String> m = r.groups(route);
    b_routes.put(r, handler);
    var_map.put(r, m);
  }

  public void addRoute(String route, Function<HttpRequest, HttpResponse> handler) {
    s_routes.put(Route.build(route), handler);
  }

  public void listenAndServe(int port) throws IOException {
    ServerBootstrap b = new ServerBootstrap();
    ConnectionLimiter globalConnectionLimiter = new ConnectionLimiter(5000); // All endpoints for a given service
    URLRouter router = new URLRouter();

    if (Epoll.isAvailable()) {
      bossGroup = new EpollEventLoopGroup(bossThreads, threadFactory(workerNameFormat));
      workerGroup = new EpollEventLoopGroup(workerThreads, threadFactory(workerNameFormat));
      channelClass = EpollServerSocketChannel.class;
    } else {
      bossGroup = new NioEventLoopGroup(bossThreads, threadFactory(workerNameFormat));
      workerGroup = new NioEventLoopGroup(workerThreads, threadFactory(workerNameFormat));
      channelClass = NioServerSocketChannel.class;

      b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
//          .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
//          .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
          .option(ChannelOption.SO_BACKLOG, 128);
//          .option(ChannelOption.TCP_NODELAY, true);
    }

    b.group(bossGroup, workerGroup);
    b.channel(channelClass);
    b.childHandler(new ChannelInitializer<Channel>() {
      @Override
      public void initChannel(Channel ch) throws Exception {
        ChannelPipeline cp = ch.pipeline();
        cp.addLast("globalConnectionLimiter", globalConnectionLimiter); // For all endpoints
        cp.addLast("serviceConnectionLimiter", new ConnectionLimiter(200)); // for any endpoint
        cp.addLast("encryptionHandler", new TLS().getEncryptionHandler()); // Add Config for Certs
        cp.addLast("messageLogger", new MessageLogger());
        cp.addLast("codec", new HttpServerCodec());
//        cp.addLast("aggregator", new NoOpHandler()); // Not Needed but maybe keep in here?
        cp.addLast("authHandler", new NoOpHandler()); // OAuth2.0 Impl needed
        cp.addLast("routingFilter", router);
        cp.addLast("idleDisconnectHandler", new IdleDisconnectHandler(
            NO_READER_IDLE_TIMEOUT,
            NO_WRITER_IDLE_TIMEOUT,
            NO_ALL_IDLE_TIMEOUT));
        cp.addLast("exceptionLogger", new ExceptionLogger());

      }

    });

    ChannelFuture future = b.bind(new InetSocketAddress(port));

    try {
      future.await();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted waiting for bind");
    }

    if (!future.isSuccess()) {
      throw new IOException("Failed to bind", future.cause());
    }

    channel = future.channel();
  }

  public void shutdown() {
    if (channel == null || !channel.isOpen()) {
      return;
    }

    channel.close().addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
          //log.log(Level.WARNING, "Error shutting down server", future.cause());
        }
        synchronized (Router.this) {
          // listener.serverShutdown();
        }
      }
    });
  }

  @ChannelHandler.Sharable
  private class URLRouter extends ChannelDuplexHandler {

    public URLRouter() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof HttpRequest) {
        String uri = ((HttpRequest) msg).uri();

        for (Route route : s_routes.keySet()) {
          if (route.matches(uri)) {
            ctx.writeAndFlush(s_routes.get(route).apply((HttpRequest) msg)).addListener(ChannelFutureListener.CLOSE);;
            ctx.fireChannelRead(msg);
            return;
          }
        }

        for (Route route : b_routes.keySet()) {
          if (route.matches(uri)) {
            ctx.writeAndFlush(b_routes.get(route).apply((HttpRequest) msg, var_map.get(route))).addListener(ChannelFutureListener.CLOSE);;
            ctx.fireChannelRead(msg);
            return;
          }
        }

        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND));
      }
      ctx.fireChannelRead(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
      ctx.fireChannelReadComplete();
    }
  }


  @ChannelHandler.Sharable
  private static class ConnectionLimiter extends ChannelDuplexHandler {

    private final AtomicInteger numConnections;
    private final int maxConnections;

    public ConnectionLimiter(int maxConnections) {
      this.maxConnections = maxConnections;
      this.numConnections = new AtomicInteger(0);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      if (maxConnections > 0) {
        if (numConnections.incrementAndGet() > maxConnections) {
          ctx.channel().close();
          // numConnections will be decremented in channelClosed
          log.info("Accepted connection above limit (" + maxConnections + "). Dropping.");
        }
      }
      ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      if (maxConnections > 0) {
        if (numConnections.decrementAndGet() < 0) {
          log.error("BUG in ConnectionLimiter");
        }
      }
      ctx.fireChannelInactive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      ctx.fireChannelRead(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
      ctx.fireChannelReadComplete();
    }
  }

  @ChannelHandler.Sharable
  class NoOpHandler extends ChannelDuplexHandler {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      ctx.pipeline().remove(this);
      ctx.fireChannelActive();
    }
  }

  class IdleDisconnectHandler extends IdleStateHandler {

    public IdleDisconnectHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds,
        int allIdleTimeSeconds) {
      super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
      ctx.channel().close();
    }
  }


}