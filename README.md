xrpc
====

xrpc is a production quality server that is capable of serving public facing APIs (read: sitting on the edge) and has an appropriate level of enterprise operationalization. It currently supports the http/1.1 and the http/2 protocols. It does so interchangeably, i.e your implementation does not need to change and it will automatically respond to a http/1.1 and a http/2 client the same way. The user is free to determine whatever payload they would like, but our recommendation is JSON where you don't control both ends and protobuf (version 3) where you do
 
## Testing with the Example class  
```
# Building the jar

```shell
$ ./gradlew shadowJar
```

# Running the jar

```shell
$ java -jar app.jar
```

# Basic http set

```shell
$ curl -k  https://localhost:8080/people/bob
```

# Basic http/2 get
--! This demo requires curl with http/2 !--
(see https://simonecarletti.com/blog/2016/01/http2-curl-macosx/)
```shell
$ curl -k  https://localhost:8080/people
[{"name":"bob"}]
```

```shell
$ curl -k  https://localhost:8080/people --http/1.1
[{"name":"bob"}]
```

# Proto http set
--! This demo requires curl with http/2 !--
(see https://simonecarletti.com/blog/2016/01/http2-curl-macosx/)
```shell
$ java -cp app.jar com.nordstrom.xrpc.demo.DinoSetEncoder trex blue | curl -k  https://localhost:8080/dinos/SetDino --data-binary @- -vv
```

# Proto http get

```shell
$ java -cp app.jar com.nordstrom.xrpc.demo.DinoGetRequestEncoder trex | curl -k -s https://localhost:8080/dinos/GetDino --data-binary @-
trexblue
```

# Contributing

Please see [the contributing guide](CONTRIBUTING.md) for details on contributing to this repository.
