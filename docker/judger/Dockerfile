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

# Set environment variables.
# TARGETARCH is provided by BuildKit (amd64 / arm64) and matches the Ubuntu
# OpenJDK directory suffix, so the image builds natively on both.
ARG TARGETARCH
ENV HOME=/root
ENV JAVA_HOME=/usr/lib/jvm/java-25-openjdk-${TARGETARCH}
ENV M2_HOME=/opt/maven

# Define working directory.
WORKDIR /root

# Install Java, Python and the C/C++ toolchain in a single layer.
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        ca-certificates wget curl \
        make g++ python3 \
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
    rm apache-maven-${MAVEN_VERSION}-bin.tar.gz

# Build the judger project from the build context (no network checkout).
COPY . /root/voj
RUN sed -i "s@jdbc.url = jdbc:mysql://localhost:3306@jdbc.url = jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}@g" voj/judger/src/main/resources/voj.properties && \
    sed -i "s/jdbc.username = root/jdbc.username = voj/g" voj/judger/src/main/resources/voj.properties && \
    sed -i "s/jdbc.password = /jdbc.password = ${MYSQL_USER_PASS}/g" voj/judger/src/main/resources/voj.properties && \
    sed -i "s/localhost:61616/${ACTIVEMQ_HOST}:${ACTIVEMQ_PORT}/g" voj/judger/src/main/resources/voj.properties && \
    mkdir -p voj/target/classes && \
    $M2_HOME/bin/mvn package -DskipTests -f voj/judger/pom.xml

# Run Judger. --enable-native-access grants the JNI runner native access; without
# it the JDK 24+ warns and a future JDK will block JNI outright.
CMD ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "voj/judger/target/voj.judger.jar"]
