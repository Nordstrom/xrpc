package com.nordstrom.xrpc.server;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import com.codahale.metrics.Meter;
import com.google.common.collect.ImmutableSortedMap;
import com.nordstrom.xrpc.client.XUrl;
import com.nordstrom.xrpc.server.http.Route;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@ChannelHandler.Sharable
public class UrlRouter extends ChannelDuplexHandler {

  private final AtomicReference<ImmutableSortedMap<Route, Handler>> routes;
  private final Meter requests;

  public UrlRouter(AtomicReference<ImmutableSortedMap<Route, Handler>> routes, Meter requests) {

    this.routes = routes;
    this.requests = requests;
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
          HttpResponse resp = routes.get().get(route).handle(xrpcRequest);

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
    }
    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelReadComplete();
  }
}
