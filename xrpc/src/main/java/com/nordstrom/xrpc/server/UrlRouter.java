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

package com.nordstrom.xrpc.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nordstrom.xrpc.XrpcConstants;
import com.nordstrom.xrpc.client.XUrl;
import com.nordstrom.xrpc.server.http.Recipes;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
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

    if (msg instanceof FullHttpRequest) {
      FullHttpRequest request = (FullHttpRequest) msg;
      String path = XUrl.getPath(request.uri());
      CompiledRoutes.Match match = xctx.getRoutes().match(path, request.method());

      ObjectMapper mapper = xctx.getMapper();
      XrpcRequest xrpcRequest = new XrpcRequest(request, xctx, match.getGroups(), ctx.channel());
      xrpcRequest.setData(request.content());

      HttpResponse resp = match.getHandler().handle(xrpcRequest);

      xctx.getMetersByStatusCode().get(resp.status()).mark();

      ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }
    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelReadComplete();
  }
}
