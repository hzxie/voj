# Dockerfile for Verwandlung Online Judge - Judger
FROM ubuntu:18.04
MAINTAINER Haozhe Xie "cshzxie@gmail.com"

# User Settings
ARG MYSQL_USER_PASS=U3bEwhRHnD6xNVpb
ARG MYSQL_HOST=voj.web
ARG MYSQL_PORT=3306
ARG ACTIVEMQ_HOST=voj.web
ARG ACTIVEMQ_PORT=61616

# Set environment variables.
ENV HOME          /root
ENV JAVA_HOME     /usr/lib/jvm/java-11-openjdk-amd64
ENV M2_HOME       /opt/maven

# Define working directory.
WORKDIR           /root

# Install Java
RUN apt-get update
RUN apt-get install -y openjdk-11-jdk

# Install Maven
RUN apt-get install -y wget
RUN wget https://archive.apache.org/dist/maven/maven-3/3.5.4/binaries/apache-maven-3.5.4-bin.tar.gz
RUN tar -xf apache-maven-3.5.4-bin.tar.gz -C /opt
RUN mv /opt/apache-maven-3.5.4 /opt/maven
RUN rm apache-maven-3.5.4-bin.tar.gz

# Setup Judger Project
RUN apt-get install -y git gcc g++ make
RUN git clone https://github.com/hzxie/voj.git
RUN sed -i "s@jdbc.url = jdbc:mysql://localhost:3306@jdbc.url = jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}@g" voj/judger/src/main/resources/voj.properties
RUN sed -i "s/jdbc.username = root/jdbc.username = voj/g" voj/judger/src/main/resources/voj.properties
RUN sed -i "s/jdbc.password = /jdbc.password = ${MYSQL_USER_PASS}/g" voj/judger/src/main/resources/voj.properties
RUN sed -i "s/localhost:61616/${ACTIVEMQ_HOST}:${ACTIVEMQ_PORT}/g" voj/judger/src/main/resources/voj.properties
RUN mkdir -p voj/target/classes
RUN $M2_HOME/bin/mvn package -DskipTests -f voj/judger/pom.xml

# Setup Compliers
RUN apt-get install -y python3
RUN ln -s /usr/bin/python3 /usr/bin/python

# Run Judger
CMD ["java", "-jar", "voj/judger/target/voj.judger.jar"]
