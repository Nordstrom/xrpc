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

    Handler customException =
        request -> {
          ErrorResponse errorResponse = new ErrorResponse();
          errorResponse.setBusinessReason("Some business reason");
          errorResponse.setBusinessStatusCode("4.2.3000");
          throw new MyCustomException(errorResponse);
        };

    get("/people", getPeople);
    post("/people", postPerson);
    get("/people/{person}", getPerson);

    // This route demonstrate how to throw custom exception payload to the client in xrpc
    // Output of this route will be 400 with this json payload {"businessReason":"Some business
    // reason","businessStatusCode":"4.2.3000"}
    get("/show-customexception", customException);
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
