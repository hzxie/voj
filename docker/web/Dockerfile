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
ENV JAVA_HOME     /usr/lib/jvm/java-17-openjdk-amd64
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

# Install Java and Maven
RUN apt-get install -y openjdk-17-jdk wget && \
    wget https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz && \
    tar -xf apache-maven-3.9.6-bin.tar.gz -C /opt && \
    mv /opt/apache-maven-3.9.6 /opt/maven && \
    rm apache-maven-3.9.6-bin.tar.gz && \
# Install ActiveMQ
    useradd activemq -s /usr/sbin/nologin && \
    wget https://archive.apache.org/dist/activemq/6.1.2/apache-activemq-6.1.2-bin.tar.gz && \
    tar -xf apache-activemq-6.1.2-bin.tar.gz -C /opt && \
    mv /opt/apache-activemq-6.1.2 /opt/activemq && \
    chown -R activemq:activemq /opt/activemq && \
    sed -i 's/memoryUsage percentOfJvmHeap="70"/memoryUsage limit="256 mb"/g' /opt/activemq/conf/activemq.xml && \
    sed -i 's/storeUsage limit="100 gb"/storeUsage limit="1 gb"/g' /opt/activemq/conf/activemq.xml && \
    sed -i 's/tempUsage limit="50 gb"/tempUsage limit="1 gb"/g' /opt/activemq/conf/activemq.xml && \
    rm apache-activemq-6.1.2-bin.tar.gz && \
# Install Tomcat
    useradd tomcat -s /usr/sbin/nologin && \
    wget https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.24/bin/apache-tomcat-10.1.24.tar.gz && \
    tar -xf apache-tomcat-10.1.24.tar.gz -C /opt && \
    mv /opt/apache-tomcat-10.1.24 /opt/tomcat/ && \
    chown -R tomcat:tomcat /opt/tomcat/ && \
    rm apache-tomcat-10.1.24.tar.gz

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
