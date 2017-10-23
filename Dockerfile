FROM openjdk:8-jre-alpine

ENV PORT=8080

WORKDIR /app
COPY . .

RUN apk --update upgrade && \
    # install JDK
    apk add curl openjdk8="$JAVA_ALPINE_VERSION" && \
    # build the application into a single JAR, including dependencies
    ./gradlew shadowJar && \
    mv build/libs/*-all.jar app.jar

# TODO(jkinkead): Fix this to use a base image instead of keeping around the build tools.

CMD java  \
  -Dconfig.file=application.conf \
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
  -jar app.jar
