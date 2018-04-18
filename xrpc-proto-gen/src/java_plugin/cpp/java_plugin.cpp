// Generates Java gRPC service interface out of Protobuf IDL.
//
// This is a Proto2 compiler plugin.  See net/proto2/compiler/proto/plugin.proto
// and net/proto2/compiler/public/plugin.h for more information on plugins.

#include <memory>

#include "java_generator.h"
#include <google/protobuf/compiler/code_generator.h>
#include <google/protobuf/compiler/plugin.h>

static string JavaPackageToDir(const string &package_name) {
  string package_dir = package_name;
  for (char &i : package_dir) {
    if (i == '.') {
      i = '/';
    }
  }
  if (!package_dir.empty()) package_dir += "/";
  return package_dir;
}

class JavaXrpcGenerator : public google::protobuf::compiler::CodeGenerator {
 public:
  JavaXrpcGenerator() {}
  virtual ~JavaXrpcGenerator() {}

  virtual bool Generate(const google::protobuf::FileDescriptor *file,
                        const string &parameter,
                        google::protobuf::compiler::GeneratorContext *context,
                        string *error) const {
    std::vector<std::pair<string, string> > options;
    google::protobuf::compiler::ParseGeneratorParameter(parameter, &options);

    bool disable_version = false;

    string package_name = java_xrpc_generator::ServiceJavaPackage(file);
    string package_filename = JavaPackageToDir(package_name);
    for (int i = 0; i < file->service_count(); ++i) {
      const google::protobuf::ServiceDescriptor *service = file->service(i);
      string filename = package_filename
          + java_xrpc_generator::ServiceClassName(service) + ".java";
      std::unique_ptr<google::protobuf::io::ZeroCopyOutputStream> output(
          context->Open(filename));
      java_xrpc_generator::GenerateService(service, output.get());
    }
    return true;
  }
};

int main(int argc, char *argv[]) {
  JavaXrpcGenerator generator;
  return google::protobuf::compiler::PluginMain(argc, argv, &generator);
}
