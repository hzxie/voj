# Dockerfile for Verwandlung Online Judge - Web
FROM ubuntu:18.04
MAINTAINER Haozhe Xie "cshzxie@gmail.com"

# User Settings
ARG MYSQL_ROOT_PASS=dBuZNz6tDKhgZjHX
ARG MYSQL_USER_PASS=U3bEwhRHnD6xNVpb
ARG WEBSITE_URL=localhost:8080/voj
ARG MAIL_HOST=smtp.mailgun.org
ARG MAIL_USERNAME=postmaster@verwandlung.org
ARG MAIL_PASSWORD=passwd

# Set environment variables.
ENV HOME          /root
ENV JAVA_HOME     /usr/lib/jvm/java-11-openjdk-amd64
ENV M2_HOME       /opt/maven

# Define working directory.
WORKDIR           /root

# Install MySQL and Set up MySQL
RUN apt-get update
RUN apt-get install -y mariadb-server mariadb-client
RUN sed -i "s/127\.0\.0\.1/0.0.0.0/g" /etc/mysql/mariadb.conf.d/50-server.cnf
RUN /etc/init.d/mysql start && \
    /usr/bin/mysqladmin -u root password '${MYSQL_ROOT_PASS}' && \
    mysql -e "CREATE DATABASE voj" && \
    mysql -e "GRANT SELECT, INSERT, UPDATE, DELETE ON voj.* TO 'voj'@'%' IDENTIFIED BY '${MYSQL_USER_PASS}'"

# Install Java
RUN apt-get install -y openjdk-11-jdk

# Install Maven
RUN apt-get install -y wget
RUN wget https://archive.apache.org/dist/maven/maven-3/3.5.4/binaries/apache-maven-3.5.4-bin.tar.gz
RUN tar -xf apache-maven-3.5.4-bin.tar.gz -C /opt
RUN mv /opt/apache-maven-3.5.4 /opt/maven
RUN rm apache-maven-3.5.4-bin.tar.gz

# Install ActiveMQ
RUN useradd activemq -s /usr/sbin/nologin
RUN wget https://archive.apache.org/dist/activemq/5.15.5/apache-activemq-5.15.5-bin.tar.gz
RUN tar -xf apache-activemq-5.15.5-bin.tar.gz -C /opt
RUN mv /opt/apache-activemq-5.15.5 /opt/activemq
RUN chown -R activemq:activemq /opt/activemq
RUN sed -i 's/memoryUsage percentOfJvmHeap="70"/memoryUsage limit="256 mb"/g' /opt/activemq/conf/activemq.xml
RUN sed -i 's/storeUsage limit="100 gb"/storeUsage limit="1 gb"/g' /opt/activemq/conf/activemq.xml
RUN sed -i 's/tempUsage limit="50 gb"/tempUsage limit="1 gb"/g' /opt/activemq/conf/activemq.xml
RUN rm apache-activemq-5.15.5-bin.tar.gz

# Install Tomcat
RUN useradd tomcat -s /usr/sbin/nologin
RUN wget https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.12/bin/apache-tomcat-9.0.12.tar.gz
RUN tar -xf apache-tomcat-9.0.12.tar.gz -C /opt
RUN mv /opt/apache-tomcat-9.0.12 /opt/tomcat/
RUN chown -R tomcat:tomcat /opt/tomcat/
RUN rm apache-tomcat-9.0.12.tar.gz

# Setup Web Project
RUN apt-get install -y git
RUN git clone https://github.com/hzxie/voj.git
RUN $M2_HOME/bin/mvn package -DskipTests -f voj/web/pom.xml
RUN cp -R voj/web/target/voj.web /opt/tomcat/webapps/voj
RUN sed -i "s@localhost:8080/voj@${WEBSITE_URL}@g" /opt/tomcat/webapps/voj/WEB-INF/classes/voj.properties
RUN sed -i "s/jdbc.username = root/jdbc.username = voj/g" /opt/tomcat/webapps/voj/WEB-INF/classes/voj.properties
RUN sed -i "s/jdbc.password = /jdbc.password = ${MYSQL_USER_PASS}/g" /opt/tomcat/webapps/voj/WEB-INF/classes/voj.properties
RUN sed -i "s/mail.host = /mail.host = ${MAIL_HOST}/g" /opt/tomcat/webapps/voj/WEB-INF/classes/voj.properties
RUN sed -i "s/mail.username = /mail.username = ${MAIL_USERNAME}/g" /opt/tomcat/webapps/voj/WEB-INF/classes/voj.properties
RUN sed -i "s/mail.password = /mail.password = ${MAIL_PASSWORD}/g" /opt/tomcat/webapps/voj/WEB-INF/classes/voj.properties
RUN /etc/init.d/mysql start && \
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
