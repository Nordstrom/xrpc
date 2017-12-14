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

  //TODO(JR): Need to impl a fell admin handler here
  public static Handler adminHandler() {
    return xrpcRequest -> Recipes.newResponseOk("TODO");
  }

  public static Handler pingHandler() {
    return xrpcRequest -> Recipes.newResponseOk("PONG");
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
}
