package com.nordstrom.xrpc.server;

import com.codahale.metrics.MetricRegistry;
import com.nordstrom.xrpc.XConfig;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class JmhServiceRateLimiterBenchmark {

  MetricRegistry metricRegistry = new MetricRegistry();
  XConfig config = new XConfig();
  XrpcConnectionContext ctx = XrpcConnectionContext.builder().build();
  EmbeddedChannel embeddedChannel = new CustomEmbeddedChannel();
  ChannelPipeline cp = embeddedChannel.pipeline();
  ChannelHandler handler = new ServiceRateLimiter(metricRegistry, config, ctx);
  ChannelHandlerContext channelHandlerContext;

  @Setup
  public void setupChannelPipeline() {
    cp.addFirst(handler);
    channelHandlerContext = cp.context(handler);
  }

  @Benchmark
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  @BenchmarkMode({Mode.AverageTime, Mode.Throughput})
  public void basicServiceRateLimiter() {
    cp.fireChannelActive();
  }

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
}
