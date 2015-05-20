![Verwandlung Online Judge](https://raw.githubusercontent.com/zjhzxhz/voj/master/web/src/main/webapp/assets/img/logo.png)

Version: 1.0.0 (Under Development)

[![Build Status](https://travis-ci.org/zjhzxhz/voj.png?branch=master)](https://travis-ci.org/zjhzxhz/voj)

[**Official Website**](#) | 
[**Tech Support**](http://zjhzxhz.com) |
[**Change Log**](#)

---

### Introduction

Cross-platform online judge system based on [Spring MVC Framework](http://spring.io).

The application used following components:

 - [Spring MVC](http://spring.io) famework
 - [MyBatis](https://mybatis.github.io/mybatis-3/index.html) persistence framework
 - [Druid](https://github.com/alibaba/druid/) database connection pool
 - [CodeMirror](http://codemirror.net) Editor
 - [Apache ActiveMQ](http://activemq.apache.org/) message queue
 - [Sock.js](https://github.com/sockjs/sockjs-client) WebSocket client
 - [Stomp.js](https://github.com/jmesnil/stomp-websocket) STOMP client

### The Origin of Verwandlung

In 2011, the LinkedIn Inc. published a message queue product called [Kafka](http://kafka.apache.org/). It's implemented in Scala and open-sourced.

In 2012, Alibaba Inc. published a message queue product called [MetaQ](https://github.com/killme2008/Metamorphosis), which is based on Kafka. It's implemented in Java.

MetaQ stands for *Metamorphosis*, which is a famous literature written by the author Franz Kafka.

As the message queue is one of the important components in the application, so I named the application *Verwandlung*, which is the German name of *Metamorphosis*.

### Architecture

The application contains three modules:

- User Interface
- Message Queue
- Native Judger(Running Program)

As you see, you the Online Judge System can contain multiple judgers. The judgers communicate with the Web application via message queue.

### License

This project is open sourced under GNU GPL v3.
