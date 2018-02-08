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

package com.nordstrom.xrpc.demos.people;

import com.codahale.metrics.health.HealthCheck;
import com.nordstrom.xrpc.server.Router;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Application {
  public static void main(String[] args) {
    // Load application config from jar resources. The 'load' method below also allows supports
    // overrides from environment variables.
    Config config = ConfigFactory.load("demo.conf");

    // Build your router. This overrides the default configuration with values from
    // src/main/resources/demo.conf.
    Router router = new Router(config);

    // Add handlers for /people routes
    new PeopleRoutes(router);

    // Add a service specific health check
    router.addHealthCheck("simple", new SimpleHealthCheck());

    try {
      // Fire away
      router.listenAndServe();
    } catch (IOException e) {
      log.error("Failed to start people server", e);
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
