package com.nordstrom.xrpc.server;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.RateLimiter;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

@ChannelHandler.Sharable
public class ServiceRateLimiter extends ChannelDuplexHandler {
  private static Timer.Context context;
  private final RateLimiter limiter;
  private final Meter reqs;
  private final Timer timer;

  public ServiceRateLimiter(MetricRegistry metrics, double rateLimit) {
    this.limiter = RateLimiter.create(rateLimit);
    this.reqs = metrics.meter(name(Router.class, "requests", "Rate"));
    this.timer = metrics.timer("Request Latency");
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    // TODO(JR): Should this be before or after the acquire? Do we want to know when
    //          we are limiting? Do we want to know what the actual rate of incoming
    //          requests are?
    reqs.mark();
    limiter.acquire();
    context = timer.time();

    ctx.fireChannelActive();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    context.stop();

    ctx.fireChannelInactive();
  }
}
