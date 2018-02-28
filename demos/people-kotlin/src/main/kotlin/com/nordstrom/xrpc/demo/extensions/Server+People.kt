package com.nordstrom.xrpc.demo.extensions

import com.nordstrom.xrpc.server.Routes
import com.nordstrom.xrpc.server.XrpcRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponse

data class RouteSpec(val method: HttpMethod,
                     val path: String,
                     val func: (XrpcRequest) -> HttpResponse)

interface Routable {
  val routes: Iterable<RouteSpec>
}

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
