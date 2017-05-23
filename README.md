xrpc
====

Simple, production ready Java API server. ConnectionLimits, OAuth2 and TLS 1.2 built in.

Make .jar not .war ... 

```java
@Slf4j
public class Example {

  @AllArgsConstructor
  static class Person {
    private String name;
  }

  public static void main(String[] args) {
    List<Person> people = new ArrayList<>();

    // See Moshi documentation for the Moshi Magic
    Moshi moshi = new Moshi.Builder().build();
    Type type = Types.newParameterizedType(List.class, Person.class);
    JsonAdapter<List<Person>> adapter = moshi.adapter(type);

    // Build your router
    Router router = new Router(4,20);

    // Define a complex function call
    BiFunction<HttpRequest, Route, HttpResponse> personHandler = (x, y) -> {
      Person p = new Person(y.groups(x.uri()).get("person"));
      people.add(p);

      HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
      response.headers().set(CONTENT_TYPE, "text/plain");
      response.headers().setInt(CONTENT_LENGTH, 0);

      return response;
    };


    // Define a simple function call
    Function<HttpRequest, HttpResponse> peopleHandler = x -> {
      ByteBuf bb =  Unpooled.compositeBuffer();

      bb.writeBytes(adapter.toJson(people).getBytes());
      HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, bb);
      response.headers().set(CONTENT_TYPE, "text/plain");
      response.headers().setInt(CONTENT_LENGTH, bb.readableBytes());

      return response;
    };


    // Create your route mapping
    router.addRoute("/people/:person", personHandler);
    router.addRoute("/people", peopleHandler);


    try {
      // Fire away
      router.listenAndServe(8080);
    } catch (IOException e) {
      log.error("Failed to start people server", e);
    }

  }
```
