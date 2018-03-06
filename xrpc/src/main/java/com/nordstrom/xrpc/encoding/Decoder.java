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

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Interface for decoding a request ByteBuf into Object. */
public interface Decoder extends MediaTypeCodec {
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
   * Invoke a static method on the target class for decoding. This is used as an intermediate step
   * for decoding.
   *
   * @param clazz target Class
   * @param methodName name of method to invoke
   * @param types array of method argument types
   * @param args array of arguments to pass into the method
   * @param <InT> target Class type
   * @param <OutT> return type of invoked method
   * @return result of method invocation
   */
  @SuppressWarnings("unchecked")
  default <InT, OutT> OutT invoke(
      Class<InT> clazz, String methodName, Class<?>[] types, Object[] args) {
    try {
      // TODO (AD): add method memoization
      Method method = clazz.getMethod(methodName, types);
      // TODO (AD): for now we suppress unchecked cast warning.  should fix this.
      return (OutT) method.invoke(null, args);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new IllegalArgumentException(
          String.format("%s is not a valid Proto class: %s", clazz.getName(), e.getMessage()));
    } catch (InvocationTargetException te) {
      throw new RuntimeException(
          String.format(
              "Error decoding into %s: %s", clazz.getName(), te.getTargetException().getMessage()),
          te.getTargetException());
    }
  }
}
