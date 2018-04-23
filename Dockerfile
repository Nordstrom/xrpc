# This is used to build all xrpc projects.  It is broken down into parts strategically to take
# advantage of docker cache.
FROM nordstrom/java-protoc

WORKDIR /build

COPY build.gradle settings.gradle ./
COPY xrpc/build.gradle xrpc/
COPY xrpc-proto-gen/build.gradle xrpc-proto-gen/
COPY xrpc-testing/build.gradle xrpc-testing/
COPY demos/dino/build.gradle demos/dino/
COPY demos/people/build.gradle demos/people/
COPY demos/request-response-pipeline/build.gradle demos/request-response-pipeline/

# Download dependencies
RUN gradle resolveDependencies

COPY config/checkstyle config/checkstyle

# Build xrpc-testing
COPY xrpc-testing/src xrpc-testing/src
RUN gradle :xrpc-testing:build

RUN ls -al xrpc-testing/

# Build xrpc
COPY xrpc/src xrpc/src
RUN gradle xrpc:build

# Build xrpc-proto-gen
COPY xrpc-proto-gen/src xrpc-proto-gen/src
RUN gradle xrpc-proto-gen:build

# Build dino demo
COPY demos/dino/src demos/dino/src
RUN gradle demos:dino:build

# Build people demo
COPY demos/people/src demos/people/src
RUN gradle demos:people:build

# Build request response pipeline demo
COPY demos/request-response-pipeline/src demos/request-response-pipeline/src
RUN gradle demos:request-response-pipeline:build


