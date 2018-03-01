package com.nordstrom.xrpc.demos.people;

import com.nordstrom.xrpc.server.Handler;
import com.nordstrom.xrpc.server.RouteBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Value;

public class PeopleRoutes extends RouteBuilder {
  private final List<Person> people = new CopyOnWriteArrayList<>();

  public PeopleRoutes() {
    Handler getPeople = request -> request.okJsonResponse(people);

    Handler postPerson =
        request -> {
          Person p = new Person(request.getDataAsString());
          people.add(p);
          return request.okResponse();
        };

    Handler getPerson =
        request -> {
          String name = request.variable("person");
          final Optional<Person> person =
              people.stream().filter(p -> Objects.equals(p.name, name)).findFirst();

          if (person.isPresent()) {
            return request.okJsonResponse(person.get());
          }

          return request.notFoundJsonResponse("Person Not Found");
        };

    get("/people", getPeople);
    post("/people", postPerson);
    get("/people/{person}", getPerson);
  }

  // Application POJO for use in request / response.
  @Value
  public static class Person {
    private String name;
  }
}
