FROM openjdk:8-jre-alpine

ENV PORT=8080
ENV M2_HOME=/usr/lib/mvn
ENV M2=$M2_HOME/bin
ENV PATH $PATH:$M2_HOME:$M2

WORKDIR /app
COPY . .

RUN apk --update upgrade && \
    # install Maven and JDK
    apk add curl openjdk8="$JAVA_ALPINE_VERSION" && \
    curl http://mirrors.sonic.net/apache/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz | tar -zx && \
    mv apache-maven-3.3.9 /usr/lib/mvn && \
    # build the application into a single JAR, including dependencies
    mvn package && \
    rm target/original-*.jar && \
    mv target/*.jar app.jar && \
    # remove all build artifacts & dependencies, Maven, and the JDK
    rm -rf /root/.m2 && \
    rm -rf /usr/lib/mvn && \
    rm -rf target && \
    apk del curl openjdk8

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

#CMD bin/startServer.sh
