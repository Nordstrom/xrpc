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

package com.nordstrom.xrpc.server.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class RouteTest {
  @Test
  public void testMatches_noVariables() {
    Route route = Route.build("/api/people");

    assertTrue(route.matches("/api/people"), "exact path match failed");
    assertTrue(route.matches("/api/people/"), "trailing slash should be allowed");
    assertFalse(route.matches("/api"), "subpath match should've failed");
    assertFalse(route.matches("/api/people/jeff"), "prefix match should've failed");
  }

  @Test
  public void testMatches_singleSimpleVariable() {
    Route route = Route.build("/api/people/{person}");

    assertTrue(route.matches("/api/people/jeff"), "path match failed with variable set");
    assertTrue(route.matches("/api/people/jeff/"), "trailing slash should be allowed");
    assertFalse(route.matches("/api/people/"), "subpath match should've failed");
    assertFalse(route.matches("/api/people/jeff/rose"), "prefix match should've failed");
  }

  @Test
  public void testMatches_multipleVariables() {
    Route route = Route.build("/api/location/{country}/{city}");
    String path = "/api/location/usa/seattle";
    assertTrue(route.matches(path), "failed to match path " + path);
    assertFalse(route.matches("/api/location//seattle"), "matched empty path segment");
  }

  @Test
  public void testMatches_customPattern() {
    Route route = Route.build("/api/person/{age:\\d+}/all");
    String path = "/api/person/45/all";
    assertTrue(route.matches(path), "failed to match path " + path);
    assertFalse(route.matches("/api/person/abc/all"), "matched non-numeric path segment");
  }

  @Test
  public void testMatches_patternEscaped() {
    Route route = Route.build("/api/.*");
    assertTrue(route.matches("/api/.*"), "failed to match exact path");
    assertFalse(route.matches("/api/person"), "matched patterned path");
  }

  @Test
  public void testGroups_singleVariable() {
    Route route = Route.build("/api/people/{person}");
    Map<String, String> groups = route.groups("/api/people/jeff");
    assertNotNull(groups, "null groups from route.groups");
    assertEquals(1, groups.size(), "should have exactly one group");
    assertEquals("jeff", groups.get("person"), "variable should bind correctly");
  }

  @Test
  public void testGroups_multipleVariables() {
    Route route = Route.build("/api/location/{country}/{city}");
    String path = "/api/location/usa/seattle";
    Map<String, String> groups = route.groups("/api/location/usa/seattle");
    assertNotNull(groups, "null groups from route.groups");
    assertEquals(2, groups.size(), "should have exactly two groups");
    assertEquals("usa", groups.get("country"), "country variable should bind correctly");
    assertEquals("seattle", groups.get("city"), "city variable should bind correctly");
  }
}
