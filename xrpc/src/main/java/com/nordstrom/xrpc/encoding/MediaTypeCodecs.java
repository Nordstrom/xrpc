package com.nordstrom.xrpc.encoding;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.netty.util.AsciiString;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Base collection of MediaTypable objects. Consolidates the logic of lookup up an item based on
 * CharSequence mediaTypes. It also provides a defaultValue and appropriate getOrDefault.
 *
 * <p>Intended for a small set of objects as lookup is O(N) where N = Number of supported Media
 * Types.
 *
 * @param <T> MediaTypeCodec item type
 */
@Accessors(fluent = true)
class MediaTypeCodecs<T extends MediaTypeCodec> {
  @Getter private final T defaultValue;
  private final ImmutableList<T> collection;

  MediaTypeCodecs(CharSequence defaultContentType, ImmutableList<T> collection) {
    this.collection = collection;
    this.defaultValue = get(defaultContentType);

    Preconditions.checkNotNull(
        this.defaultValue,
        String.format(
            "default_content_type %s has no registered Encoder or Decoder", defaultContentType));
  }

  protected T get(CharSequence contentType) {
    for (T item : collection) {
      if (AsciiString.contentEquals(contentType, item.mediaType())) {
        return item;
      }
    }
    return null;
  }

  protected T getOrDefault(CharSequence contentType) {
    T item = get(contentType);
    return item == null ? defaultValue : item;
  }
}
