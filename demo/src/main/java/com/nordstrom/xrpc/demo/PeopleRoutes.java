package com.nordstrom.xrpc.demo;

import com.nordstrom.xrpc.server.Router;
import com.nordstrom.xrpc.server.http.Recipes;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;

public class PeopleRoutes {
  final List<Person> people = new ArrayList<>();

  public PeopleRoutes(Router router) {
    // For the JSON portion of the demo
    Moshi moshi = new Moshi.Builder().build();
    Type type = Types.newParameterizedType(List.class, Person.class);
    JsonAdapter<List<Person>> adapter = moshi.adapter(type);

    router.get(
        "/people/{person}",
        request -> {
          byte[] jsonBytes = adapter.toJson(people).getBytes(StandardCharsets.UTF_8);
          return Recipes.newResponse(
              HttpResponseStatus.OK,
              request.getAlloc().directBuffer().writeBytes(jsonBytes),
              Recipes.ContentType.Application_Json);
        });

    // Add handler for HTTP POST:/people route
    router.post(
        "/people",
        request -> {
          Person p = new Person(request.getDataAsString());
          people.add(p);

          return Recipes.newResponseOk("");
        });

    // Add handler for HTTP GET:/people route
    router.get(
        "/people",
        request -> {
          byte[] jsonBytes = adapter.toJson(people).getBytes(StandardCharsets.UTF_8);
          return Recipes.newResponse(
              HttpResponseStatus.OK,
              request.getAlloc().directBuffer().writeBytes(jsonBytes),
              Recipes.ContentType.Application_Json);
        });
  }

  // Example POJO for use in request / response.
  @AllArgsConstructor
  private static class Person {
    private String name;
  }
}
