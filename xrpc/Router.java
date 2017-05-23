package xrpc;

import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ReferenceCounted;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.HashMap;
import java.util.Map;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ThreadFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger

public class Router {
    private final String workerNameFormat = "xrpc_router-";

    private int bossThreads;
    private int workerThreads;
    private Map<String, ChannelHandler> routeMap = new HashMap<>();

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

    public void addRoute(String route, ChannelHandler handler) {
	routeMap.put(route, handler);
    }

    public void listenAndServe(int port) throws IOException {
	ServerBootstrap b = new ServerBootstrap();

	if (Epoll.isAvailable()) {
	    bossGroup = new EpollEventLoopGroup(bossThreads, threadFactory(workerNameFormat));
	    workerGroup = new EpollEventLoopGroup(workerThreads, threadFactory(workerNameFormat));
	    channelClass = EpollServerSocketChannel.class;
	} else {
	    bossGroup = new NioEventLoopGroup(bossThreads, threadFactory(workerNameFormat));
	    workerGroup = new NioEventLoopGroup(workerThreads, threadFactory(workerNameFormat));
	    channelClass = NioServerSocketChannel.class;

	    b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
		.option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
		.option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
		.option(ChannelOption.SO_BACKLOG, 128)
		.option(ChannelOption.TCP_NODELAY, true);

	//b.option(SO_BACKLOG, 128);
	    //   b.childOption(SO_KEEPALIVE, true);

	}

	b.group(bossGroup, workerGroup);
	b.channel(channelClass);
	b.childHandler(new ChannelInitializer<Channel>() {
		@Override
		public void initChannel(Channel ch) throws Exception {
		    ChannelPipeline cp = ch.pipeline();


		    cp.addLast("connectionContext", new ConnectionContextHandler());
		    cp.addLast("globalConnectionLimiter", connectionLimiter);
		    cp.addLast("serviceConnectionLimiter", new ConnectionLimiter(def.getMaxConnections()));
		    cp.addLast(ChannelStatistics.NAME, channelStatistics);
		    cp.addLast("encryptionHandler", securityHandlers.getEncryptionHandler());
		    cp.addLast("messageLogger", new XioMessageLogger());
		    cp.addLast("codec", def.getCodecFactory().getCodec());
		    cp.addLast("aggregator", def.getAggregatorFactory().getAggregator());
		    cp.addLast("routingFilter", def.getRoutingFilterFactory().getRoutingFilter());
		    if (def.getClientIdleTimeout() != null) {
			cp.addLast("idleDisconnectHandler", new XioIdleDisconnectHandler(
							    (int) def.getClientIdleTimeout().toMillis(),
							    NO_WRITER_IDLE_TIMEOUT,
							    NO_ALL_IDLE_TIMEOUT,
							    TimeUnit.MILLISECONDS));
		    }
		    cp.addLast("authHandler", securityHandlers.getAuthenticationHandler());
		    //        cp.addLast("dispatcher", new XioDispatcher(def, xioServerConfig));
		    cp.addLast("exceptionLogger", new XioExceptionLogger());

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
		    synchronized (Server.this) {
			// listener.serverShutdown();
		    }
		}
	    });
    }



    @ChannelHandler.Sharable
	class NoOpHandler extends ChannelDuplexHandler {
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
	    ctx.pipeline().remove(this);
	    ctx.fireChannelActive();
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




    class IdleDisconnectHandler extends IdleStateHandler {
	public IdleDisconnectHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
	    super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
	}

	@Override
	protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
	    ctx.channel().close();
	}
    }


}
