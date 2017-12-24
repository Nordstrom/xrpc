package com.nordstrom.xrpc.server;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.RateLimiter;
import com.nordstrom.xrpc.XrpcConstants;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class ServiceRateLimiter extends ChannelDuplexHandler {
  private static Timer.Context context;
  private final RateLimiter softLimiter;
  private final RateLimiter hardLimiter;
  private final Meter reqs;
  private final Timer timer;

  public ServiceRateLimiter(MetricRegistry metrics, double softRateLimit, double hardRateLimit) {
    this.softLimiter = RateLimiter.create(softRateLimit);
    this.hardLimiter = RateLimiter.create(hardRateLimit);
    this.reqs = metrics.meter(name(Router.class, "requests", "Rate"));
    this.timer = metrics.timer("Request Latency");
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    reqs.mark();

    hardLimiter.acquire();

    if (!softLimiter.tryAcquire()) {
      ctx.channel().attr(XrpcConstants.XRPC_RATE_LIMIT).set(true);
    }

    context = timer.time();
    ctx.fireChannelActive();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    context.stop();

    ctx.fireChannelInactive();
  }
}
