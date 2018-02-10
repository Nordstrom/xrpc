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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpMethod;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Tests for Routes and CompiledRoutes. */
class RoutesTest {
  /** Tests basic routing features. */
  @Test
  public void trivialRouteMatch() throws Exception {
    Handler mockHandler = mock(Handler.class);
    MetricRegistry registry = new MetricRegistry();

    CompiledRoutes routes = new Routes().get("/get", mockHandler).compile(registry);

    Optional<CompiledRoutes.Match> putMatch = routes.match("/get", HttpMethod.PUT);
    assertEquals(Optional.empty(), putMatch, "expected no match for PUT");
    Optional<CompiledRoutes.Match> missingMatch = routes.match("/ge", HttpMethod.GET);
    assertEquals(Optional.empty(), missingMatch, "expected no match for bad path");

    Optional<CompiledRoutes.Match> getMatch = routes.match("/get", HttpMethod.GET);
    assertTrue(getMatch.isPresent(), "expected match to be returned for GET");

    XrpcRequest mockRequest = mock(XrpcRequest.class);
    getMatch.get().getHandler().handle(mockRequest);
    verify(mockHandler, times(1)).handle(mockRequest);
    assertEquals(new HashMap<>(), getMatch.get().getGroups());
    assertEquals(1L, registry.meter(MetricRegistry.name("routes", "GET", "/get")).getCount());
    assertEquals(0L, registry.meter(MetricRegistry.name("routes", "PUT", "/get")).getCount());
  }

  /** Tests that two handlers on the same path with different methods get handled correctly. */
  @Test
  public void samePathDifferentMethods() throws Exception {
    Handler mockGetHandler = mock(Handler.class);
    Handler mockPostHandler = mock(Handler.class);
    MetricRegistry registry = new MetricRegistry();

    CompiledRoutes routes =
        new Routes().get("/path", mockGetHandler).post("/path", mockPostHandler).compile(registry);

    Optional<CompiledRoutes.Match> getMatch = routes.match("/path", HttpMethod.GET);
    Optional<CompiledRoutes.Match> postMatch = routes.match("/path", HttpMethod.POST);
    assertTrue(getMatch.isPresent(), "expected match to be returned for GET");
    assertTrue(postMatch.isPresent(), "expected match to be returned for POST");

    XrpcRequest mockGetRequest = mock(XrpcRequest.class);
    getMatch.get().getHandler().handle(mockGetRequest);
    verify(mockGetHandler, times(1)).handle(mockGetRequest);
    assertEquals(new HashMap<>(), getMatch.get().getGroups());
    assertEquals(1L, registry.meter(MetricRegistry.name("routes", "GET", "/path")).getCount());

    XrpcRequest mockPostRequest = mock(XrpcRequest.class);
    postMatch.get().getHandler().handle(mockPostRequest);
    verify(mockPostHandler, times(1)).handle(mockPostRequest);
    assertEquals(new HashMap<>(), postMatch.get().getGroups());
    assertEquals(1L, registry.meter(MetricRegistry.name("routes", "POST", "/path")).getCount());
  }

  /** Tests that groups are passed through correctly when captured. */
  @Test
  public void groupsCapture() throws Exception {
    Handler mockGroupsHandler = mock(Handler.class);
    Handler mockGetHandler = mock(Handler.class);
    MetricRegistry registry = new MetricRegistry();

    CompiledRoutes routes =
        new Routes()
            .get("/path/{grp}", mockGroupsHandler)
            .get("/path", mockGetHandler)
            .compile(registry);

    Optional<CompiledRoutes.Match> match = routes.match("/path/subpath", HttpMethod.GET);
    assertTrue(match.isPresent(), "expected match to be returned for GET");

    XrpcRequest mockGroupsRequest = mock(XrpcRequest.class);
    match.get().getHandler().handle(mockGroupsRequest);
    verify(mockGetHandler, never()).handle(any());
    verify(mockGroupsHandler, times(1)).handle(mockGroupsRequest);
    assertEquals(ImmutableMap.of("grp", "subpath"), match.get().getGroups());
    assertEquals(
        1L, registry.meter(MetricRegistry.name("routes", "GET", "/path/{grp}")).getCount());
  }

  /** Adding the same path+method should throw an exception. */
  @Test
  public void duplicateHandlerThrows() {
    Routes routes = new Routes().get("/twice", request -> null);
    assertThrows(IllegalArgumentException.class, () -> routes.get("/twice", request -> null));
  }
}
