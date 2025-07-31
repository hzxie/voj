![Verwandlung Online Judge](https://raw.githubusercontent.com/hzxie/voj/master/web/src/main/webapp/assets/img/logo.png)

[![codefactor badge](https://www.codefactor.io/repository/github/hzxie/voj/badge)](https://www.codefactor.io/repository/github/hzxie/voj)
[![Build status](https://ci.appveyor.com/api/projects/status/j62ns9p8whttittm?svg=true)](https://ci.appveyor.com/project/hzxie/voj)
[![CircleCI](https://dl.circleci.com/status-badge/img/circleci/SxfxeEFFqyS3JM5ftpk7cK/DNyawsGdEXznk4GzmBs8dx/tree/master.svg?style=shield)](https://dl.circleci.com/status-badge/redirect/circleci/SxfxeEFFqyS3JM5ftpk7cK/DNyawsGdEXznk4GzmBs8dx/tree/master)
[![Coverage Status](https://coveralls.io/repos/hzxie/voj/badge.svg?branch=master&service=github)](https://coveralls.io/github/hzxie/voj?branch=master)
[![Docker Automated build](https://img.shields.io/docker/automated/jrottenberg/ffmpeg.svg)](http://hub.docker.com/r/zjhzxhz)
![badgegen](https://api.infinitescript.com/badgen/count.svg?name=hzxie/voj)

[**Official Website**](https://verwandlung.org) | 
[**Tech Support**](https://infinitescript.com/project/verwandlung-online-judge/) |
[**Change Log**](https://github.com/hzxie/voj/commits/master)

**Update:** Verwandlung Online Judge now supports Docker. 
You can use Verwandlung Online Judge with ONLY 4 bash commands.

```
docker pull zjhzxhz/voj.web
docker pull zjhzxhz/voj.judger
docker run -d --name voj.web -p 8080:8080 zjhzxhz/voj.web
docker run -d --name voj.judger --link voj.web zjhzxhz/voj.judger
```

For more information, please refer to [this documentation](https://github.com/hzxie/voj/tree/master/docker).

---

## Introduction

It is a cross-platform online judge system based on [Spring MVC Framework](http://spring.io).

The application used following open-source projects:

 - [Spring MVC](http://spring.io) famework
 - [MyBatis](https://mybatis.github.io/mybatis-3/index.html) persistence framework
 - [Apache ActiveMQ](http://activemq.apache.org/) message queue
 - [Druid](https://github.com/alibaba/druid/) database connection pool
 - [Flat UI](http://designmodo.github.io/Flat-UI/)
 - [jQuery](http://jquery.com)
 - [FontAwesome](http://fontawesome.io)
 - [CodeMirror](http://codemirror.net)
 - [Highlight.js](https://highlightjs.org/)

### The Origin of Verwandlung

In 2011, LinkedIn Inc. launched a message queue called [Kafka](http://kafka.apache.org/), which is written in Scala and available as open-source software.

A year later, Alibaba Inc. introduced a message queue named [MetaQ](https://github.com/killme2008/Metamorphosis) which is built on top of Kafka and implemented in Java. The name "MetaQ" is derived from the title of a renowned literary work, "Metamorphosis", authored by Franz Kafka.

Since message queues are vital components in an application, the name of the application was chosen as "Verwandlung", which is the German translation for "*Metamorphosis*".

### Architecture

The Verwandlung application consists of two primary components:

- Web Application
- Judger (compatible with both Windows and Linux operating systems)

The overall architecture of the application can be illustrated by the diagram provided below:

![Software-Architecture](https://www.infinitescript.com/projects/Verwandlung/Software-Architecture.png)

As depicted in the diagram, the Verwandlung Online Judge features support for multiple judgers. These judgers are responsible for communicating with the web application through the use of ActiveMQ.

---

## Getting Started

### System Requirements

#### Hardware Requirements

- **CPU**: 2.0 GHz or faster 32-bit (x86) or 64-bit (x64) processor

For Web Application (including Database and Message Queue):

- **RAM**: 2.0 GB RAM on Windows, 1.0 GB RAM on Linux.

For Judger:

- **RAM**: 1.0 GB RAM on Windows, 512 MB RAM on Linux.

#### Software Requirements

For Web Application (including Database and Message Queue):

- **Operating System**: Windows, Linux or Mac
- **Database**: [MySQL](http://www.mysql.com) 5.5+ or [MariaDB](https://mariadb.org/) 5.5+
- **Java Runtime**: [Oracle JRE](http://java.oracle.com) 17+ or [Oracle JDK](http://java.oracle.com) 17+
- **Message Queue**: [ActiveMQ](http://activemq.apache.org) 5.18+
- **Web Server**: [Tomcat](http://tomcat.apache.org) 10+

For Judger:

- **Operating System**: Windows or Linux
- **Java Runtime**: [Oracle JRE](http://java.oracle.com) 17+ or [Oracle JDK](http://java.oracle.com) 17+

### Installation

#### Docker Releases (Recommended)

Now you can easily use Verwandlung Online Judge with Docker.

See the installation instructions [here](https://github.com/hzxie/voj/tree/master/docker).

#### Binary Releases

- **Web Application**: [0.2.0](https://github.com/hzxie/voj/releases/download/0.2.0/voj.war)
- **Judger (Windows, 64 Bit)**: [0.2.0](https://github.com/hzxie/voj/releases/download/0.2.0/voj-judger-windows-x64.jar)
- **Judger (Linux, 64 Bit)**: [0.2.0](https://github.com/hzxie/voj/releases/download/0.2.0/voj-judger-linux-x64.jar)

#### Source Releases

**NOTE:** 

- [Maven](http://maven.apache.org) 3+ and [GCC](http://gcc.gnu.org/) 4.8+ with **POSIX thread model** is required.
- Make sure `mvn` (Maven), `g++` and `make` are added to the PATH.

After download source code from this repository, run following commands from a terminal:

For Web Application:

```
cd web
mvn package -DskipTests
```

If the build is successful, the terminal will display a message as following:

```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 10.168 s
[INFO] Finished at: 2015-11-26T13:20:09+08:00
[INFO] Final Memory: 24M/210M
[INFO] ------------------------------------------------------------------------
```

And you'll get a package named `voj.web.war` in the `target` folder.

For Judger:

**Windows**:

```
cd %JAVA_HOME%\include\win32
copy jawt_md.h  ..
copy jni_md.h  ..

cd judger
mvn package -DskipTests
```

**Linux**:

```
cd $JAVA_HOME/include/linux
cp jawt_md.h jni_md.h ..

cd SOURCE_CODE_PATH/judger
mvn package -DskipTests
```

If the build is successful, the terminal will display a message as following:

```
[INFO] Executing tasks

jni:
     [echo] Generating JNI headers
     [exec] mkdir -p target/cpp
     [exec] g++ -c -std=c++11 -Wall -fPIC -I ... -o target/cpp/Judger.Core.Runner.o
[INFO] Executed tasks
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 12.432 s
[INFO] Finished at: 2015-11-26T13:22:46+08:00
[INFO] Final Memory: 81M/513M
[INFO] ------------------------------------------------------------------------
```

And you'll get a package named `voj.judger.jar` in the `target` folder.

### Configuration

#### Setup the ActiveMQ

To reduce the memory of ActiveMQ, you can edit `activemq.xml` in `ACTIVEMQ_HOME\conf`.

Please find following content in this file, and change it to proper values that suitable for your servers.

```
<systemUsage>
    <systemUsage>
        <memoryUsage>
            <!-- Change this value -->
            <memoryUsage limit="128 mb" />
        </memoryUsage>
        <storeUsage>
            <!-- Change this value -->
            <storeUsage limit="4 gb"/>
        </storeUsage>
        <tempUsage>
            <!-- Change this value -->
            <tempUsage limit="4 gb"/>
        </tempUsage>
    </systemUsage>
</systemUsage>
```

#### Setup the Web Application

Create a database in MySQL, import `voj.sql`.

Edit the values in `/WEB-INF/classes/voj.properties` of the file `voj.web.war`.

You can open it with archive manager software such as `WinRAR`.

After then, you can copy this file `voj.web.war` to `TOMCAT_HOME/webapps`.

**IMPORTANT:** For Windows users, please edit `server.xml` of your Tomcat configuration:

```
<Connector connectionTimeout="20000" port="8080" protocol="HTTP/1.1"
    redirectPort="8443" useBodyEncodingForURI="true">
</Connector>
```

#### Setup the Judger

Edit the values in `/voj.properties` of the file `voj.judger.jar`.

You can run the judger using following command :

**Windows**:

```
javaw -jar voj.judger.jar
```

**Linux**:

```
sudo java -jar voj.judger.jar
```

**Important:**

If you are using Linux, please run following commands using `root`:

```
# Shutdown and Kill process is not allowed for non-root user
chmod 700 /sbin/init
chmod 700 /sbin/poweroff
chmod 700 /usr/bin/pkill
```

---

### Contribution

We're glad that you want to improve this project. 

- **We NEED TRANSLATORS** for multi-language support(English and Chinese have supported).
- You can report bugs [here](https://github.com/hzxie/voj/issues).
- You can also create a pull request if you can fix the bug.
- If you want to add features to the project, please tell us in the [issues](https://github.com/hzxie/voj/issues) page before developing.

Thanks for your corporation.

### License

This project is open sourced under GNU GPL v3.
