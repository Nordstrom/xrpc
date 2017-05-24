package com.xjeffrose.xrpc;


import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Example {

  public static void main(String[] args) {
    final List<Person> people = new ArrayList<>();

    // See https://github.com/square/moshi for the Moshi Magic
    Moshi moshi = new Moshi.Builder().build();
    Type type = Types.newParameterizedType(List.class, Person.class);
    JsonAdapter<List<Person>> adapter = moshi.adapter(type);

    // Build your router
    Router router = new Router(4, 20);

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

  @AllArgsConstructor
  static class Person {

    private String name;
  }

}
