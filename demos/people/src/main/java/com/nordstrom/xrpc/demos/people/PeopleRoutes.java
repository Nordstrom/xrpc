package com.nordstrom.xrpc.demos.people;

import com.nordstrom.xrpc.server.Handler;
import com.nordstrom.xrpc.server.Router;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Value;

public class PeopleRoutes {
  private final List<Person> people = new ArrayList<>();

  public PeopleRoutes(Router router) {
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
          Optional<Person> person =
              people.stream().filter(p -> Objects.equals(p.name, name)).findFirst();

          if (person.isPresent()) {
            return request.okJsonResponse(person.get());
          }
          return request.notFoundJsonResponse("Person Not Found");
        };

    router.get("/people", getPeople);
    router.post("/people", postPerson);
    router.get("/people/{person}", getPerson);
  }

  // Application POJO for use in request / response.
  @Value
  public static class Person {
    private String name;
  }
}
