/*
 * Copyright 2018 Nordstrom, Inc.
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
import com.google.common.util.concurrent.MoreExecutors;
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
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Call {
  private final XrpcClient client;
  private final String uri;
  private final FullHttpRequest request;

  Call(XrpcClient client, String uri) {

    this.client = client;
    this.uri = uri;
    this.request = null;
  }

  private Call(XrpcClient client, String uri, FullHttpRequest request) {

    this.client = client;
    this.uri = uri;
    this.request = request;
  }

  public Call get(FullHttpRequest request) {

    return new Call(client, uri, request);
  }

  public ListenableFuture<FullHttpResponse> execute() throws URISyntaxException {
    Preconditions.checkState(request != null);
    final SettableFuture<FullHttpResponse> error = SettableFuture.create();
    final SettableFuture<FullHttpResponse> response = SettableFuture.create();
    final ListenableFuture<ChannelFuture> connectFuture =
        connect(XUrl.getInetSocket(uri), client.getBootstrap(), buildRetryLoop());

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
        },
        MoreExecutors.directExecutor());

    if (response.isCancelled()) {
      return error;
    } else {
      return response;
    }
  }

  private RetryLoop buildRetryLoop() {
    // TODO(JR): Make these retry options configurable, perhaps from a client.conf?
    return buildRetryLoop(50, 500, 4);
  }

  private RetryLoop buildRetryLoop(int baseSleep, int maxSleep, int reties) {
    BoundedExponentialBackoffRetry retry =
        new BoundedExponentialBackoffRetry(baseSleep, maxSleep, reties);

    /*
     * TODO(JR): This trace driver will be used in the future to allow for tracing of reties and
     * will also be the entry point for a future circuit breaker logic. As of now these features are
     * not enabled yet, but this entrypoint should be maintained.
     */
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
                f.setException(e);
              }
            } else {
              log.debug("Xrpc connected to: {}", server);
              String hostname = server.getAddress().getHostAddress();
              if (hostname.equals("localhost")) {
                hostname = "127.0.0.1";
              }
              log.debug(
                  "Adding hostname: {}:{}",
                  hostname,
                  ((InetSocketAddress) future.channel().remoteAddress()).getPort());
              f.set(future);
            }
          }
        };

    // TODO(JR): There should be a configurable timeout here
    try {
      bootstrap.connect(server).addListener(listener).await(200, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      try {
        retryLoop.takeException(e);
      } catch (Exception e1) {
        // TODO(JR): Throw proper exception for exceeding retry limit

      }
      log.info("==== Service connect failure (will retry)", e);
      connect(server, bootstrap, retryLoop);
    }

    return f;
  }
}
