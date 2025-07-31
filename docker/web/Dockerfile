# Dockerfile for Verwandlung Online Judge - Web
FROM ubuntu:24.04
MAINTAINER Haozhe Xie "root@haozhexie.com"

# User Settings
ARG MYSQL_ROOT_PASS=dBuZNz6tDKhgZjHX
ARG MYSQL_USER_PASS=U3bEwhRHnD6xNVpb
ARG WEBSITE_URL=localhost:8080/voj
ARG MAIL_HOST=smtp.mailgun.org
ARG MAIL_USERNAME=postmaster@verwandlung.org
ARG MAIL_PASSWORD=passwd

# Set environment variables.
ENV HOME          /root
ENV JAVA_HOME     /usr/lib/jvm/java-21-openjdk-amd64
ENV M2_HOME       /opt/maven

# Define working directory.
WORKDIR           /root

# Install MySQL and Set up MySQL
RUN apt-get update && \
    apt-get install -y mariadb-server mariadb-client && \
    sed -i "s/127\.0\.0\.1/0.0.0.0/g" /etc/mysql/mariadb.conf.d/50-server.cnf
RUN /etc/init.d/mariadb start && \
    /usr/bin/mysqladmin -u root password '${MYSQL_ROOT_PASS}' && \
    mysql -e "CREATE DATABASE voj" && \
    mysql -e "GRANT SELECT, INSERT, UPDATE, DELETE ON voj.* TO 'voj'@'%' IDENTIFIED BY '${MYSQL_USER_PASS}'"

# Install Java
RUN apt-get install -y openjdk-21-jdk wget curl && \
# Determine the versions of dependencies
    MAVEN_VERSION=$(curl -s https://maven.apache.org/download.cgi | grep -oP 'apache-maven-\K[0-9]+\.[0-9]+\.[0-9]+' | head -1) && \
    ACTIVEMQ_VERSION=$(curl -s https://activemq.apache.org/components/classic/download/ | grep -oP 'apache-activemq-\K[0-9]+\.[0-9]+\.[0-9]+' | head -1) && \
    TOMCAT_VERSION=$(curl -s https://tomcat.apache.org/download-11.cgi | grep -oP 'apache-tomcat-\K[0-9]+\.[0-9]+\.[0-9]+' | head -1) && \
    TOMCAT_MAJOR=$(echo $TOMCAT_VERSION | cut -d. -f1) && \
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
    sed -i 's/tempUsage limit="50 gb"/tempUsage limit="1 gb"/g' /opt/activemq/conf/activemq.xml && \
# Install Tomcat
    useradd tomcat -s /usr/sbin/nologin && \
    wget https://archive.apache.org/dist/tomcat/tomcat-${TOMCAT_MAJOR}/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz && \
    tar -xf apache-tomcat-${TOMCAT_VERSION}.tar.gz -C /opt && \
    mv /opt/apache-tomcat-${TOMCAT_VERSION} /opt/tomcat && \
    chown -R tomcat:tomcat /opt/tomcat && \
    rm apache-tomcat-${TOMCAT_VERSION}.tar.gz

# Setup Web Project
RUN apt-get install -y git && \
    git clone https://github.com/hzxie/voj.git && \
    $M2_HOME/bin/mvn package -DskipTests -f voj/web/pom.xml && \
    cp -R voj/web/target/voj.web /opt/tomcat/webapps/voj && \
    sed -i "s@localhost:8080/voj@${WEBSITE_URL}@g" /opt/tomcat/webapps/voj/WEB-INF/classes/voj.properties && \
    sed -i "s/jdbc.username = root/jdbc.username = voj/g" /opt/tomcat/webapps/voj/WEB-INF/classes/voj.properties && \
    sed -i "s/jdbc.password = /jdbc.password = ${MYSQL_USER_PASS}/g" /opt/tomcat/webapps/voj/WEB-INF/classes/voj.properties && \
    sed -i "s/mail.host = /mail.host = ${MAIL_HOST}/g" /opt/tomcat/webapps/voj/WEB-INF/classes/voj.properties && \
    sed -i "s/mail.username = /mail.username = ${MAIL_USERNAME}/g" /opt/tomcat/webapps/voj/WEB-INF/classes/voj.properties && \
    sed -i "s/mail.password = /mail.password = ${MAIL_PASSWORD}/g" /opt/tomcat/webapps/voj/WEB-INF/classes/voj.properties && \
    /etc/init.d/mariadb start && \
    mysql voj < voj/voj.sql

# Expose Ports
EXPOSE 3306
EXPOSE 8080
EXPOSE 61616

# Run MySQL, Tomcat and ActiveMQ
RUN apt-get install -y supervisor
RUN mkdir -p /var/log/supervisor
COPY supervisord.conf /etc/supervisord.conf 
CMD ["/usr/bin/supervisord"]
