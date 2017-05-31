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

#CMD java -Dserver.port=$PORT -Dconfig.file=application.conf -jar app.jar
CMD bin/startServer.sh