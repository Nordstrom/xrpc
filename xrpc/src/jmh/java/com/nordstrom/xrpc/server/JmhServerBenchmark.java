package com.nordstrom.xrpc.server;

import com.codahale.metrics.MetricRegistry;
import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.server.tls.Tls;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;

@org.openjdk.jmh.annotations.State(Scope.Thread)
public class JmhServerBenchmark {

  EmbeddedChannel embeddedChannel = new CustomEmbeddedChannel();
  ChannelPipeline cp;
  ChannelHandlerContext channelHandlerContext;

  MetricRegistry metricRegistry = new MetricRegistry();
  XConfig config = new XConfig();
  ServerContext ctx = ServerContext.builder().build();

  private final com.nordstrom.xrpc.server.State state =
      com.nordstrom.xrpc.server.State.builder()
          .config(config)
          .globalConnectionLimiter(new ConnectionLimiter(metricRegistry, config.maxConnections()))
          .rateLimiter(new ServiceRateLimiter(metricRegistry, config, ctx))
          .whiteListFilter(new WhiteListFilter(metricRegistry, config.ipWhiteList()))
          .blackListFilter(new BlackListFilter(metricRegistry, config.ipBlackList()))
          .firewall(new Firewall(metricRegistry))
          .tls(new Tls(config.cert(), config.key()))
          .h1h2(
              new Http2OrHttpHandler(
                  new UrlRouter(), ctx, config.corsConfig(), config.maxPayloadBytes()))
          .build();

  ServerChannelInitializer serverChannelInitializer = new ServerChannelInitializer(state);

  @Setup
  public void setupServer() throws Exception {
    serverChannelInitializer.initChannel(embeddedChannel);
    cp = embeddedChannel.pipeline();
  }

  @BenchmarkMode({Mode.AverageTime, Mode.Throughput})
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  @Benchmark
  public void benchmark() {
    cp.fireChannelActive();
  }
}
