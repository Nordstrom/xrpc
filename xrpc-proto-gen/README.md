xRPC Java Codegen Plugin for Protobuf Compiler
==============================================

This generates the Java interfaces out of the service definition from a
`.proto` file. It works with the Protobuf Compiler (``protoc``).

## System requirement

* Linux, Mac OS X with Clang, or Windows with MSYS2
* Java 8 or up
* [Protobuf](https://github.com/google/protobuf) 3.0.0 or up

## Compiling and testing the codegen

To compile the plugin:
```
$ ../gradlew java_pluginExecutable
```

To test the plugin with the compiler:
```
$ ../gradlew test
```
You will see a `BUILD SUCCESSFUL` if the test succeeds.

To compile a proto file and generate Java interfaces out of the service definitions:
```
$ protoc --plugin=protoc-gen-xrpc-java=build/exe/java_plugin/protoc-gen-xrpc-java \
  --grpc-java_out="$OUTPUT_FILE" --proto_path="$DIR_OF_PROTO_FILE" "$PROTO_FILE"
```

## Installing the codegen to Maven local repository
This will compile a codegen and put it under your ``~/.m2/repository``. This
will make it available to any build tool that pulls codegens from Maven
repostiories.
```
$ ../gradlew install
```
