package com.nordstrom.xrpc.server;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import com.google.common.collect.ImmutableMap;
import com.nordstrom.xrpc.XrpcConstants;
import com.nordstrom.xrpc.client.XUrl;
import com.nordstrom.xrpc.server.http.Recipes;
import com.nordstrom.xrpc.server.http.Route;
import com.nordstrom.xrpc.server.http.XHttpMethod;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class UrlRouter extends ChannelDuplexHandler {

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    XrpcConnectionContext xctx = ctx.channel().attr(XrpcConstants.CONNECTION_CONTEXT).get();
    xctx.getRequestMeter().mark();

    if (ctx.channel().hasAttr(XrpcConstants.XRPC_SOFT_RATE_LIMITED)) {
      ctx.writeAndFlush(
              Recipes.newResponse(
                  HttpResponseStatus.TOO_MANY_REQUESTS,
                  XrpcConstants.RATE_LIMIT_RESPONSE.retain(),
                  Recipes.ContentType.Text_Plain))
          .addListener(ChannelFutureListener.CLOSE);
      xctx.getMetersByStatusCode().get(HttpResponseStatus.TOO_MANY_REQUESTS).mark();
      return;
    }

    if (msg instanceof HttpRequest) {
      FullHttpRequest request = (FullHttpRequest) msg;
      String path = XUrl.getPath(request.uri());
      for (Route route : xctx.getRoutes().get().descendingKeySet()) {
        Optional<Map<String, String>> groups = Optional.ofNullable(route.groups(path));
        if (groups.isPresent()) {
          XrpcRequest xrpcRequest = new XrpcRequest(request, groups.get(), ctx.channel());
          xrpcRequest.setData(request.content());
          HttpResponse resp;
          Optional<ImmutableMap<XHttpMethod, Handler>> handlerMapOptional =
              xctx.getRoutes()
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
                xctx.getRoutes()
                    .get()
                    .get(route)
                    .stream()
                    .filter(mx -> mx.containsKey(XHttpMethod.ANY))
                    .findFirst()
                    .get()
                    .get(XHttpMethod.ANY)
                    .handle(xrpcRequest);
          }

          xctx.getMetersByStatusCode().get(resp.status()).mark();

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
      xctx.getMetersByStatusCode().get(HttpResponseStatus.NOT_FOUND).mark();
    }
    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelReadComplete();
  }
}
