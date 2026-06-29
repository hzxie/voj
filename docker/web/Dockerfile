# Dockerfile for Verwandlung Online Judge - Web
#
# Build context is the repository root, e.g.:
#   docker build -t zjhzxhz/voj.web -f docker/web/Dockerfile .
FROM ubuntu:26.04
LABEL org.opencontainers.image.authors="Haozhe Xie <root@haozhexie.com>"

# Credentials for the bundled (demo) MariaDB only. The app no longer bakes its
# config at build time — every deployment knob is read from a VOJ_* environment
# variable at runtime (see the ENV block below and voj.properties). These two
# ARGs just initialise the in-image database; if you override them, also pass a
# matching `-e VOJ_DB_PASSWORD=...` at `docker run` so the app can log in.
ARG MYSQL_ROOT_PASS=voj
ARG MYSQL_USER_PASS=voj

# TARGETARCH is provided by BuildKit (amd64 / arm64) and matches the Ubuntu
# OpenJDK directory suffix, so the image builds natively on both.
ARG TARGETARCH
ENV HOME=/root
ENV JAVA_HOME=/usr/lib/jvm/java-25-openjdk-${TARGETARCH}
ENV M2_HOME=/opt/maven

# Runtime configuration (override any of these with `docker run -e ...`).
# Defaults wire the app to the bundled MariaDB + embedded broker so a bare
# `docker run` works out of the box; point VOJ_DB_HOST / VOJ_JMS_BROKER_URL at
# external services, or set VOJ_BASE_URL when running behind a reverse proxy.
ENV VOJ_DB_USERNAME=voj
ENV VOJ_DB_PASSWORD=voj
# Embed the ActiveMQ broker in the web process (saves a standalone broker JVM,
# ~150 MB+); both the app and supervisord read VOJ_JMS_BROKER_EMBEDDED. The TCP
# connector binds 0.0.0.0 so a separate voj.judger container can connect.
ENV VOJ_JMS_BROKER_EMBEDDED=true
ENV VOJ_JMS_BROKER_URL=tcp://0.0.0.0:61616
# Non-secret default so the published web + judger images interoperate out of the
# box. Override on BOTH images with a long random value for a real deployment.
ENV VOJ_JUDGER_API_TOKEN=verwandlung

# Define working directory.
WORKDIR /root

# Install system packages in a single layer and drop the apt cache.
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        ca-certificates wget curl \
        mariadb-server mariadb-client \
        openjdk-25-jdk \
        supervisor && \
    rm -rf /var/lib/apt/lists/*

# Trim MariaDB's memory footprint for the single-container demo. Copied before
# the server first starts so the leaner settings apply to the build-time import
# and every subsequent boot.
COPY docker/web/mariadb-demo.cnf /etc/mysql/mariadb.conf.d/99-voj-demo.cnf

# Set up MariaDB
RUN sed -i "s/127\.0\.0\.1/0.0.0.0/g" /etc/mysql/mariadb.conf.d/50-server.cnf && \
    /etc/init.d/mariadb start && \
    mysqladmin -u root password "${MYSQL_ROOT_PASS}" && \
    mysql -e "CREATE DATABASE voj" && \
    mysql -e "GRANT SELECT, INSERT, UPDATE, DELETE ON voj.* TO 'voj'@'%' IDENTIFIED BY '${MYSQL_USER_PASS}'"

# Install Maven and ActiveMQ. The web app now ships as a self-contained Spring
# Boot executable JAR with an embedded Tomcat, so no external Tomcat is needed.
RUN MAVEN_VERSION=$(curl -s https://maven.apache.org/download.cgi | grep -oP 'apache-maven-\K[0-9]+\.[0-9]+\.[0-9]+' | head -1) && \
    ACTIVEMQ_VERSION=$(curl -s https://activemq.apache.org/components/classic/download/ | grep -oP 'apache-activemq-\K[0-9]+\.[0-9]+\.[0-9]+' | head -1) && \
# Install Maven
    wget https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    tar -xf apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt && \
    mv /opt/apache-maven-${MAVEN_VERSION} /opt/maven && \
    rm apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
# Install ActiveMQ
    useradd activemq -s /usr/sbin/nologin && \
    wget https://archive.apache.org/dist/activemq/${ACTIVEMQ_VERSION}/apache-activemq-${ACTIVEMQ_VERSION}-bin.tar.gz && \
    tar -xf apache-activemq-${ACTIVEMQ_VERSION}-bin.tar.gz -C /opt && \
    mv /opt/apache-activemq-${ACTIVEMQ_VERSION} /opt/activemq && \
    rm apache-activemq-${ACTIVEMQ_VERSION}-bin.tar.gz && \
    chown -R activemq:activemq /opt/activemq && \
    sed -i 's/memoryUsage percentOfJvmHeap="70"/memoryUsage limit="256 mb"/g' /opt/activemq/conf/activemq.xml && \
    sed -i 's/storeUsage limit="100 gb"/storeUsage limit="1 gb"/g' /opt/activemq/conf/activemq.xml && \
    sed -i 's/tempUsage limit="50 gb"/tempUsage limit="1 gb"/g' /opt/activemq/conf/activemq.xml

# Build the web project from the build context (no network checkout). voj.properties
# ships with ${VOJ_*} placeholders that are resolved from the environment at runtime,
# so nothing is baked in here — the image stays generic and is configured with
# `docker run -e ...`. A self-contained executable JAR (embedded Tomcat) is produced.
COPY . /root/voj
RUN $M2_HOME/bin/mvn package -DskipTests -f voj/web/pom.xml && \
    mkdir -p /opt/voj && \
    cp voj/web/target/voj.web.jar /opt/voj/voj.web.jar && \
    /etc/init.d/mariadb start && \
    mysql voj < voj/sql/schema.sql && \
    mysql voj < voj/sql/seed.sql && \
    mysql voj < voj/sql/demo.sql

# Expose Ports
EXPOSE 3306
EXPOSE 8080
EXPOSE 61616

# Run MySQL, ActiveMQ and the web application
RUN mkdir -p /var/log/supervisor
COPY docker/web/supervisord.conf /etc/supervisord.conf
CMD ["/usr/bin/supervisord"]
