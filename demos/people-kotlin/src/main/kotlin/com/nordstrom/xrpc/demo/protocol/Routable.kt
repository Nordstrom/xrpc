package com.nordstrom.xrpc.demo.protocol

import com.nordstrom.xrpc.server.XrpcRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponse

data class RouteSpec(val method: HttpMethod = HttpMethod.GET,
                     val path: String,
                     val func: (XrpcRequest) -> HttpResponse)

interface Routable {
  val routes: Iterable<RouteSpec>
}
