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

/** Holds a set of Decoders each registered to do decoding for a given content type. */
@Slf4j
public class Decoders extends MediaTypeableCollection<Decoder> {
  private static final Pattern PARAM_DELIMITER = Pattern.compile(" *; *");

  @Builder
  private Decoders(CharSequence defaultContentType, @Singular ImmutableList<Decoder> decoders) {
    super(defaultContentType, decoders);
  }

  /**
   * Find a Decoder based on an Content-Type header value.
   *
   * @param contentType content type header value. If null or not registered the default decoder
   *     will be returned
   * @return best Decoder for the given content type header
   */
  public Decoder decoder(CharSequence contentType) {
    if (contentType == null) {
      return defaultValue();
    }
    String[] parts = PARAM_DELIMITER.split(contentType);
    return getOrDefault(parts[0]);
  }
}
