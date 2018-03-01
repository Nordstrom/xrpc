package com.nordstrom.xrpc.demos.people;

import com.nordstrom.xrpc.server.Handler;
import com.nordstrom.xrpc.server.RouteBuilder;
import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;

public class PeopleRoutes extends RouteBuilder {
  private final List<Person> people = new CopyOnWriteArrayList<>();

  public PeopleRoutes() {
    Handler getPeople = request -> request.ok(people);

    Handler postPerson =
        request -> {
          Person p = request.body(Person.class);
          people.add(p);
          return request.ok();
        };

    Handler getPerson =
        request -> {
          String name = request.variable("person");
          final Optional<Person> person =
              people.stream().filter(p -> Objects.equals(p.name, name)).findFirst();

          if (person.isPresent()) {
            return request.ok(person.get());
          }

          return request.notFound("Person Not Found");
        };

    get("/people", getPeople);
    post("/people", postPerson);
    get("/people/{person}", getPerson);
  }

  // Application POJO for use in request / response.
  public static class Person {
    @Getter private String name;

    // This should not be required, but is used by Jackson to properly bind this object
    // during decode.
    @ConstructorProperties("name")
    public Person(String name) {
      this.name = name;
    }
  }
}
