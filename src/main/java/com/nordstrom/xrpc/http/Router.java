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

package com.nordstrom.xrpc.http;

import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.tls.TLS;
import com.nordstrom.xrpc.logging.MessageLogger;
import com.nordstrom.xrpc.logging.ExceptionLogger;

import static com.codahale.metrics.MetricRegistry.name;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.channel.ChannelOption.SO_BACKLOG;
import static io.netty.channel.ChannelOption.SO_KEEPALIVE;
import static io.netty.channel.ChannelOption.TCP_NODELAY;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.google.common.util.concurrent.RateLimiter;
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
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

@Slf4j
public class Router {
        private final XConfig config = new XConfig();

        private final int NO_READER_IDLE_TIMEOUT = config.readerIdleTimeout();
        private final int NO_WRITER_IDLE_TIMEOUT = config.writerIdleTimeout();;
        private final int NO_ALL_IDLE_TIMEOUT = config.requestIdleTimeout();

        private final String workerNameFormat = config.workerNameFormat();

        private final int bossThreads = config.bossThreads();
        private final int workerThreads = config.workerThreads();
        private final Map<Route, Function<HttpRequest, HttpResponse>> s_routes = new HashMap<>();
        private final Map<Route, BiFunction<HttpRequest, Route, HttpResponse>> b_routes = new HashMap<>();

        private Channel channel;
        private EventLoopGroup bossGroup;
        private EventLoopGroup workerGroup;
        private Class<? extends ServerChannel> channelClass;

        // see http://metrics.dropwizard.io/3.2.2/getting-started.html for more on this
        private final MetricRegistry metrics = new MetricRegistry();
        private final Meter requests = metrics.meter("requests");
        private final Histogram responseSizes = metrics.histogram(name(URLRouter.class, "responses"));

        private final ConsoleReporter consoleReporter = ConsoleReporter.forRegistry(metrics)
                        .convertRatesTo(TimeUnit.SECONDS)
                        .convertDurationsTo(TimeUnit.MILLISECONDS)
                        .build();

        final Slf4jReporter slf4jReporter = Slf4jReporter.forRegistry(metrics)
                        .outputTo(LoggerFactory.getLogger(Router.class))
                        .convertRatesTo(TimeUnit.SECONDS)
                        .convertDurationsTo(TimeUnit.MILLISECONDS)
                        .build();

        final JmxReporter jmxReporter = JmxReporter.forRegistry(metrics).build();

  private final TLS tls =  new TLS(config.cert(), config.key());

        static private ThreadFactory threadFactory(String nameFormat) {
                return new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
        }

        public void addRoute(String route, BiFunction<HttpRequest, Route, HttpResponse> handler) {
                Route r = Route.build(route);
                b_routes.put(r, handler);
        }

        public void addRoute(String route, Function<HttpRequest, HttpResponse> handler) {
                s_routes.put(Route.build(route), handler);
        }

        public MetricRegistry getMetrics() {

                return metrics;
        }

        public void listenAndServe() throws IOException {
                ConnectionLimiter globalConnectionLimiter = new ConnectionLimiter(metrics, config.maxConnections()); // All endpoints for a given service
                ServiceRateLimiter rateLimiter = new ServiceRateLimiter(metrics, config.rateLimit()); // RateLimit incomming connections in terms of req / second

                ServerBootstrap b = new ServerBootstrap();
                URLRouter router = new URLRouter();

                if (Epoll.isAvailable()) {
                        bossGroup = new EpollEventLoopGroup(bossThreads, threadFactory(workerNameFormat));
                        workerGroup = new EpollEventLoopGroup(workerThreads, threadFactory(workerNameFormat));
                        channelClass = EpollServerSocketChannel.class;
                } else {
                        bossGroup = new NioEventLoopGroup(bossThreads, threadFactory(workerNameFormat));
                        workerGroup = new NioEventLoopGroup(workerThreads, threadFactory(workerNameFormat));
                        channelClass = NioServerSocketChannel.class;

                        b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
                        b.option(SO_BACKLOG, 128);
                        b.childOption(SO_KEEPALIVE, true);
                        b.childOption(TCP_NODELAY, true);
                }

                b.group(bossGroup, workerGroup);
                b.channel(channelClass);
                b.childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        public void initChannel(Channel ch) throws Exception {
                                ChannelPipeline cp = ch.pipeline();
                                cp.addLast("serverConnectionLimiter", globalConnectionLimiter);
                                cp.addLast("serverRateLimiter", rateLimiter);
                                cp.addLast("encryptionHandler", tls.getEncryptionHandler()); // Add Config for Certs
                                cp.addLast("messageLogger", new MessageLogger());
                                cp.addLast("codec", new HttpServerCodec());
                                cp.addLast("aggregator", new HttpObjectAggregator(1*1024*1024)); // Aggregate up to 1MB
                                //        cp.addLast("aggregator", new NoOpHandler()); // Not Needed but maybe keep in here?
                                //        cp.addLast("authHandler", new NoOpHandler()); // OAuth2.0 Impl needed
                                cp.addLast("routingFilter", router);
                                cp.addLast("idleDisconnectHandler", new IdleDisconnectHandler(
                                                NO_READER_IDLE_TIMEOUT,
                                                NO_WRITER_IDLE_TIMEOUT,
                                                NO_ALL_IDLE_TIMEOUT));
                                cp.addLast("exceptionLogger", new ExceptionLogger());

                        }

                });

                ChannelFuture future = b.bind(new InetSocketAddress(config.port()));

                try {
                        // Get some loggy logs
                        consoleReporter.start(30, TimeUnit.SECONDS);
                        slf4jReporter.start(30, TimeUnit.SECONDS);
                        jmxReporter.start();

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

                @Override
                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        requests.mark();
                }

                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        if (msg instanceof HttpRequest) {
                                String uri = ((HttpRequest) msg).uri();

                                for (Route route : s_routes.keySet()) {
                                        if (route.matches(uri)) {
                                                FullHttpResponse resp = (FullHttpResponse) s_routes.get(route).apply((HttpRequest) msg);
                                                responseSizes.update(resp.content().readableBytes());
                                                ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
                                                ctx.fireChannelRead(msg);
                                                return;
                                        }
                                }

                                for (Route route : b_routes.keySet()) {
                                        if (route.matches(uri)) {
                                                FullHttpResponse resp = (FullHttpResponse) b_routes.get(route).apply((HttpRequest) msg, route);
                                                responseSizes.update(resp.content().readableBytes());
                                                ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
                                                ctx.fireChannelRead(msg);
                                                return;
                                        }
                                }

                                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
                                response.headers().set(CONTENT_TYPE, "text/plain");
                                response.headers().setInt(CONTENT_LENGTH, 0);
                                ctx.writeAndFlush(response)
                                        .addListener(ChannelFutureListener.CLOSE);
                        }
                        ctx.fireChannelRead(msg);
                }

                @Override
                public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                        ctx.fireChannelReadComplete();
                }
        }

        @ChannelHandler.Sharable
        private static class ServiceRateLimiter extends ChannelDuplexHandler {
                private final RateLimiter limiter;
                private final Meter reqs;

                public ServiceRateLimiter(MetricRegistry metrics, float rateLimit) {
                        this.limiter = RateLimiter.create(rateLimit);
                        this.reqs = metrics.meter(name(Router.class, "requests", "Rate"));
                }


                @Override
                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        //TODO(JR): Should this be before or after the acquire? Do we want to know when
                        //          we are limiting? Do we want to know what the actual rate of incoming
                        //          requests are?
                        reqs.mark();
                        limiter.acquire();

                        ctx.fireChannelActive();
                }

                @Override
                public void channelInactive(ChannelHandlerContext ctx) throws Exception {

                        ctx.fireChannelInactive();
                }

        }

        @ChannelHandler.Sharable
        private static class ConnectionLimiter extends ChannelDuplexHandler {
                private final AtomicInteger numConnections;
                private final int maxConnections;
                private final Counter connections;

                public ConnectionLimiter(MetricRegistry metrics, int maxConnections) {
                        this.maxConnections = maxConnections;
                        this.numConnections = new AtomicInteger(0);
                        this.connections = metrics.counter(name(Router.class, "Active Connections"));
                }

                @Override
                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        connections.inc();

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
                        connections.dec();

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
