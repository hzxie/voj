# Dockerfile for Verwandlung Online Judge - Web
#
# Build context is the repository root, e.g.:
#   docker build -t zjhzxhz/voj.web -f docker/web/Dockerfile .
FROM ubuntu:26.04
LABEL org.opencontainers.image.authors="Haozhe Xie <root@haozhexie.com>"

# Deployment settings. Pass real values at build time with --build-arg (or use
# scripts/run-docker.sh, which generates strong random database passwords). The
# placeholder password defaults below are NOT secrets — override them.
ARG MYSQL_ROOT_PASS=changeme
ARG MYSQL_USER_PASS=changeme
ARG WEBSITE_URL=localhost:8080/voj
ARG MAIL_HOST=
ARG MAIL_USERNAME=
ARG MAIL_PASSWORD=
# Shared secret the judger presents to download test data. Must match the judger
# image's JUDGER_API_TOKEN. The placeholder default is NOT a secret — override it.
ARG JUDGER_API_TOKEN=changeme
# Host the ActiveMQ broker inside the web process instead of as a separate one.
# Defaults to true: this single-container demo is single-web-instance, so the
# embedded broker saves a whole broker JVM (~150 MB+). The web app reaches it over
# vm://, while voj.judger (a separate container) connects to the TCP connector on
# 61616. Set to false to run the bundled standalone broker instead.
ARG JMS_BROKER_EMBEDDED=true

# Set environment variables.
# TARGETARCH is provided by BuildKit (amd64 / arm64) and matches the Ubuntu
# OpenJDK directory suffix, so the image builds natively on both.
ARG TARGETARCH
ENV HOME=/root
ENV JAVA_HOME=/usr/lib/jvm/java-25-openjdk-${TARGETARCH}
ENV M2_HOME=/opt/maven
# Persist the broker mode into the runtime environment so supervisord can decide
# whether to start the standalone ActiveMQ process (see docker/web/supervisord.conf).
ENV JMS_BROKER_EMBEDDED=${JMS_BROKER_EMBEDDED}

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

# Build the web project from the build context (no network checkout).
# Deployment values are baked into voj.properties before packaging, then a
# self-contained executable JAR (embedded Tomcat) is produced.
COPY . /root/voj
RUN sed -i "s@localhost:8080/voj@${WEBSITE_URL}@g" voj/web/src/main/resources/voj.properties && \
    sed -i "s/jdbc.username = root/jdbc.username = voj/g" voj/web/src/main/resources/voj.properties && \
    sed -i "s/jdbc.password = /jdbc.password = ${MYSQL_USER_PASS}/g" voj/web/src/main/resources/voj.properties && \
    sed -i "s/mail.host = /mail.host = ${MAIL_HOST}/g" voj/web/src/main/resources/voj.properties && \
    sed -i "s/mail.username = /mail.username = ${MAIL_USERNAME}/g" voj/web/src/main/resources/voj.properties && \
    sed -i "s/mail.password = /mail.password = ${MAIL_PASSWORD}/g" voj/web/src/main/resources/voj.properties && \
    sed -i "s/judger.apiToken = change-me-shared-secret/judger.apiToken = ${JUDGER_API_TOKEN}/g" voj/web/src/main/resources/voj.properties && \
    if [ "${JMS_BROKER_EMBEDDED}" = "true" ]; then \
        sed -i "s/jms.broker.embedded = false/jms.broker.embedded = true/" voj/web/src/main/resources/voj.properties && \
        sed -i "s#jms.broker.url = tcp://localhost:61616#jms.broker.url = tcp://0.0.0.0:61616#" voj/web/src/main/resources/voj.properties ; \
    fi && \
    $M2_HOME/bin/mvn package -DskipTests -f voj/web/pom.xml && \
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
