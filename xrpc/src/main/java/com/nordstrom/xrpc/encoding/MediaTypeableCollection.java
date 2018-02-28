package com.nordstrom.xrpc.encoding;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.netty.util.AsciiString;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
class MediaTypeableCollection<T extends MediaTypeable> {
  @Getter private final T defaultValue;
  private final ImmutableList<T> collection;

  MediaTypeableCollection(CharSequence defaultContentType, ImmutableList<T> collection) {
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
