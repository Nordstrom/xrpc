xrpc
====
[![Build Status][ci-image]][ci-link] [ ![Download][artifact-image]][artifact-download]

Xrpc is a framework for creating production quality API services on top of Netty. The framework helps to encapsulate our
best practices and provides sane (and safe) defaults.

It currently supports the http/1.1 and the http/2 protocols. It does so interchangeably, 
i.e your implementation does not need to change and it will automatically respond to a http/1.1 
and a http/2 client the same way. The user is free to determine whatever payload they would like, 
but our recommendation is JSON where you don't control both ends and protobuf (version 3) where you do.

# Testing with the Example class  
```
# Building the jar

```shell
$ ./gradlew shadowJar
```

# Running the jar

```shell
$ java -jar app.jar
```

# Running the people demo app in a test server

```shell
$ ./bin/startPeopleTestServer.sh
```

# Basic http set

```shell
$ curl -k -d "bob" -X POST https://localhost:8080/people
```

# Basic http/2 get
--! This demo requires curl with http/2 !--
(see https://simonecarletti.com/blog/2016/01/http2-curl-macosx/)
```shell
$ curl -k  https://localhost:8080/people
[{"name":"bob"}]
```

```shell
$ curl -k  https://localhost:8080/people --http1.1
[{"name":"bob"}]
```

# Running the dino demo app in a test server
Run the dino app server to demo proto

```shell
$ ./bin/startDinoTestServer.sh
```

# Proto http set
--! This demo requires curl with http/2 !--
(see https://simonecarletti.com/blog/2016/01/http2-curl-macosx/)
```shell
$ java -cp app.jar com.nordstrom.xrpc.demo.DinoSetEncoder trex blue | curl -k  https://localhost:8080/DinoService/SetDino --data-binary @- -vv
```

# Proto http get

```shell
$ java -cp app.jar com.nordstrom.xrpc.demo.DinoGetRequestEncoder trex | curl -k -s https://localhost:8080/DinoService/GetDino --data-binary @-
trexblue
```

# Admin routes

xrpc comes with some built in admin routes. See also [Router.java](https://github.com/Nordstrom/xrpc/blob/master/src/main/java/com/nordstrom/xrpc/server/Router.java#L262-L270) and [AdminHandlers.java](https://github.com/Nordstrom/xrpc/blob/master/src/main/java/com/nordstrom/xrpc/server/AdminHandlers.java)
* `/metrics` -> Returns the metrics reporters in JSON format
* `/health` -> Expose a summary of downstream health checks
* `/ping` -> Responds with a 200-OK status code and the text 'PONG'
* `/ready` -> Exposes a Kubernetes or ELB specific healthcheck for liveliness
* `/restart` -> Restart service (should be restricted to approved devs / tooling)
* `/killkillkill` -> Shutdown service (should be restricted to approved devs / tooling)

# Contributing

Please see [the contributing guide](CONTRIBUTING.md) for details on contributing to this repository.


[ci-image]:https://travis-ci.org/Nordstrom/xrpc.svg?branch=master
[ci-link]:https://travis-ci.org/Nordstrom/xrpc
[artifact-image]:https://api.bintray.com/packages/nordstromoss/oss_maven/xrpc/images/download.svg
[artifact-download]:https://bintray.com/nordstromoss/oss_maven/xrpc/_latestVersion
