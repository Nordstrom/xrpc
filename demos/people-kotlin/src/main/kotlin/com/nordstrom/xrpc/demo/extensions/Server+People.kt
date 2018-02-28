package com.nordstrom.xrpc.demo.extensions

import com.nordstrom.xrpc.demo.protocol.Routable
import com.nordstrom.xrpc.server.Routes
import io.netty.handler.codec.http.HttpMethod


fun Routes.addRoutable(routable: Routable) {
  apply {
    routable.routes.forEach {
      when (it.method) {
        HttpMethod.OPTIONS -> options(it.path, it.func)
        HttpMethod.GET -> get(it.path, it.func)
        HttpMethod.HEAD -> head(it.path, it.func)
        HttpMethod.POST -> post(it.path, it.func)
        HttpMethod.PUT -> put(it.path, it.func)
        HttpMethod.PATCH -> patch(it.path, it.func)
        HttpMethod.DELETE -> delete(it.path, it.func)
        HttpMethod.TRACE -> trace(it.path, it.func)
        HttpMethod.CONNECT -> connect(it.path, it.func)
      }
    }
  }
}
