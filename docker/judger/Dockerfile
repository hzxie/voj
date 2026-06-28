# Dockerfile for Verwandlung Online Judge - Judger
#
# Build context is the repository root, e.g.:
#   docker build -t zjhzxhz/voj.judger -f docker/judger/Dockerfile .
FROM ubuntu:26.04
LABEL org.opencontainers.image.authors="Haozhe Xie <root@haozhexie.com>"

# Deployment settings. Must match the web image's MYSQL_USER_PASS. Pass it at
# build time with --build-arg (or use scripts/run-docker.sh). The placeholder
# default below is NOT a secret — override it.
ARG MYSQL_USER_PASS=changeme
ARG MYSQL_HOST=voj.web
ARG MYSQL_PORT=3306
ARG ACTIVEMQ_HOST=voj.web
ARG ACTIVEMQ_PORT=61616
# Where the judger downloads test data from, and the shared secret to do so. The
# host defaults to the web container; JUDGER_API_TOKEN must match the web image.
ARG WEB_HOST=voj.web
ARG WEB_PORT=8080
ARG JUDGER_API_TOKEN=changeme

# Set environment variables.
# TARGETARCH is provided by BuildKit (amd64 / arm64) and matches the Ubuntu
# OpenJDK directory suffix, so the image builds natively on both.
ARG TARGETARCH
ENV HOME=/root
ENV JAVA_HOME=/usr/lib/jvm/java-25-openjdk-${TARGETARCH}
ENV M2_HOME=/opt/maven
# Go toolchain settings. GOROOT/PATH point at the official tarball unpacked into
# /opt/go below. The judger drops privileges before compiling, so the build cache
# and GOPATH must live somewhere the unprivileged user can write (HOME stays
# /root, 0700) -- hence /tmp. GOENV=off skips the per-user env file under $HOME,
# and GOTOOLCHAIN=local keeps `go build` from fetching a newer toolchain over the
# (namespaced, offline) sandbox network.
ENV GOROOT=/opt/go
ENV GOCACHE=/tmp/.cache/go-build
ENV GOPATH=/tmp/go
ENV GOENV=off
ENV GOTOOLCHAIN=local
# Put the Go and Kotlin compilers (unpacked into /opt below) on PATH so the
# judger can invoke `go`/`kotlinc` when compiling submissions.
ENV PATH=/opt/go/bin:/opt/kotlinc/bin:${PATH}

# Define working directory.
WORKDIR /root

# Install the toolchains for every language enabled in sql/seed.sql
# (C/C++, Pascal, Java, Python 3, Go, JavaScript/Node.js, Rust and Kotlin)
# in a single layer.
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        ca-certificates wget curl unzip \
        make g++ fp-compiler python3 nodejs rustc \
        libseccomp-dev \
        openjdk-25-jdk && \
    rm -rf /var/lib/apt/lists/* && \
# Setup Python3 as the default Python
    ln -s /usr/bin/python3 /usr/bin/python && \
# Install Maven
    MAVEN_VERSION=$(curl -s https://maven.apache.org/download.cgi | grep -oP 'apache-maven-\K[0-9]+\.[0-9]+\.[0-9]+' | head -1) && \
    wget https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    tar -xf apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt && \
    mv /opt/apache-maven-${MAVEN_VERSION} /opt/maven && \
    rm apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
# Install the Kotlin command-line compiler (no apt package; mirror the Maven
# pattern by unpacking the official release into /opt/kotlinc).
    KOTLIN_VERSION=$(curl -s https://api.github.com/repos/JetBrains/kotlin/releases/latest | grep -oP '"tag_name": "v\K[^"]+') && \
    wget https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip && \
    unzip -q kotlin-compiler-${KOTLIN_VERSION}.zip -d /opt && \
    rm kotlin-compiler-${KOTLIN_VERSION}.zip && \
# Install the official Go toolchain (latest stable) into /opt/go. Apt's golang-go
# lags upstream; this keeps Go on the same download-and-unpack footing as Maven
# and Kotlin. TARGETARCH (amd64/arm64) matches Go's release archive naming.
    GO_VERSION=$(curl -s https://go.dev/VERSION?m=text | head -1) && \
    wget https://go.dev/dl/${GO_VERSION}.linux-${TARGETARCH}.tar.gz && \
    tar -C /opt -xzf ${GO_VERSION}.linux-${TARGETARCH}.tar.gz && \
    rm ${GO_VERSION}.linux-${TARGETARCH}.tar.gz

# Build the judger project from the build context (no network checkout).
COPY . /root/voj
RUN sed -i "s@jdbc.url = jdbc:mysql://localhost:3306@jdbc.url = jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}@g" voj/judger/src/main/resources/voj.properties && \
    sed -i "s/jdbc.username = root/jdbc.username = voj/g" voj/judger/src/main/resources/voj.properties && \
    sed -i "s/jdbc.password = /jdbc.password = ${MYSQL_USER_PASS}/g" voj/judger/src/main/resources/voj.properties && \
    sed -i "s/localhost:61616/${ACTIVEMQ_HOST}:${ACTIVEMQ_PORT}/g" voj/judger/src/main/resources/voj.properties && \
    sed -i "s@judger.web.baseUrl = http://localhost:8080/voj@judger.web.baseUrl = http://${WEB_HOST}:${WEB_PORT}/voj@g" voj/judger/src/main/resources/voj.properties && \
    sed -i "s/judger.apiToken = change-me-shared-secret/judger.apiToken = ${JUDGER_API_TOKEN}/g" voj/judger/src/main/resources/voj.properties && \
    mkdir -p voj/target/classes && \
    $M2_HOME/bin/mvn package -DskipTests -f voj/judger/pom.xml

# Run Judger. --enable-native-access grants the JNI runner native access; without
# it the JDK 24+ warns and a future JDK will block JNI outright.
# Lean JVM defaults for the small-demo footprint: capped heap/metaspace, the
# single-threaded GC and no C2 JIT. The judged program runs as a separate OS
# process, so this heap only sizes the judger's own orchestration. Keep in sync
# with scripts/run-judger.sh.
CMD ["java", "--enable-native-access=ALL-UNNAMED", "-Xmx256m", "-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=128m", "-XX:TieredStopAtLevel=1", "-Xss512k", "-jar", "voj/judger/target/voj.judger.jar"]
