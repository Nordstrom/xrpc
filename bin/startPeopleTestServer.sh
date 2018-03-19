#!/usr/bin/env bash

set -e

PORT=8080

./gradlew :xrpc:compileJava :demos:people:shadowJar && \
java -ea          \
  $JAVA_OPTS                      \
  -Djava.net.preferIPv4Stack=true \
  -Dio.netty.allocator.type=pooled \
  -XX:+UseStringDeduplication     \
  -XX:+UseTLAB                    \
  -XX:+AggressiveOpts             \
  -XX:+UseConcMarkSweepGC         \
  -XX:+CMSParallelRemarkEnabled   \
  -XX:+CMSClassUnloadingEnabled   \
  -XX:ReservedCodeCacheSize=128m  \
  -XX:SurvivorRatio=128           \
  -XX:MaxTenuringThreshold=0      \
  -XX:MaxDirectMemorySize=8G      \
  -Xss8M                          \
  -Xms512M                        \
  -Xmx4G                          \
  -server                         \
  -Dcom.sun.management.jmxremote                    \
  -Dcom.sun.management.jmxremote.port=9010          \
  -Dcom.sun.management.jmxremote.local.only=false   \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false          \
  -Dserver.port=$PORT                               \
  -Dconfig.file=application.conf                    \
  -jar demos/people/build/libs/xrpc-people-demo-0.1.1-SNAPSHOT-all.jar
