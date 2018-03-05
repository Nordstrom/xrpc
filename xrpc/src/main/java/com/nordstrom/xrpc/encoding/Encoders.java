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
import lombok.Builder;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

/** Holds a set of Encoder objects each registered to do encoding for a given content type. */
@Slf4j
public class Encoders extends MediaTypeCodecs<Encoder> {
  private static final Pattern CONTENT_TYPE_DELIMITER = Pattern.compile(" *, *");
  private static final Pattern PARAM_DELIMITER = Pattern.compile(" *; *");

  @Builder
  private Encoders(CharSequence defaultContentType, @Singular ImmutableList<Encoder> encoders) {
    super(defaultContentType, encoders);
  }

  /**
   * Find an Encoder based on an Accept header value.
   *
   * @param accept accept header value.
   * @return best encoder for the given accept header. If null, returns defaultValue().
   */
  public Encoder acceptedEncoder(CharSequence accept) {
    if (accept == null) {
      return defaultValue();
    }

    // TODO (AD): Consider LRU cache of accepts to encoders.
    String[] contentTypes = CONTENT_TYPE_DELIMITER.split(accept);
    for (String contentType : contentTypes) {
      String[] parts = PARAM_DELIMITER.split(contentType);
      Encoder encoder = get(parts[0]);
      if (encoder != null) {
        return encoder;
      }
    }
    return defaultValue();
  }
}
