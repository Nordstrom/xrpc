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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Holds a set of Decoders each registered to do decoding for a given content type. */
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Decoders {
  private final Decoder defaultDecoder;
  private final ImmutableMap<CharSequence, Decoder> decoders;

  /**
   * Find a Decoder based on an Content-Type header value.
   *
   * @param contentType content type header value. If null or not registered the default decoder
   *     will be returned
   * @return best Decoder for the given content type header
   */
  public Decoder decoder(CharSequence contentType) {
    if (contentType == null) {
      return defaultDecoder;
    }
    // TODO (AD): Handle charset see:
    // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type
    Decoder decoder = decoders.get(contentType.toString());
    if (decoder != null) {
      return decoder;
    }
    return defaultDecoder;
  }

  /** Get Decoders builder. */
  public static Builder builder() {
    return new Builder();
  }

  /** Decoders Builder. */
  public static class Builder {
    private CharSequence defaultContentType;
    private final ImmutableMap.Builder<CharSequence, Decoder> builder = ImmutableMap.builder();

    private Builder() {}

    /** Set default_content_type. */
    public Builder defaultContentType(CharSequence defaultContentType) {
      this.defaultContentType = defaultContentType;
      return this;
    }

    /** Register a Decoder. */
    public Builder decoder(Decoder decoder) {
      builder.put(decoder.contentType(), decoder);
      return this;
    }

    /** Build a Decoders instance. */
    public Decoders build() {
      ImmutableMap<CharSequence, Decoder> decoders = builder.build();
      Decoder defaultDecoder = decoders.get(defaultContentType);
      Preconditions.checkNotNull(
          defaultDecoder,
          String.format("default_content_type %s has no registered Decoder", defaultContentType));

      return new Decoders(defaultDecoder, decoders);
    }
  }
}
