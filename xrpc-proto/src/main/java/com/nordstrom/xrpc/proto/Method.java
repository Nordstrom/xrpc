package com.nordstrom.xrpc.proto;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

@Builder
@Value
@Accessors(fluent = true)
class Method {
  String packageName;
  String serviceName;
  String methodName;
  String inputType;
  String outputType;
  boolean deprecated;
  int methodNumber;
  String[] comments;

  // This method mimics the upper-casing method ogf gRPC to ensure compatibility
  // See
  // https://github.com/grpc/grpc-java/blob/v1.8.0/compiler/src/java_plugin/cpp/java_generator.cpp#L58
  public String methodNameUpperUnderscore() {
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < methodName.length(); i++) {
      char c = methodName.charAt(i);
      s.append(Character.toUpperCase(c));
      if ((i < methodName.length() - 1)
          && Character.isLowerCase(c)
          && Character.isUpperCase(methodName.charAt(i + 1))) {
        s.append('_');
      }
    }
    return s.toString();
  }

  public String methodNamePascalCase() {
    String mn = methodName.replace("_", "");
    return String.valueOf(Character.toUpperCase(mn.charAt(0))) + mn.substring(1);
  }

  public String methodNameCamelCase() {
    String mn = methodName.replace("_", "");
    return String.valueOf(Character.toLowerCase(mn.charAt(0))) + mn.substring(1);
  }
}
