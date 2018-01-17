package com.nordstrom.xrpc.server;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.hash.Funnels;
import com.google.common.util.concurrent.RateLimiter;
import com.nordstrom.xrpc.RendezvousHash;
import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.XrpcConstants;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.PlatformDependent;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
class ServiceRateLimiter extends ChannelDuplexHandler {
  private final Map<String, RateLimiter> softLimiterMap = PlatformDependent.newConcurrentHashMap();
  private final Map<String, RateLimiter> hardLimiterMap = PlatformDependent.newConcurrentHashMap();
  private final Meter reqs;
  private final Timer timer;
  private final XConfig config;
  private final RendezvousHash softRateLimitHasher;
  private final RendezvousHash hardRateLimitHasher;
  private final RateLimiter globalHardLimiter;
  private final RateLimiter globalSoftLimiter;

  private Map<ChannelHandlerContext, Timer.Context> timerMap =
      PlatformDependent.newConcurrentHashMap();

  public ServiceRateLimiter(MetricRegistry metrics, XConfig config) {
    this.reqs = metrics.meter(name(Router.class, "requests", "Rate"));
    this.timer = metrics.timer("Request Latency");
    this.config = config;
    this.globalHardLimiter = RateLimiter.create(config.globalHardReqPerSec());
    this.globalSoftLimiter = RateLimiter.create(config.globalSoftReqPerSec());

    softRateLimitHasher =
        buildHasher(softLimiterMap, config.getRateLimiterPoolSize(), config.softReqPerSec());
    hardRateLimitHasher =
        buildHasher(hardLimiterMap, config.getRateLimiterPoolSize(), config.hardReqPerSec());
  }

  private RendezvousHash buildHasher(
      Map<String, RateLimiter> limiterMap, int poolSize, double rate) {
    List<String> _tempPool = new ArrayList<>();

    for (int i = 0; i < poolSize; i++) {
      String id = UUID.randomUUID().toString();
      _tempPool.add(id);
      limiterMap.put(id, RateLimiter.create(rate));
    }

    return new RendezvousHash(Funnels.stringFunnel(XrpcConstants.DEFAULT_CHARSET), _tempPool);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    reqs.mark();

    // Rate Limit per server
    String remoteAddress =
        ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

    if (config.getClientRateLimitOverride().containsKey(remoteAddress)) {
      if (hardLimiterMap.containsKey(remoteAddress)) {
        if (!hardLimiterMap.get(remoteAddress).tryAcquire()) {
          log.debug("Hard Rate limit fired for " + remoteAddress);
          ctx.channel().attr(XrpcConstants.XRPC_HARD_RATE_LIMITED).set(Boolean.TRUE);
        } else if (!softLimiterMap.get(remoteAddress).tryAcquire()) {
          ctx.channel().attr(XrpcConstants.XRPC_SOFT_RATE_LIMITED).set(Boolean.TRUE);
        }
      } else {
        hardLimiterMap.put(
            remoteAddress,
            RateLimiter.create(config.getClientRateLimitOverride().get(remoteAddress).get(1)));
        softLimiterMap.put(
            remoteAddress,
            RateLimiter.create(config.getClientRateLimitOverride().get(remoteAddress).get(0)));
      }

    } else {
      if (!hardLimiterMap
          .get(hardRateLimitHasher.getOne(remoteAddress.getBytes(XrpcConstants.DEFAULT_CHARSET)))
          .tryAcquire()) {
        log.debug("Hard Rate limit fired for " + remoteAddress);
        ctx.channel().attr(XrpcConstants.XRPC_HARD_RATE_LIMITED).set(Boolean.TRUE);
      } else if (!softLimiterMap
          .get(softRateLimitHasher.getOne(remoteAddress.getBytes(XrpcConstants.DEFAULT_CHARSET)))
          .tryAcquire()) {
        ctx.channel().attr(XrpcConstants.XRPC_SOFT_RATE_LIMITED).set(Boolean.TRUE);
      }
    }

    // Global Rate Limiter
    if (!globalHardLimiter.tryAcquire()) {
      log.debug("Global Hard Rate limit fired");
      ctx.channel().attr(XrpcConstants.XRPC_HARD_RATE_LIMITED).set(Boolean.TRUE);

    } else if (!globalSoftLimiter.tryAcquire()) {
      ctx.channel().attr(XrpcConstants.XRPC_SOFT_RATE_LIMITED).set(Boolean.TRUE);
    }

    ctx.fireChannelActive();

    timerMap.put(ctx, timer.time());
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelInactive();

    timerMap.remove(ctx).stop();
  }
}
