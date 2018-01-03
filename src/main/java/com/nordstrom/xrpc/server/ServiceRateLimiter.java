package com.nordstrom.xrpc.server;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.RateLimiter;
import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.XrpcConstants;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.PlatformDependent;
import java.net.InetSocketAddress;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
class ServiceRateLimiter extends ChannelDuplexHandler {
  private final Map<String, RateLimiter> softLimiterMap = PlatformDependent.newConcurrentHashMap();
  private final Map<String, RateLimiter> hardLimiterMap = PlatformDependent.newConcurrentHashMap();
  private final RateLimiter softLimiter;
  private final RateLimiter hardLimiter;
  private final Meter reqs;
  private final Timer timer;
  private final XConfig config;

  private Timer.Context context;

  public ServiceRateLimiter(MetricRegistry metrics, XConfig config) {
    this.softLimiter = RateLimiter.create(config.globalHardReqPerSec());
    this.hardLimiter = RateLimiter.create(config.globalSoftReqPerSec());
    this.reqs = metrics.meter(name(Router.class, "requests", "Rate"));
    this.timer = metrics.timer("Request Latency");
    this.config = config;
  }

  protected void clean() {
    softLimiterMap.clear();
    hardLimiterMap.clear();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    reqs.mark();

    // Rate Limit per server
    String remoteAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostName();

    if (hardLimiterMap.containsKey(remoteAddress)) {
      if (!hardLimiterMap.get(remoteAddress).tryAcquire()) {
        log.debug("Hard Rate limit fired for " + remoteAddress);
        ctx.channel().attr(XrpcConstants.XRPC_HARD_RATE_LIMIT).set(Boolean.TRUE);
      } else if (!softLimiterMap.get(remoteAddress).tryAcquire()) {
        ctx.channel().attr(XrpcConstants.XRPC_SOFT_RATE_LIMIT).set(Boolean.TRUE);
      }

    } else {
      // TODO(JR): You should be able to override per server
      hardLimiterMap.put(remoteAddress, RateLimiter.create(config.hardReqPerSec()));
      softLimiterMap.put(remoteAddress, RateLimiter.create(config.softReqPerSec()));
    }

    // Global Rate Limiter
    if (!hardLimiter.tryAcquire()) {
      log.debug("Global Hard Rate limit fired");
      ctx.channel().attr(XrpcConstants.XRPC_HARD_RATE_LIMIT).set(Boolean.TRUE);

    } else if (!softLimiter.tryAcquire()) {
      ctx.channel().attr(XrpcConstants.XRPC_SOFT_RATE_LIMIT).set(Boolean.TRUE);
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
