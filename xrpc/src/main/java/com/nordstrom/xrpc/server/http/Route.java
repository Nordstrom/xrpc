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

package com.nordstrom.xrpc.server.http;

import com.nordstrom.xrpc.server.Handler;
import io.netty.handler.codec.http.HttpMethod;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Represents an HTTP route with method, path (pattern), and the handler handling requests for this
 * route.
 */
@Value
@Accessors(fluent = true)
public class Route {
  HttpMethod method;
  RoutePath path;
  Handler handler;
}
