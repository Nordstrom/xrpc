/*
 * Copyright 2017 Nordstrom, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nordstrom.xrpc.demo;

import com.codahale.metrics.health.HealthCheck;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.nordstrom.xrpc.XConfig;
import com.nordstrom.xrpc.XrpcConstants;
import com.nordstrom.xrpc.demo.proto.*;
import com.nordstrom.xrpc.server.Handler;
import com.nordstrom.xrpc.server.Router;
import com.nordstrom.xrpc.server.XrpcRequest;
import com.nordstrom.xrpc.server.http.Recipes;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Example {
  public static void main(String[] args) {
    final List<Person> people = new ArrayList<>();
    final List<Dino> dinos = new ArrayList<>();

    // For the JSON portion of the demo
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
        request -> {
          return Recipes.newResponse(
              HttpResponseStatus.OK,
              request.getAlloc().directBuffer().writeBytes(adapter.toJson(people).getBytes()),
              Recipes.ContentType.Application_Json);
        };

    // Define a complex function call
    Handler personPostHandler =
      request -> {
      byte[] _p = new byte[request.getData().readableBytes()];
      request.getData().readBytes(_p, 0, request.getData().readableBytes());
        Person p = new Person(new String(_p, XrpcConstants.DEFAULT_CHARSET));
        people.add(p);

        return Recipes.newResponseOk("");
      };

    // Define a complex function call
    Handler personHandler =
        request -> {
          Person p = new Person(request.variable("person"));
          people.add(p);

          return Recipes.newResponseOk("");
        };

    // RPC style endpoint
    Handler dinoHandler =
        request -> {
          String path = request.variable("method");
          switch (path) {
            case "SetDino":
              return setDino(request, dinos);
            case "GetDino":
              return getDino(request, dinos);
            default:
              return Recipes.newResponseBadRequest("Method not found in Dino Service");
          }
        };

    // Create your route mapping for the JSON requests
    router.addRoute("/people/{person}", personHandler, HttpMethod.GET);
    router.addRoute("/people", personPostHandler, HttpMethod.POST);
    router.addRoute("/people", peopleHandler, HttpMethod.GET);

    // Create your route mapping
    router.addRoute("/dinos/{method}", dinoHandler);

    // Add a service specific health check
    router.addHealthCheck("simple", new SimpleHealthCheck());

    try {
      // Fire away
      router.listenAndServe();
    } catch (IOException e) {
      log.error("Failed to start people server", e);
    }
  }

  private static HttpResponse getDino(XrpcRequest request, List<Dino> dinos) {
    try {
      DinoGetRequest getRequest =
          DinoGetRequest.parseFrom(ByteString.copyFrom(request.getData().nioBuffer()));
      Optional<Dino> dinoOptional =
          dinos.stream().filter(xs -> xs.getName().equals(getRequest.getName())).findFirst();
      dinoOptional.ifPresent(
          dino ->
              Recipes.newResponse(
                  HttpResponseStatus.OK,
                  request
                      .getByteBuf()
                      .writeBytes(DinoGetReply.newBuilder().setDino(dino).build().toByteArray()),
                  Recipes.ContentType.Application_Octet_Stream));

    } catch (InvalidProtocolBufferException e) {
      return Recipes.newResponseBadRequest("Malformed GetDino Request");
    }

    return Recipes.newResponseOk("Dino not Found");
  }

  private static HttpResponse setDino(XrpcRequest request, List<Dino> dinos) {
    try {
      DinoSetRequest setRequest =
          DinoSetRequest.parseFrom(ByteString.copyFrom(request.getData().nioBuffer()));
      dinos.add(setRequest.getDino());
      System.out.println(setRequest.getDino().toString());
      return Recipes.newResponse(
          HttpResponseStatus.OK,
          request
              .getByteBuf()
              .writeBytes(DinoSetReply.newBuilder().setResponseCode("OK").build().toByteArray()),
          Recipes.ContentType.Application_Octet_Stream);
    } catch (InvalidProtocolBufferException e) {
      return Recipes.newResponseBadRequest("Malformed SetDino Request");
    }

    //return Recipes.newResponseBadRequest("Could not SetDino");
  }

  /** Example POJO for use in request / response. */
  @AllArgsConstructor
  private static class Person {
    private String name;
  }

  public static class SimpleHealthCheck extends HealthCheck {

    public SimpleHealthCheck() {}

    @Override
    protected Result check() throws Exception {
      System.out.println("Health Check Ran");
      return Result.healthy();
    }
  }
}
