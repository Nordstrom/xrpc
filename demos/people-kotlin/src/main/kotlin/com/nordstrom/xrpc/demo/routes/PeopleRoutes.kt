package com.nordstrom.xrpc.demo.routes

import com.nordstrom.xrpc.demo.model.Person
import com.nordstrom.xrpc.demo.protocol.Routable
import com.nordstrom.xrpc.demo.protocol.RouteSpec
import com.nordstrom.xrpc.demo.util.MoshiFactory
import com.nordstrom.xrpc.server.XrpcRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponse
import java.util.concurrent.CopyOnWriteArrayList

class PeopleRoutes : Routable {

  private val people = CopyOnWriteArrayList<Person>()

  override val routes: Iterable<RouteSpec>
    get() = listOf(
      RouteSpec(method = HttpMethod.GET, path = "/people", func = ::getPeople),
      RouteSpec(method = HttpMethod.POST, path = "/people", func = ::postPerson),
      RouteSpec(method = HttpMethod.GET, path = "/people/{person}", func = ::getPerson)
    )

  private fun getPeople(request: XrpcRequest): HttpResponse {
    return request.okJsonResponse(people)
  }

  private fun postPerson(request: XrpcRequest): HttpResponse {
    return MoshiFactory.personAdapter.fromJson(request.dataAsString)?.let {
      people.add(it)
      request.okResponse()
    } ?: request.badRequestJsonResponse(null)
  }

  private fun getPerson(request: XrpcRequest): HttpResponse {
    return people.firstOrNull {
      it.name == request.variable("person")
    }?.let {
      request.okJsonResponse(it)
    } ?: request.notFoundJsonResponse("Person Not Found")
  }
}
