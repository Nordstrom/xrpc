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
  // CHECKSTYLE:OFF
  /**
   * Output metrics reporters in JSON format.
   *
   * @param metrics MetricRegistry
   * @param mapper ObjectMapper
   * @return xrpcRequest object with metrics in JSON format
   *     <p>Example output:
   *     <pre>{@code
   * {
   *   "version": "3.1.3",
   *   "gauges": null,
   *   "counters": {
   *     "com.nordstrom.xrpc.server.Router.Active Connections": {
   *       "count": 1
   *     }
   *   },
   *   "histograms": null,
   *   "meters": {
   *     "Hard Rate Limits": {
   *       "count": 0,
   *       "m15_rate": 0.0,
   *       "m1_rate": 0.0,
   *       "m5_rate": 0.0,
   *       "mean_rate": 0.0,
   *       "units": "events/second"
   *     },
   *     "com.nordstrom.xrpc.server.Router.requests.Rate": {
   *       "count": 5,
   *       "m15_rate": 0.00441373963469717,
   *       "m1_rate": 0.060230960459854356,
   *       "m5_rate": 0.0130598243355337,
   *       "mean_rate": 0.09378130907818451,
   *       "units": "events/second"
   *     },
   *     "requests": {
   *       "count": 0,
   *       "m15_rate": 0.0,
   *       "m1_rate": 0.0,
   *       "m5_rate": 0.0,
   *       "mean_rate": 0.0,
   *       "units": "events/second"
   *     },
   *     "responseCodes.accepted": {
   *       "count": 0,
   *       "m15_rate": 0.0,
   *       "m1_rate": 0.0,
   *       "m5_rate": 0.0,
   *       "mean_rate": 0.0,
   *       "units": "events/second"
   *     },
   *     "responseCodes.badRequest": {
   *       "count": 0,
   *       "m15_rate": 0.0,
   *       "m1_rate": 0.0,
   *       "m5_rate": 0.0,
   *       "mean_rate": 0.0,
   *       "units": "events/second"
   *     },
   *     "responseCodes.created": {
   *       "count": 0,
   *       "m15_rate": 0.0,
   *       "m1_rate": 0.0,
   *       "m5_rate": 0.0,
   *       "mean_rate": 0.0,
   *       "units": "events/second"
   *     },
   *     "responseCodes.forbidden": {
   *       "count": 0,
   *       "m15_rate": 0.0,
   *       "m1_rate": 0.0,
   *       "m5_rate": 0.0,
   *       "mean_rate": 0.0,
   *       "units": "events/second"
   *     },
   *     "responseCodes.noContent": {
   *       "count": 0,
   *       "m15_rate": 0.0,
   *       "m1_rate": 0.0,
   *       "m5_rate": 0.0,
   *       "mean_rate": 0.0,
   *       "units": "events/second"
   *     },
   *     "responseCodes.notFound": {
   *       "count": 1,
   *       "m15_rate": 0.0011080303990206543,
   *       "m1_rate": 0.015991117074135343,
   *       "m5_rate": 0.0033057092356765017,
   *       "mean_rate": 0.01869797053449918,
   *       "units": "events/second"
   *     },
   *     "responseCodes.ok": {
   *       "count": 3,
   *       "m15_rate": 0.003305709235676515,
   *       "m1_rate": 0.04423984338571901,
   *       "m5_rate": 0.009754115099857198,
   *       "mean_rate": 0.056093859634458246,
   *       "units": "events/second"
   *     },
   *     "responseCodes.serverError": {
   *       "count": 0,
   *       "m15_rate": 0.0,
   *       "m1_rate": 0.0,
   *       "m5_rate": 0.0,
   *       "mean_rate": 0.0,
   *       "units": "events/second"
   *     },
   *     "responseCodes.tooManyRequests": {
   *       "count": 0,
   *       "m15_rate": 0.0,
   *       "m1_rate": 0.0,
   *       "m5_rate": 0.0,
   *       "mean_rate": 0.0,
   *       "units": "events/second"
   *     },
   *     "responseCodes.unauthorized": {
   *       "count": 0,
   *       "m15_rate": 0.0,
   *       "m1_rate": 0.0,
   *       "m5_rate": 0.0,
   *       "mean_rate": 0.0,
   *       "units": "events/second"
   *     }
   *   },
   *   "timers": {
   *     "Request Latency": {
   *       "count": 4,
   *       "max": 296.871107,
   *       "mean": 103.19818475091806,
   *       "min": 19.407526999999998,
   *       "p50": 29.858400999999997,
   *       "p75": 87.221846,
   *       "p95": 296.871107,
   *       "p98": 296.871107,
   *       "p99": 296.871107,
   *       "p999": 296.871107,
   *       "values": [
   *         19.407526999999998,
   *         29.858400999999997,
   *         87.221846,
   *         296.871107
   *       ],
   *       "stddev": 107.77184424066809,
   *       "m15_rate": 0.00441373963469717,
   *       "m1_rate": 0.060230960459854356,
   *       "m5_rate": 0.0130598243355337,
   *       "mean_rate": 0.07503044496250141,
   *       "duration_units": "milliseconds",
   *       "rate_units": "calls/second"
   *     }
   *   }
   * }
   * }</pre>
   */
  // CHECKSTYLE:ON
  public static Handler metricsHandler(MetricRegistry metrics, ObjectMapper mapper) {
    Preconditions.checkArgument(metrics != null, "metrics may not be null");
    Preconditions.checkArgument(mapper != null, "mapper may not be null");
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

  /**
   * Simple ping, indicates service is up.
   *
   * @return xrpcRequest - `200 OK` with `PONG`
   */
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

  /**
   * Run registered health checks.
   *
   * @param healthCheckRegistry Health Check Registry
   * @param mapper ObjectMapper
   * @return xrpcRequest with `200 OK` and status of registered health checks
   *     <pre>{@code
   * {
   *   "simple": {
   *     "healthy": true,
   *     "message": null,
   *     "error": null,
   *     "details": null,
   *     "timestamp": "2018-02-01T12:12:22.033-0800"
   *   }
   * }
   *
   * }</pre>
   */
  public static Handler healthCheckHandler(
      HealthCheckRegistry healthCheckRegistry, ObjectMapper mapper) {
    Preconditions.checkArgument(healthCheckRegistry != null, "healthCheckRegistry may not be null");
    Preconditions.checkArgument(mapper != null, "mapper may not be null");

    return xrpcRequest -> {
      SortedMap<String, HealthCheck.Result> healthChecks = healthCheckRegistry.runHealthChecks();
      return Recipes.newResponseOk(
          xrpcRequest
              .getAlloc()
              .directBuffer()
              .writeBytes(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(healthChecks)),
          Recipes.ContentType.Application_Json);
    };
  }

  /**
   * Readiness handler, for use with kubernetes or ELB healthchecks.
   *
   * @return xrpcRequest `200 OK` with `OK`
   */
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
