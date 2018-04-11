package com.nordstrom.xrpc.proto;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File;
import java.io.IOException;
import java.util.List;
import lombok.val;

public class ProtoPlugin {

  public static void main(String[] args) {
    try {
      // Parse the input stream to extract the generator request
      byte[] generatorRequestBytes = ByteStreams.toByteArray(System.in);
      val request = CodeGeneratorRequest.parseFrom(generatorRequestBytes);

      val generator = new ProtoPlugin();
      CodeGeneratorResponse response = generator.generateResponse(request);
      response.writeTo(System.out);

    } catch (Exception ex) {
      try {
        CodeGeneratorResponse.newBuilder().setError(ex.getMessage()).build().writeTo(System.out);
      } catch (IOException ex2) {
        abort(ex2);
      }
    } catch (Throwable ex) { // Catch all the things!
      abort(ex);
    }
  }

  private static void abort(Throwable ex) {
    ex.printStackTrace(System.err);
    System.exit(1);
  }

  public CodeGeneratorResponse generateResponse(CodeGeneratorRequest request) {
    List<File> outputFiles = generateFiles(request);

    // Send the files back to protoc
    return CodeGeneratorResponse.newBuilder().addAllFile(outputFiles).build();
  }

  public List<CodeGeneratorResponse.File> generateFiles(CodeGeneratorRequest request) {
    final ProtoTypeMap typeMap = ProtoTypeMap.of(request.getProtoFileList());

    ImmutableList<FileDescriptorProto> protosToGenerate =
        request
            .getProtoFileList()
            .stream()
            .filter(protoFile -> request.getFileToGenerateList().contains(protoFile.getName()))
            .collect(collectingAndThen(toList(), ImmutableList::copyOf));

    val servicesBuilder = new ModelBuilder(typeMap);
    val services = servicesBuilder.buildServices(protosToGenerate);
    return generateFiles(services);
  }

  private List<CodeGeneratorResponse.File> generateFiles(List<Service> services) {
    return services.stream().map(this::buildFile).collect(toList());
  }

  private CodeGeneratorResponse.File buildFile(Service service) {
    val builder = new CodeBuilder();
    val content = builder.buildService(service);
    return CodeGeneratorResponse.File.newBuilder()
        .setName(service.absoluteFileName())
        .setContent(content)
        .build();
  }
}
