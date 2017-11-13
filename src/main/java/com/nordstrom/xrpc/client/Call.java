package com.nordstrom.xrpc.client;

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

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.nordstrom.xrpc.client.retry.BoundedExponentialBackoffRetry;
import com.nordstrom.xrpc.client.retry.RetryLoop;
import com.nordstrom.xrpc.client.retry.TracerDriver;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Call {
  private final XrpcClient client;
  private final String uri;

  private FullHttpRequest request = null;

  public Call(XrpcClient client, String uri) {

    this.client = client;
    this.uri = uri;
  }

  public Call get(FullHttpRequest request) {
    this.request = request;

    return this;
  }

  public ListenableFuture<FullHttpResponse> execute() {
    Preconditions.checkState(request != null);
    final SettableFuture<FullHttpResponse> error = SettableFuture.create();
    final SettableFuture<FullHttpResponse> response = SettableFuture.create();

    ListenableFuture<ChannelFuture> connectFuture =
        connect(
            new InetSocketAddress(XUrl.getHost(uri), XUrl.getPort(uri)),
            client.getBootstrap(),
            buildRetryLoop());

    Futures.addCallback(
        connectFuture,
        new FutureCallback<ChannelFuture>() {
          @Override
          public void onSuccess(ChannelFuture result) {
            try {
              Channel channel = result.await().channel();
              channel.writeAndFlush(request);

              HttpResponseHandler responseHandler =
                  (HttpResponseHandler) channel.pipeline().get("responseHandler");
              response.setFuture(responseHandler.getResponse());
            } catch (InterruptedException e) {
              response.cancel(true);
              error.setException(e);
            }
          }

          @Override
          public void onFailure(Throwable t) {
            response.cancel(true);
            error.setException(t);
          }
        });

    if (response.isCancelled()) {
      return error;
    }

    return response;
  }

  private RetryLoop buildRetryLoop() {
    return buildRetryLoop(50, 500, 4);
  }

  private RetryLoop buildRetryLoop(int baseSleep, int maxSleep, int reties) {
    BoundedExponentialBackoffRetry retry =
        new BoundedExponentialBackoffRetry(baseSleep, maxSleep, reties);

    TracerDriver tracerDriver =
        new TracerDriver() {

          @Override
          public void addTrace(String name, long time, TimeUnit unit) {}

          @Override
          public void addCount(String name, int increment) {}
        };

    return new RetryLoop(retry, new AtomicReference<>(tracerDriver));
  }

  private SettableFuture<ChannelFuture> connect(
      InetSocketAddress server, Bootstrap bootstrap, RetryLoop retryLoop) {
    final SettableFuture<ChannelFuture> f = SettableFuture.create();
    ChannelFutureListener listener =
        new ChannelFutureListener() {
          @Override
          public void operationComplete(ChannelFuture future) {
            if (!future.isSuccess()) {
              try {
                retryLoop.takeException((Exception) future.cause());
                log.info("==== Service connect failure (will retry)", future.cause());
                connect(server, bootstrap, retryLoop);
              } catch (Exception e) {
                log.error("==== Service connect failure ", future.cause());
                // Close the connection if the connection attempt has failed.
                future.channel().close();
                f.setException((Exception) e);
              }
            } else {
              log.debug("Xrpc connected to: " + server);
              String hostname = server.getAddress().getHostAddress();
              if (hostname.equals("localhost")) {
                hostname = "127.0.0.1";
              }
              log.debug(
                  "Adding hostname: "
                      + hostname
                      + ":"
                      + ((InetSocketAddress) future.channel().remoteAddress()).getPort());
              f.set(future);
            }
          }
        };

    bootstrap.connect(server).addListener(listener);

    return f;
  }
}
