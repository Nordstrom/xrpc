cmake_minimum_required(VERSION 3.8)
project(compiler)

set(CMAKE_CXX_STANDARD 11)

set(SOURCE_FILES
        src/java_plugin/cpp/java_generator.cpp
        src/java_plugin/cpp/java_generator.h
        src/java_plugin/cpp/java_plugin.cpp)

add_executable(compiler ${SOURCE_FILES})