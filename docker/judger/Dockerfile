# Dockerfile for Verwandlung Online Judge - Judger
#
# Build context is the repository root, e.g.:
#   docker build -t zjhzxhz/voj.judger -f docker/judger/Dockerfile .
FROM ubuntu:26.04
LABEL org.opencontainers.image.authors="Haozhe Xie <root@haozhexie.com>"

# The judger no longer bakes its config at build time — every deployment knob is
# read from a VOJ_* environment variable at runtime (see the ENV block below and
# voj.properties), so the image stays generic and is configured with
# `docker run -e ...`.

# TARGETARCH is provided by BuildKit (amd64 / arm64) and matches the Ubuntu
# OpenJDK directory suffix, so the image builds natively on both.
ARG TARGETARCH
ARG MAVEN_VERSION=3.10.0
ARG KOTLIN_VERSION=2.4.20
ARG GO_VERSION=go1.27.1
ENV HOME=/root
ENV JAVA_HOME=/usr/lib/jvm/java-25-openjdk-${TARGETARCH}
ENV M2_HOME=/opt/maven

# Runtime configuration (override any of these with `docker run -e ...`).
# Defaults assume the judger shares a Docker network with a container named
# "voj.web" (the bundled MariaDB + ActiveMQ + web app); point them at external
# services for a split deployment. VOJ_DB_PASSWORD matches the web image's bundled
# database, and VOJ_JUDGER_API_TOKEN must match the web image's value.
ENV VOJ_DB_HOST=voj.web
ENV VOJ_DB_USERNAME=voj
ENV VOJ_DB_PASSWORD=voj
ENV VOJ_JMS_BROKER_URL=tcp://voj.web:61616
ENV VOJ_WEB_BASE_URL=http://voj.web:8080/voj
ENV VOJ_JUDGER_API_TOKEN=verwandlung
# Go toolchain settings. GOROOT/PATH point at the official tarball unpacked into
# /opt/go below. GOCACHE and GOPATH are deliberately not set: the judger gives
# every submission its own, in its private work directory, because a build cache
# shared by all submissions (which compile and run as the same sandbox user)
# could be poisoned by one of them for every later Go build. Each submission's
# cache is seeded from the read-only, root-owned cache built below
# (VOJ_GO_BUILD_CACHE_SEED), so the standard library is not rebuilt every time.
# GOENV=off skips the per-user env file under $HOME, and GOTOOLCHAIN=local keeps
# `go build` from fetching a newer toolchain over the (namespaced, offline)
# sandbox network.
ENV GOROOT=/opt/go
ENV GOENV=off
ENV GOTOOLCHAIN=local
ENV VOJ_GO_BUILD_CACHE_SEED=/opt/go-build-cache
# Put the Go and Kotlin compilers (unpacked into /opt below) on PATH so the
# judger can invoke `go`/`kotlinc` when compiling submissions.
ENV PATH=/opt/go/bin:/opt/kotlinc/bin:${PATH}

# Define working directory.
WORKDIR /root

# Install the toolchains for every language enabled in sql/seed.sql
# (C/C++, Pascal, Java, Python 3, Go, JavaScript/Node.js, Rust and Kotlin)
# using pinned versions so builds do not depend on release-page scraping or the
# unauthenticated GitHub API.
RUN apt-get -o Acquire::Retries=5 update && \
    DEBIAN_FRONTEND=noninteractive apt-get -o Acquire::Retries=5 install -y --no-install-recommends \
        ca-certificates wget curl unzip \
        make g++ fp-compiler python3 nodejs rustc \
        libseccomp-dev \
        openjdk-25-jdk && \
    rm -rf /var/lib/apt/lists/* && \
# Setup Python3 as the default Python
    ln -s /usr/bin/python3 /usr/bin/python

# Install Maven
# Apache downloads come from the fast release CDN, which only keeps the latest release
# of each line, and fall back to the permanent archive once a newer one is out.
RUN WGET="wget --progress=dot:giga --tries=5 --timeout=30 --retry-connrefused" && \
    TGZ=apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    URL=maven/maven-3/${MAVEN_VERSION}/binaries/$TGZ && \
    { $WGET -O $TGZ https://dlcdn.apache.org/$URL || $WGET -O $TGZ https://archive.apache.org/dist/$URL; } && \
    tar -xf $TGZ -C /opt && \
    mv /opt/apache-maven-${MAVEN_VERSION} /opt/maven && \
    rm $TGZ

# Install the Kotlin command-line compiler (no apt package; mirror the Maven
# pattern by unpacking the official release into /opt/kotlinc).
RUN wget --progress=dot:giga --tries=5 --timeout=30 --retry-connrefused \
        https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip && \
    unzip -q kotlin-compiler-${KOTLIN_VERSION}.zip -d /opt && \
    rm kotlin-compiler-${KOTLIN_VERSION}.zip

# Install the pinned official Go toolchain into /opt/go. Apt's golang-go lags
# upstream; this keeps Go on the same download-and-unpack footing as Maven and
# Kotlin. TARGETARCH (amd64/arm64) matches Go's release archive naming.
RUN wget --progress=dot:giga --tries=5 --timeout=30 --retry-connrefused \
        https://go.dev/dl/${GO_VERSION}.linux-${TARGETARCH}.tar.gz && \
    tar -C /opt -xzf ${GO_VERSION}.linux-${TARGETARCH}.tar.gz && \
    rm ${GO_VERSION}.linux-${TARGETARCH}.tar.gz

# Build the read-only Go build cache that seeds each submission's own cache: the
# standard-library packages submissions commonly import, compiled with the same
# toolchain and environment the judger compiles with. The judger hard-links these
# files into a submission's cache; they must stay owned by root and read-only, so
# a submission can neither modify them nor anything another submission builds on.
RUN mkdir /tmp/go-warm && cd /tmp/go-warm && \
    printf '%s\n' 'package main' 'import (' \
        '_ "bufio"' '_ "bytes"' '_ "cmp"' '_ "container/heap"' '_ "container/list"' \
        '_ "errors"' '_ "fmt"' '_ "io"' '_ "maps"' '_ "math"' '_ "math/big"' '_ "math/bits"' \
        '_ "math/rand"' '_ "math/rand/v2"' '_ "os"' '_ "regexp"' '_ "slices"' '_ "sort"' \
        '_ "strconv"' '_ "strings"' '_ "sync"' '_ "time"' '_ "unicode"' ')' 'func main() {}' > main.go && \
    GOCACHE=$VOJ_GO_BUILD_CACHE_SEED GOPATH=/tmp/go-warm/go go build -o /dev/null main.go && \
    rm -f $VOJ_GO_BUILD_CACHE_SEED/trim.txt && \
    chown -R root:root $VOJ_GO_BUILD_CACHE_SEED && \
    find $VOJ_GO_BUILD_CACHE_SEED -type d -exec chmod 0755 {} + && \
    find $VOJ_GO_BUILD_CACHE_SEED -type f -exec chmod 0444 {} + && \
    cd / && rm -rf /tmp/go-warm /root/.cache

# Build the judger project from the build context (no network checkout). voj.properties
# ships with ${VOJ_*} placeholders that are resolved from the environment at runtime,
# so nothing is baked in here — the image stays generic and is configured with
# `docker run -e ...`.
# The project version (scripts/version.sh); the build context has no .git to derive
# it from. Declared here so a new version does not invalidate the toolchain layers.
ARG VOJ_VERSION=0.0.0-SNAPSHOT
COPY . /root/voj
RUN mkdir -p voj/target/classes && \
    $M2_HOME/bin/mvn package -DskipTests -Drevision=${VOJ_VERSION} -f voj/judger/pom.xml

# Run Judger. --enable-native-access grants the JNI runner native access; without
# it the JDK 24+ warns and a future JDK will block JNI outright.
# Lean JVM defaults for the small-demo footprint: capped heap/metaspace, the
# single-threaded GC and no C2 JIT. The judged program runs as a separate OS
# process, so this heap only sizes the judger's own orchestration. Keep in sync
# with scripts/run-judger.sh.
CMD ["java", "--enable-native-access=ALL-UNNAMED", "-Xmx256m", "-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=128m", "-XX:TieredStopAtLevel=1", "-Xss512k", "-jar", "voj/judger/target/voj.judger.jar"]
