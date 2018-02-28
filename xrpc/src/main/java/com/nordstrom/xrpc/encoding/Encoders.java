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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Holds a set of Encoder objects each registered to do encoding for a given content type. */
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Encoders {
  private static final Pattern ACCEPT_SPLIT_PATTERN = Pattern.compile(", *");
  private final Encoder defaultEncoder;
  private final ImmutableMap<CharSequence, Encoder> encoders;

  /**
   * Find an Encoder based on an Accept header value.
   *
   * @param accept accept header value.
   * @return best encoder for the given accept header
   */
  public Encoder acceptedEncoder(CharSequence accept) {
    if (accept == null) {
      return defaultEncoder;
    }

    // TODO (AD): Consider LRU cache of accepts to encoders.
    String[] contentTypes = ACCEPT_SPLIT_PATTERN.split(accept);
    for (String contentType : contentTypes) {
      // TODO (AD): Handle q-factor weighting see:
      // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept
      Encoder encoder = encoders.get(contentType);
      if (encoder != null) {
        return encoder;
      }
    }
    return defaultEncoder;
  }

  /** Get Encoders builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** Encoders Builder. */
  public static class Builder {
    private CharSequence defaultContentType;
    private final ImmutableMap.Builder<CharSequence, Encoder> builder = ImmutableMap.builder();

    private Builder() {}

    /** Set default_content_type. */
    public Builder defaultContentType(String defaultContentType) {
      this.defaultContentType = defaultContentType;
      return this;
    }

    /** Register an Encoder. */
    public Builder encoder(Encoder encoder) {
      builder.put(encoder.contentType(), encoder);
      return this;
    }

    /** Build an Encoders instance. */
    public Encoders build() {
      ImmutableMap<CharSequence, Encoder> encoders = builder.build();
      Encoder defaultEncoder = encoders.get(defaultContentType);

      Preconditions.checkNotNull(
          defaultEncoder,
          String.format("default_content_type %s has no registered Encoder", defaultContentType));

      return new Encoders(defaultEncoder, encoders);
    }
  }
}
