package com.nordstrom.xrpc.proto;

import java.util.Arrays;

public class ServiceCodeBuilder {
  private static final String NL = System.getProperty("line.separator");

  private static final String I1 = NL + "  ";
  private static final String I2 = I1 + "  ";
  private static final String I3 = I2 + "  ";

  private final StringBuilder builder = new StringBuilder();

  public String buildService(Service service) {
    // Append package line
    builder.append("package ").append(service.packageName()).append(NL).append(NL);

    // Append imports
    builder
        .append("import com.nordstrom.xrpc.server.RouteBuilder;")
        .append("import com.nordstrom.xrpc.server.Routes;")
        .append("import com.nordstrom.xrpc.server.Service;");

    // Append interface java doc if it exists
    buildJavaDoc(service.javaDoc(), NL);

    // Append interface definition for service
    builder
        .append("public interface ")
        .append(service.className())
        .append(" extends Service {")
        .append(NL)
        .append(NL);

    // Append each method
    service.methods().forEach(this::buildMethod);

    buildRoutes(service);

    // Append interface closing brace
    builder.append(NL).append("}").append(NL);

    return builder.toString();
  }

  private void buildRoutes(Service service) {
    // Append routing logic similar to the following:
    //
    //  @Override
    //  default Routes routes() {
    //    RouteBuilder routes = new RouteBuilder();
    //
    //    routes.post("/package.Service/Method", request -> {
    //      InputType input = request.body(InputType.class);
    //      OutputType output = someMethod(input);
    //      return request.ok(output);
    //    });
    //
    //    return routes;
    //  }
    builder
        .append(I1)
        .append("@Override")
        .append(I1)
        .append("default Routes routes() {")
        .append(I2)
        .append("RouteBuilder routes = new RouteBuilder();")
        .append(NL);

    service.methods().forEach(this::buildRoute);
    builder.append("  }").append(NL);
  }

  private void buildMethod(Method method) {
    // Append method java doc if it exists
    buildJavaDoc(method.javaDoc(), I1);

    // Append method definition
    builder
        .append(I1)
        .append(method.outputType())
        .append(" ")
        .append(method.methodNameCamelCase())
        .append("(")
        .append(method.inputType())
        .append(" input);")
        .append(NL);
  }

  private void buildRoute(Method method) {
    // Append similar to the following:
    //
    //    routes.post("/package.Service/Method", request -> {
    //      InputType input = request.body(InputType.class);
    //      OutputType output = foo(input);
    //      return request.ok(output);
    //    });
    builder
        .append(I2)
        .append("routes.post(\"/")
        .append(method.packageName())
        .append('.')
        .append(method.serviceName())
        .append('/')
        .append(method.methodName())
        .append("\", request -> {")
        .append(I3)
        .append(method.inputType())
        .append(" input = request.body(")
        .append(method.inputType())
        .append(".class);")
        .append(I3)
        .append(method.outputType())
        .append(" output = ")
        .append(method.methodNameCamelCase())
        .append("(input);")
        .append(I3)
        .append("return request.ok(output);")
        .append(I2)
        .append("});")
        .append(NL);
  }

  private void buildJavaDoc(String[] comments, String prefix) {
    if (comments == null) {
      return;
    }
    builder.append(prefix).append("/**").append(prefix).append(" * <pre>");

    Arrays.stream(comments).forEach(line -> builder.append(prefix).append(" * ").append(line));

    builder.append(prefix).append(" * <pre>").append(prefix).append(" */");
  }
}
