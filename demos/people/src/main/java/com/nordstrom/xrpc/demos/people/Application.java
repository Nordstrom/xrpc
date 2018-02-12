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
import com.nordstrom.xrpc.server.Routes;
import com.nordstrom.xrpc.server.Server;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Application {
  private final Server server;

  public Application(Config config) {
    // Build your router. This overrides the default configuration with values from
    // src/main/resources/demo.conf.
    Routes routes = new Routes();
    this.server = new Server(config, routes);

    // Add handlers for /people routes
    new PeopleRoutes(routes);

    // Add a service specific health check
    this.server.addHealthCheck(
        "simple",
        new HealthCheck() {
          @Override
          protected Result check() {
            System.out.println("Health Check Ran");
            return Result.healthy();
          }
        });
  }

  public void start() throws IOException {
    server.listenAndServe();
  }

  public void stop() {
    server.shutdown();
  }

  public static void main(String[] args) {
    // Load application config from jar resources. The 'load' method below also allows supports
    // overrides from environment variables.
    Config config = ConfigFactory.load("demo.conf");

    Application app = new Application(config);

    try {
      // Fire away
      app.start();
    } catch (IOException e) {
      log.error("Failed to start people server", e);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
  }
}
