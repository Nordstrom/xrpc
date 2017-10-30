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

package com.nordstrom.xrpc.http;

import io.netty.handler.codec.http.HttpRequest;
import java.util.Map;
import lombok.Getter;

/** Request context. */
public class Context {
  /** The request to handle. */
  @Getter private final HttpRequest request;
  /** The variables captured from the route path. */
  private final Map<String, String> groups;

  public Context(HttpRequest request, Map<String, String> groups) {
    this.request = request;
    this.groups = groups;
  }

  /** Returns the variable with the given name, or null if that variable doesn't exist. */
  public String variable(String name) {
    return groups.get(name);
  }
}
