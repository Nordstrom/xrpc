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
import io.netty.handler.codec.http.HttpMethod;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class JmhCompiledRoutesBenchmark {
  CompiledRoutes compiledRoutes;

  @Setup
  public void setupRouter() {
    Handler handler = request -> null;
    MetricRegistry metricRegistry = new MetricRegistry();
    RouteBuilder routes = new RouteBuilder();
    routes.get("/foo", handler);
    compiledRoutes = routes.compile(metricRegistry);
  }

  @BenchmarkMode({Mode.AverageTime, Mode.Throughput})
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  @Benchmark
  public void basicMatch() throws IOException {
    compiledRoutes.match("/foo", HttpMethod.GET);
  }
}
