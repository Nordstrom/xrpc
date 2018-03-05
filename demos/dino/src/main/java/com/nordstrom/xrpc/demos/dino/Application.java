/*
 * Copyright 2018 Nordstrom, Inc.
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

package com.nordstrom.xrpc.demos.dino;

import com.codahale.metrics.health.HealthCheck;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.nordstrom.xrpc.demos.dino.proto.Dino;
import com.nordstrom.xrpc.demos.dino.proto.DinoGetReply;
import com.nordstrom.xrpc.demos.dino.proto.DinoGetRequest;
import com.nordstrom.xrpc.demos.dino.proto.DinoSetReply;
import com.nordstrom.xrpc.demos.dino.proto.DinoSetRequest;
import com.nordstrom.xrpc.server.Handler;
import com.nordstrom.xrpc.server.Server;
import com.nordstrom.xrpc.server.XrpcRequest;
import com.nordstrom.xrpc.server.http.Recipes;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Application {
  public static void main(String[] args) {
    final List<Dino> dinos = new ArrayList<>();

    // Load application config from jar resources. The 'load' method below also allows supports
    // overrides from environment variables.
    Config config = ConfigFactory.load("demo.conf");

    // Build your server. This overrides the default configuration with values from
    // src/main/resources/demo.conf.
    Server server = new Server(config);

    // RPC style endpoint.
    Handler dinoHandler =
        request -> {
          String path = request.variable("method");
          switch (path) {
            case "SetDino":
              return setDino(request, dinos);
            case "GetDino":
              return getDino(request, dinos);
            default:
              return Recipes.newResponseBadRequest("Method not found in DinoService");
          }
        };

    // Add predefined handler for HTTP GET:/DinoService/{method}
    server.get("/DinoService/{method}", dinoHandler);

    // Add a service specific health check.
    server.addHealthCheck("simple", new SimpleHealthCheck());

    try {
      // Start the server!
      server.listenAndServe();
    } catch (IOException e) {
      log.error("Failed to start people server", e);
    }
  }

  private static FullHttpResponse getDino(XrpcRequest request, List<Dino> dinos) {
    try {
      DinoGetRequest getRequest =
          DinoGetRequest.parseFrom(CodedInputStream.newInstance(request.body().nioBuffer()));
      Optional<Dino> dinoOptional =
          dinos.stream().filter(xs -> xs.getName().equals(getRequest.getName())).findFirst();

      if (dinoOptional.isPresent()) {
        DinoGetReply getReply = DinoGetReply.newBuilder().setDino(dinoOptional.get()).build();
        ByteBuf resp = request.byteBuf();
        resp.ensureWritable(CodedOutputStream.computeMessageSizeNoTag(getReply), true);
        getReply.writeTo(new ByteBufOutputStream(resp));

        return Recipes.newResponse(
            HttpResponseStatus.OK,
            request.byteBuf().writeBytes(resp),
            Recipes.ContentType.Application_Octet_Stream);
      }

    } catch (IOException e) {
      return Recipes.newResponseBadRequest("Malformed GetDino Request: " + e.getMessage());
    }

    return Recipes.newResponseOk("Dino not Found");
  }

  private static HttpResponse setDino(XrpcRequest request, List<Dino> dinos) {
    try {

      Optional<DinoSetRequest> setRequest =
          Optional.of(
              DinoSetRequest.parseFrom(CodedInputStream.newInstance(request.body().nioBuffer())));
      setRequest.ifPresent(req -> dinos.add(req.getDino()));

      return Recipes.newResponse(
          HttpResponseStatus.OK,
          request
              .byteBuf()
              .writeBytes(DinoSetReply.newBuilder().setResponseCode("OK").build().toByteArray()),
          Recipes.ContentType.Application_Octet_Stream);
    } catch (IOException e) {
      return Recipes.newResponseBadRequest("Malformed SetDino Request: " + e.getMessage());
    }
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
