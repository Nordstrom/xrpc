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

import com.google.protobuf.MessageLite;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/** Interface for decoding a request ByteBuf into Object. */
public interface Decoder extends MediaTypeCodec {
  /** Cached protobuf MessageLite instances keyed by Class. */
  // TODO (AD): this is not optimal design -- mutable shared state.
  // Consider design that makes this immutable.
  ConcurrentHashMap<Class<?>, MessageLite> protoDefaultInstances = new ConcurrentHashMap<>();

  /**
   * Decode a request body to an object of designated Class type.
   *
   * @param body current http request body
   * @param contentType content type header
   * @param clazz target class for decoding
   * @return object of type clazz
   */
  <T> T decode(ByteBuf body, CharSequence contentType, Class<T> clazz) throws IOException;

  /**
   * Get a protobuf Message instance based on Class. This lazily caches instances as it reflectively
   * gets them.
   *
   * @param clazz proto generated class for which we get the default instance
   * @return default instance of the given Class
   */
  default MessageLite protoDefaultInstance(Class<?> clazz) {
    MessageLite message = protoDefaultInstances.get(clazz);
    if (message != null) {
      return message;
    }
    try {
      Method method = clazz.getMethod("getDefaultInstance");
      message = (MessageLite) method.invoke(null);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalArgumentException(
          String.format("%s is not a valid Class: %s", clazz.getName(), e.getMessage()));
    }
    protoDefaultInstances.put(clazz, message);
    return message;
  }
}
