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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * A single route in an application, possibly with variable path elements. An xrpc application is
 * built out of {route, handler} pairs.
 */
@Slf4j
public class Route {
  /**
   * Pattern to extract a variable from a URL path. This captures the variable name and any
   * associated regular expression.
   */
  private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)(?::([^\\{\\}]+))?\\}");

  /**
   * Compiled pattern that matches a path, and extracts named groups for each named wildcard
   * segment.
   */
  private final Pattern pathPattern;

  private final List<String> keywords;

  private Route(Pattern pathPattern, List<String> keywords) {
    this.pathPattern = pathPattern;
    this.keywords = keywords;
  }

  /**
   * Build a route from the given path pattern. This pattern will be exact-matched, expect if there
   * are variables matching "{identifer}" or "{identifier:regex}". All text matched by the
   * variable's regular expression will be available to the handler; if "regex" isn't provided, the
   * regular expression "[^/]+" will be used (at least one non-slash character).
   */
  public static Route build(String pathPattern) {
    // Keywords pulled from the path pattern (items matching KEYWORD_PATTERN).
    List<String> keywords = new ArrayList<>();
    StringBuilder pathRegex = new StringBuilder();
    Matcher variableMatcher = VARIABLE_PATTERN.matcher(pathPattern);
    int prevEnd = 0;
    while (variableMatcher.find()) {
      // Add the keyword to our list.
      String keywordName = variableMatcher.group(1);
      keywords.add(keywordName);

      // Extract the regex for the variable.
      String variableRegex = variableMatcher.group(2);
      if (variableRegex == null) {
        variableRegex = "[^/]+";
      }

      // Add a named capture group to the pattern for extraction.
      pathRegex.append(Pattern.quote(pathPattern.substring(prevEnd, variableMatcher.start())));
      pathRegex.append("(?<").append(keywordName).append('>').append(variableRegex).append(')');
      prevEnd = variableMatcher.end();
    }
    pathRegex.append(Pattern.quote(pathPattern.substring(prevEnd, pathPattern.length())));
    // Always allow paths to end in a slash, for usability / sanity.
    if (!pathPattern.endsWith("/")) {
      pathRegex.append("/?");
    }

    Pattern compiledPattern = Pattern.compile(pathRegex.toString());

    return new Route(compiledPattern, keywords);
  }

  /** @return true if this route matches the given URL path */
  public boolean matches(String path) {
    return pathPattern.matcher(path).matches();
  }

  /**
   * @return the groups this captured from the given path, if any; or null if this route doesn't
   *     match the path
   */
  public Map<String, String> groups(String path) {
    Matcher matcher = pathPattern.matcher(path);
    if (matcher.matches()) {
      Map<String, String> groups = new HashMap<>();
      for (String keyword : keywords) {
        groups.put(keyword, matcher.group(keyword));
      }
      return groups;
    } else {
      return null;
    }
  }
}
