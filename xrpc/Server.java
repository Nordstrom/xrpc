package xrpc;

import static io.netty.channel.ChannelOption.SO_BACKLOG;
import static io.netty.channel.ChannelOption.SO_KEEPALIVE;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ReferenceCounted;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import io.netty.channel.ChannelHandler;

import java.util.HashMap;
import java.util.Map;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ThreadFactory;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Server {
    private final String workerNameFormat = "test-";

    private int bossThreads;
    private int workerThreads;
    private Map<String, ChannelHandler> routeMap = new HashMap<>();

    private Channel channel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Class<? extends ServerChannel> channelClass;

    public Server(int bthreads, int wthreads) {
	this.bossThreads = bthreads;
	this.workerThreads = wthreads;
    }

    static private ThreadFactory threadFactory(String nameFormat) {
	return new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
    }

    public void addRoute(String route, ChannelHandler handler) {

    }

    public void start(int port) throws IOException {
	ServerBootstrap b = new ServerBootstrap();

	if (Epoll.isAvailable()) {
	    bossGroup = new EpollEventLoopGroup(bossThreads, threadFactory(workerNameFormat));
	    workerGroup = new EpollEventLoopGroup(workerThreads, threadFactory(workerNameFormat));
	    channelClass = EpollServerSocketChannel.class;
	} else {
	    bossGroup = new NioEventLoopGroup(bossThreads, threadFactory(workerNameFormat));
	    workerGroup = new NioEventLoopGroup(workerThreads, threadFactory(workerNameFormat));
	    channelClass = NioServerSocketChannel.class;

	    b.option(SO_BACKLOG, 128);
	    b.childOption(SO_KEEPALIVE, true);

	}

	b.group(bossGroup, workerGroup);
	b.channel(channelClass);
	b.childHandler(new ChannelInitializer<Channel>() {
		@Override
		public void initChannel(Channel ch) throws Exception {
		    ChannelPipeline p = ch.pipeline();
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

}

@ChannelHandler.Sharable
class NoOpHandler extends ChannelDuplexHandler {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
	ctx.pipeline().remove(this);
	ctx.fireChannelActive();
    }
}
