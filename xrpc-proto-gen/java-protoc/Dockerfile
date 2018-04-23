## -*- docker-image-name: "nordstrom/java-protoc" -*-
FROM openjdk:8-jdk
MAINTAINER Andy Day <andy.day@nordstrom.com>

RUN apt-get update
RUN apt-get -y install autoconf libtool g++ make sudo

RUN git clone https://github.com/google/protobuf.git
WORKDIR protobuf
RUN git checkout v3.5.1
RUN ./autogen.sh
RUN ./configure
RUN make
RUN make check
RUN sudo make install

RUN apt-get -y install zip
RUN curl -LO https://services.gradle.org/distributions/gradle-4.7-bin.zip
RUN sudo mkdir /opt/gradle
RUN sudo unzip -d /opt/gradle gradle-4.7-bin.zip
ENV PATH $PATH:/opt/gradle/gradle-4.7/bin
RUN gradle -v
