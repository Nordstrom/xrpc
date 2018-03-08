package com.nordstrom.xrpc.encoding;

import com.google.protobuf.MessageLite;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/** Cache for default instances of protobuf generated classes. */
public class ProtoDefaultInstances {
  private ConcurrentHashMap<Class<?>, MessageLite> instances = new ConcurrentHashMap<>();

  /**
   * Get a protobuf Message instance based on Class. This lazily caches instances as it reflectively
   * gets them.
   *
   * @param clazz proto generated class for which we get the default instance
   * @return default instance of the given Class
   */
  public MessageLite get(Class<?> clazz) {
    MessageLite message = instances.get(clazz);
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
    instances.put(clazz, message);
    return message;
  }
}
