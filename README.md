![Verwandlung Online Judge](https://raw.githubusercontent.com/zjhzxhz/voj/master/web/src/main/webapp/assets/img/logo.png)

Version: 0.0.1-SNAPSHOT

[![Build Status](https://travis-ci.org/zjhzxhz/voj.png?branch=master)](https://travis-ci.org/zjhzxhz/voj)

[**Official Website**](#) | 
[**Tech Support**](http://zjhzxhz.com) |
[**Change Log**](#)

---

## Introduction

Cross-platform online judge system based on [Spring MVC Framework](http://spring.io).

The application used following open-source projects:

 - [Spring MVC](http://spring.io) famework
 - [MyBatis](https://mybatis.github.io/mybatis-3/index.html) persistence framework
 - [Apache ActiveMQ](http://activemq.apache.org/) message queue
 - [Druid](https://github.com/alibaba/druid/) database connection pool
 - [Flat UI](http://flat-ui.com)
 - [jQuery](http://jquery.com)
 - [FontAwesome](http://fontawesome.io)
 - [CodeMirror](http://codemirror.net)
 - [Highlight.js](https://highlightjs.org/)

### The Origin of Verwandlung

In 2011, the LinkedIn Inc. published a message queue product called [Kafka](http://kafka.apache.org/). It's implemented in Scala and open-sourced.

In 2012, Alibaba Inc. published a message queue product called [MetaQ](https://github.com/killme2008/Metamorphosis), which is based on Kafka. It's implemented in Java.

MetaQ stands for *Metamorphosis*, which is a famous literature written by the author Franz Kafka.

As the message queue is one of the important components in the application, so I named the application *Verwandlung*, which is the German name of *Metamorphosis*.

### Architecture

The application contains two modules:

- Web Application
- Cross-Platform Judger

As you see, the Online Judge System can contain multiple judgers. The judgers communicate with the web application via message queue.

---

### Contribution

We're glad that you want to improve this project. 

- **We NEED TRANSLATOR** for multi-language support(English and Chinese have supported).
- You can report bugs [here](https://github.com/zjhzxhz/voj/issues).
- You can also create a pull request if you can fix the bug.
- If you want to add features to the project, please tell me in the [issues](https://github.com/zjhzxhz/voj/issues) page before developing.

Thanks for your corporation.

### License

This project is open sourced under GNU GPL v3.
