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
11) Many Much more

Make .jar not .war ...

Example.java
```java
@Slf4j
public class Example {
  /** Example POJO for use in request / response. */
  @AllArgsConstructor
  private static class Person {
    private String name;
  }

  public static void main(String[] args) {
    final List<Person> people = new ArrayList<>();

    // See https://github.com/square/moshi for the Moshi Magic.
    Moshi moshi = new Moshi.Builder().build();
    Type type = Types.newParameterizedType(List.class, Person.class);
    JsonAdapter<List<Person>> adapter = moshi.adapter(type);

    // Load application config from jar resources. The 'load' method below also allows supports
    // overrides from environment variables.
    Config config = ConfigFactory.load("demo.conf");

    // Build your router. This overrides the default configuration with values from
    // src/main/resources/demo.conf.
    XConfig xConfig = new XConfig(config.getConfig("xrpc"));
    Router router = new Router(xConfig);

    // Define a simple function call.
    Handler peopleHandler =
        context -> {
          return Recipes.newResponse(
            HttpResponseStatus.OK,
            context.getAlloc().directBuffer().writeBytes(adapter.toJson(people).getBytes()),
            Recipes.ContentType.Application_Json);
        };

    // Define a complex function call
    Handler personHandler =
        context -> {
          Person p = new Person(context.variable("person"));
          people.add(p);

          return Recipes.newResponseOk("");
        };


    // Define a simple function call
    Handler healthCheckHandler =
        context -> {
          return Recipes.newResponseOk("");
     };

    // Create your route mapping
    router.addRoute("/people/{person}", personHandler);
    router.addRoute("/people", peopleHandler);

    // Health Check for k8s
    router.addRoute("/health", healthCheckHandler);

    try {
      // Fire away
      router.listenAndServe();
    } catch (IOException e) {
      log.error("Failed to start people server", e);
    }
  }
}
 
```

# Building the jar

```shell
$ ./gradlew shadowJar
```

# Running the jar

```shell
$ java -jar build/libs/xrpc-0.1.0-SNAPSHOT-all.jar
```

# Basic http set

```shell
$ curl -k  https://localhost:8080/people/bob
```

# Basic http get

```shell
$ curl -k  https://localhost:8080/people
[{"name":"bob"}]
```

# Proto encode/decode

```shell
$ java -cp build/libs/xrpc-0.1.0-SNAPSHOT-all.jar com.nordstrom.xrpc.DinoEncoder trex blue > out
$ java -cp build/libs/xrpc-0.1.0-SNAPSHOT-all.jar com.nordstrom.xrpc.DinoDecoder < out
Dino{name=trex, fav_color=blue}
```

# Proto http set

```shell
$ java -cp build/libs/xrpc-0.1.0-SNAPSHOT-all.jar com.nordstrom.xrpc.DinoEncoder trex blue | curl -k  https://localhost:8080/dinos/trex --data-binary @-
```

# Proto http get

```shell
$ curl -k -s   https://localhost:8080/dinos/ | java -cp build/libs/xrpc-0.1.0-SNAPSHOT-all.jar com.nordstrom.xrpc.DinoDecoder
Dino{name=trex, fav_color=blue}
```

# Contributing

Please see [the contributing guide](CONTRIBUTING.md) for details on contributing to this repository.
