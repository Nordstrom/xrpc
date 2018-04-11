package com.nordstrom.xrpc.proto;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.html.HtmlEscapers;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location;
import java.util.List;
import lombok.val;

public class ModelBuilder {
  private static final int METHOD_NUMBER_OF_PATHS = 4;
  private static final String CLASS_PREFIX = "";
  private static final String CLASS_SUFFIX = "";
  private static final String SERVICE_JAVADOC_PREFIX = "  ";
  private static final String METHOD_JAVADOC_PREFIX = "    ";

  private final ProtoTypeMap typeMap;

  public ModelBuilder(ProtoTypeMap typeMap) {
    this.typeMap = typeMap;
  }

  public ImmutableList<Service> buildServices(ImmutableList<FileDescriptorProto> protos) {
    return protos
        .stream()
        .flatMap(
            fileProto -> {
              List<Location> locations = fileProto.getSourceCodeInfo().getLocationList();
              return locations
                  .stream()
                  .filter(
                      location ->
                          location.getPathCount() == 2
                              && location.getPath(0) == FileDescriptorProto.SERVICE_FIELD_NUMBER)
                  .map(location -> buildService(fileProto, location, locations));
            })
        .collect(collectingAndThen(toList(), ImmutableList::copyOf));
  }

  private Service buildService(
      FileDescriptorProto fileProto, Location serviceLocation, List<Location> locations) {

    val serviceNumber = serviceLocation.getPath(1);
    val serviceProto = fileProto.getService(serviceNumber);
    val packageName = extractPackageName(fileProto);
    val methods =
        locations
            .stream()
            .filter(
                location ->
                    location.getPathCount() == METHOD_NUMBER_OF_PATHS
                        && location.getPath(0) == FileDescriptorProto.SERVICE_FIELD_NUMBER
                        && location.getPath(1) == serviceNumber
                        && location.getPath(2) == ServiceDescriptorProto.METHOD_FIELD_NUMBER)
            .map(location -> buildMethod(packageName, serviceProto, location))
            .collect(toList());

    return Service.builder()
        .fileName(String.format("%s%s%s.java", CLASS_PREFIX, serviceProto.getName(), CLASS_SUFFIX))
        .className(String.format("%s%s%s", CLASS_PREFIX, serviceProto.getName(), CLASS_SUFFIX))
        .serviceName(serviceProto.getName())
        .deprecated(serviceProto.getOptions() != null && serviceProto.getOptions().getDeprecated())
        .comments(getJavaDoc(getComments(serviceLocation)))
        .protoName(fileProto.getName())
        .packageName(packageName)
        .methods(methods)
        .build();
  }

  private Method buildMethod(
      String packageName, ServiceDescriptorProto serviceProto, Location methodLocation) {
    // TODO (AD): Error on method streaming
    val methodNumber = methodLocation.getPath(METHOD_NUMBER_OF_PATHS - 1);
    val methodProto = serviceProto.getMethod(methodNumber);
    return Method.builder()
        .packageName(packageName)
        .serviceName(serviceProto.getName())
        .methodName(lowerCaseFirst(methodProto.getName()))
        .inputType(typeMap.toJavaTypeName(methodProto.getInputType()))
        .outputType(typeMap.toJavaTypeName(methodProto.getOutputType()))
        .deprecated(methodProto.getOptions() != null && methodProto.getOptions().getDeprecated())
        .methodNumber(methodNumber)
        .comments(getJavaDoc(methodLocation))
        .build();
  }

  private String extractPackageName(FileDescriptorProto proto) {
    FileOptions options = proto.getOptions();
    if (options != null) {
      String javaPackage = options.getJavaPackage();
      if (!Strings.isNullOrEmpty(javaPackage)) {
        return javaPackage;
      }
    }

    return Strings.nullToEmpty(proto.getPackage());
  }

  private static String lowerCaseFirst(String s) {
    return Character.toLowerCase(s.charAt(0)) + s.substring(1);
  }

  private static String getComments(Location location) {
    return location.getLeadingComments().isEmpty()
        ? location.getTrailingComments()
        : location.getLeadingComments();
  }

  private static String[] getJavaDoc(Location location) {
    return getJavaDoc(getComments(location));
  }

  private static String[] getJavaDoc(String comments) {
    if (comments.isEmpty()) {
      return null;
    }
    // TODO (AD): Use system dependent line separator
    return HtmlEscapers.htmlEscaper().escape(comments).split("\n");
  }
}
