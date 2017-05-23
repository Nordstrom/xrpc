xrpc
====

Simple, production ready Java API server. ConnectionLimits, OAuth2 and TLS 1.2 built in.

Make .jar not .war ... 

```java
    List<String> people = new ArrayList<>();

    // Acceptor threads, Worker threads
    Router router = new Router(4,20); 

    // Create a simple function to handle a post
    BiFunction<HttpRequest, Map<String, String>, HttpResponse> personHandler = (x, y) -> {
      people.add(y.get("person"));

      return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    };

    // Create a more complicated function with URI vars
    Function<HttpRequest, HttpResponse> peopleHandler = x -> {
      ByteBuf bb =  Unpooled.compositeBuffer();
      people.forEach(xs -> bb.writeBytes(xs.getBytes()));

      return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, bb);
    };

    // Oh so simple 
    router.addRoute("/people/:person", personHandler);
    router.addRoute("/people", peopleHandler);

    try {
      router.listenAndServe(8080);
    } catch (IOException e) {
      log.error("Failed to start people server", e);
    } finally {
      router.shutdown();
    }
```
