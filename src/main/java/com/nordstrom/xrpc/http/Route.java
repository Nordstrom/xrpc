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

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Route {
  private static final Pattern keywordPattern = Pattern.compile("(:\\w+)");
  private final Pattern pathPattern;
  private final List<String> keywords;

  private Route(Pattern path, List<String> keywords) {
    this.pathPattern = path;
    this.keywords = keywords;
  }

  public static Pattern compile(String pattern, List<String> keywords) {
    StringBuilder regexPattern = new StringBuilder();

    if (pattern.equals("/")) {
      regexPattern.append("/");
    } else {
      final String[] segments = pattern.split("/");

      for (String segment : segments) {
        if (!segment.equals("")) {
          regexPattern.append("/");
          if (keywordPattern.matcher(segment).matches()) {
            String keyword = segment.substring(1);
            regexPattern.append("(?<").append(keyword).append(">[^/]*)");
            keywords.add(keyword);
          } else {
            regexPattern.append(segment);
          }
        }
      }
    }
    regexPattern.append("[/]?");

    return Pattern.compile(regexPattern.toString());
  }

  public static Route build(String pattern) {
    List<String> keywords = new ArrayList<>();
    return new Route(compile(pattern, keywords), keywords);
  }

  public Pattern pathPattern() {
    return pathPattern;
  }

  public boolean matches(String path) {
    return pathPattern.matcher(path).matches();
  }

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
