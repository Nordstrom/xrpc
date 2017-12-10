package com.nordstrom.xrpc.server;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import com.codahale.metrics.Meter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.nordstrom.xrpc.client.XUrl;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@ChannelHandler.Sharable
public class UrlRouter extends ChannelDuplexHandler {

  private final AtomicReference<ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>>>
      routes;
  private final Meter requests;
  private ConcurrentHashMap<HttpResponseStatus, Meter> metersByStatusCode;

  public UrlRouter(
      AtomicReference<ImmutableSortedMap<Route, List<ImmutableMap<XHttpMethod, Handler>>>> routes,
      Meter requests,
      ConcurrentHashMap<HttpResponseStatus, Meter> metersByStatusCode) {

    this.routes = routes;
    this.requests = requests;
    this.metersByStatusCode = metersByStatusCode;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    requests.mark();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpRequest) {
      FullHttpRequest request = (FullHttpRequest) msg;
      String path = XUrl.getPath(request.uri());
      for (Route route : routes.get().descendingKeySet()) {
        Optional<Map<String, String>> groups = Optional.ofNullable(route.groups(path));
        if (groups.isPresent()) {
          XrpcRequest xrpcRequest = new XrpcRequest(request, groups.get(), ctx.channel());

          HttpResponse resp;
          Optional<ImmutableMap<XHttpMethod, Handler>> handlerMapOptional =
              routes
                  .get()
                  .get(route)
                  .stream()
                  .filter(
                      m -> m.keySet().stream().anyMatch(mx -> mx.compareTo(request.method()) == 0))
                  .findFirst();

          if (handlerMapOptional.isPresent()) {
            resp =
                handlerMapOptional
                    .get()
                    .get(handlerMapOptional.get().keySet().asList().get(0))
                    .handle(xrpcRequest);
          } else {
            resp =
                routes
                    .get()
                    .get(route)
                    .stream()
                    .filter(mx -> mx.containsKey(XHttpMethod.ANY))
                    .findFirst()
                    .get()
                    .get(XHttpMethod.ANY)
                    .handle(xrpcRequest);
          }

          metersByStatusCode.get(resp.status()).mark();

          ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
          ctx.fireChannelRead(msg);
          return;
        }
      }
      // No matching route.
      FullHttpResponse response =
          new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
      response.headers().set(CONTENT_TYPE, "text/plain");
      response.headers().setInt(CONTENT_LENGTH, 0);
      ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
      metersByStatusCode.get(HttpResponseStatus.NOT_FOUND).mark();
    }
    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelReadComplete();
  }
}
