package com.nordstrom.xrpc.demo.app

import com.nordstrom.xrpc.demo.extensions.addRoutable
import com.nordstrom.xrpc.demo.routes.PeopleRoutes
import com.nordstrom.xrpc.server.Server
import com.typesafe.config.ConfigFactory

fun main(args: Array<String>) {
  val config = ConfigFactory.load("demo.conf")
  Server(config).apply {
    addRoutable(PeopleRoutes())
    listenAndServe()
    Runtime.getRuntime().addShutdownHook(Thread { shutdown() })
  }
}
