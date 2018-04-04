package com.nordstrom.xrpc.proto;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Accessors;

@Builder
@Value
@Accessors(fluent = true)
class Service {
  String fileName;
  String protoName;
  String packageName;
  String className;
  String serviceName;
  boolean deprecated;
  String[] javaDoc;
  @Singular ImmutableList<Method> methods;

  public String absoluteFileName() {
    String dir = packageName().replace('.', '/');
    if (Strings.isNullOrEmpty(dir)) {
      return fileName();
    } else {
      return dir + "/" + fileName();
    }
  }
}
