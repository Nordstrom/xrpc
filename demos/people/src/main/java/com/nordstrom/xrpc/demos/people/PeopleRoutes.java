package com.nordstrom.xrpc.demos.people;

import com.nordstrom.xrpc.server.Handler;
import com.nordstrom.xrpc.server.Routes;
import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;

public class PeopleRoutes {
  private final List<Person> people = new CopyOnWriteArrayList<>();

  public PeopleRoutes(Routes routes) {
    Handler getPeople = request -> request.response().ok(people);

    Handler postPerson =
        request -> {
          Person p = request.body(Person.class);
          people.add(p);
          return request.response().ok();
        };

    Handler getPerson =
        request -> {
          String name = request.variable("person");
          final Optional<Person> person =
              people.stream().filter(p -> Objects.equals(p.name, name)).findFirst();

          if (person.isPresent()) {
            return request.response().ok(person.get());
          }

          return request.response().notFound("Person Not Found");
        };

    routes.get("/people", getPeople);
    routes.post("/people", postPerson);
    routes.get("/people/{person}", getPerson);
  }

  // Application POJO for use in request / response.
  public static class Person {
    @Getter private String name;

    @ConstructorProperties("name")
    public Person(String name) {
      this.name = name;
    }
  }
}
