xrpc
====

Simple, production ready Java API server built on top of functional composition.

What you get:
1) Openssl Native TLS w/ direct x509 pem support (vs keytool)
2) Epoll native
3) RateLimiting
4) Connection Limiting
5) Log forwarder Integration
6) Metrics & Logs
7) Binary Debug for packet level inspection
8) Protobuf support
9) Docker
10) k8s
11) protobuf
12) Many Much more

Make .jar not .war ...


Example.java
```java
@Slf4j
public class Example {

  @AllArgsConstructor
  private static class Person {

    private String name;
  }

  public static void main(String[] args) {
    final List<Person> people = new ArrayList<>();
    final List<Dino> dinos = new ArrayList<>();

    // See https://github.com/square/moshi for the Moshi Magic
    Moshi moshi = new Moshi.Builder().build();
    Type type = Types.newParameterizedType(List.class, Person.class);
    JsonAdapter<List<Person>> adapter = moshi.adapter(type);

    // Build your router
    Router router = new Router();

    // Define a simple function call
    Function<HttpRequest, HttpResponse> peopleHandler = x -> {
      ByteBuf bb = Unpooled.compositeBuffer();

      bb.writeBytes(adapter.toJson(people).getBytes(Charset.forName("UTF-8")));
      HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
	  HttpResponseStatus.OK, bb);
      response.headers().set(CONTENT_TYPE, "text/plain");
      response.headers().setInt(CONTENT_LENGTH, bb.readableBytes());

      return response;
    };


    // Define a complex function call
    BiFunction<HttpRequest, Route, HttpResponse> personHandler = (x, y) -> {
      Person p = new Person(y.groups(x.uri()).get("person"));
      people.add(p);

      HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
	  HttpResponseStatus.OK);
      response.headers().set(CONTENT_TYPE, "text/plain");
      response.headers().setInt(CONTENT_LENGTH, 0);

      return response;
    };


    // do some proto
    Function<HttpRequest, HttpResponse> dinosHandler = x -> {
      ByteBuf bb = Unpooled.compositeBuffer();

      bb.writeBytes(dinos.get(0).encode());
      HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
	  HttpResponseStatus.OK, bb);
      response.headers().set(CONTENT_TYPE, "application/octet-stream");
      response.headers().setInt(CONTENT_LENGTH, bb.readableBytes());

      return response;
    };


    // Define a complex function call with Proto
    BiFunction<HttpRequest, Route, HttpResponse> dinoHandler = (x, y) -> {

      Optional<Dino> d = null;
      try {
	Optional.of(Dino.ADAPTER.decode(((FullHttpRequest) x).content().array()));
      } catch (IOException e) {
	HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
	    HttpResponseStatus.BAD_REQUEST);
	response.headers().set(CONTENT_TYPE, "text/plain");
	response.headers().setInt(CONTENT_LENGTH, 0);

	return response;
      }

      d.ifPresent(dinos::add);

      HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
	  HttpResponseStatus.OK);
      response.headers().set(CONTENT_TYPE, "text/plain");
      response.headers().setInt(CONTENT_LENGTH, 0);

      return response;
    };


    // Create your route mapping
    router.addRoute("/people/:person", personHandler);
    router.addRoute("/people", peopleHandler);

    // Create your route mapping
    router.addRoute("/dinos/:dino", dinoHandler);
    router.addRoute("/dinos", dinosHandler);

    try {
      // Fire away
      router.listenAndServe();
    } catch (IOException e) {
      log.error("Failed to start people server", e);
    }

  }
```
