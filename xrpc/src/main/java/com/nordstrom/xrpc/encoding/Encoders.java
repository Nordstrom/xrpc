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

package com.nordstrom.xrpc.encoding;

import com.google.common.collect.ImmutableList;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Encoders {
  private static final Pattern ACCEPT_SPLIT_PATTERN = Pattern.compile(",[ ]*");
  private final ContentTypeEncoder defaultEncoder;
  private final ImmutableList<ContentTypeEncoder> encoders;

  public ContentTypeEncoder acceptedEncoder(CharSequence accept) {
    if (accept == null) {
      return defaultEncoder;
    }
    String[] contentTypes = ACCEPT_SPLIT_PATTERN.split(accept);
    for (String contentType : contentTypes) {
      // TODO (AD): Handle q-factor weighting see:
      // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept
      for (ContentTypeEncoder encoder : encoders) {
        if (encoder.contentType().equals(contentType)) {
          return encoder;
        }
      }
    }
    return defaultEncoder;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String defaultContentType;
    private final ImmutableList.Builder<ContentTypeEncoder> builder = ImmutableList.builder();

    private Builder() {}

    public Builder defaultContentType(String defaultContentType) {
      this.defaultContentType = defaultContentType;
      return this;
    }

    public Builder encoder(String contentType, Encoder encoder) {
      builder.add(new ContentTypeEncoder(contentType, encoder));
      return this;
    }

    public Encoders build() {
      ImmutableList<ContentTypeEncoder> encoders = builder.build();
      ContentTypeEncoder defaultEncoder =
          encoders
              .stream()
              .filter(e -> e.contentType().equals(defaultContentType))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          String.format(
                              "default_content_type %s has no registered Encoder",
                              defaultContentType)));

      return new Encoders(defaultEncoder, encoders);
    }
  }
}
