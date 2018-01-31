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
package com.nordstrom.xrpc.server;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.nordstrom.xrpc.server.http.Recipes;
import java.util.SortedMap;

public class AdminHandlers {
  public static Handler metricsHandler(MetricRegistry metrics, ObjectMapper mapper) {
    Preconditions.checkState(metrics != null);
    Preconditions.checkState(mapper != null);
    return xrpcRequest ->
        Recipes.newResponseOk(
            xrpcRequest
                .getAlloc()
                .directBuffer()
                .writeBytes(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(metrics)),
            Recipes.ContentType.Application_Json);
  }

  // TODO(JR): Need to impl a fell admin handler here
  public static Handler adminHandler() {
    return xrpcRequest -> Recipes.newResponseOk("TODO");
  }

  public static Handler pingHandler() {
    return xrpcRequest -> Recipes.newResponseOk("PONG");
  }

  public static Handler infoHandler() {

    return xrpcRequest -> Recipes.newResponseOk("TODO");
  }

  public static Handler gcHandler() {
    Runtime.getRuntime().gc();

    return xrpcRequest -> Recipes.newResponseOk("OK");
  }

  public static Handler healthCheckHandler(
      HealthCheckRegistry healthCheckRegistry, ObjectMapper mapper) {
    Preconditions.checkState(healthCheckRegistry != null);
    Preconditions.checkState(mapper != null);

    SortedMap<String, HealthCheck.Result> healthChecks = healthCheckRegistry.runHealthChecks();

    return xrpcRequest ->
        Recipes.newResponseOk(
            xrpcRequest
                .getAlloc()
                .directBuffer()
                .writeBytes(
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(healthChecks)),
            Recipes.ContentType.Application_Json);
  }

  public static Handler readyHandler() {
    return xrpcRequest -> Recipes.newResponseOk("OK");
  }

  public static Handler restartHandler(Router router) {
    return xrpcRequest -> Recipes.newResponseOk("TODO");
  }

  public static Handler killHandler(Router router) {
    router.shutdown();

    return xrpcRequest -> Recipes.newResponseOk("OK");
  }
}
