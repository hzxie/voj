[![Verwandlung Online Judge](https://raw.githubusercontent.com/zjhzxhz/verwandlung/master/web/verwandlung/src/main/webapp/assets/img/logo.png)](#)

Version: 1.0.0 (Under Development)

[![Build Status](https://travis-ci.org/zjhzxhz/verwandlung.png?branch=master)](https://travis-ci.org/zjhzxhz/verwandlung)

[**Official Website**](#) | 
[**Tech Support**](http://www.zjhzxhz.com) |
[**Change Log**](#)

---

### Introduction

Cross-Platform Online Judge System based on [Spring MVC Framework](http://spring.io).

The application used following components:

 - [Spring](http://spring.io) MVC Famework
 - [Druid](https://github.com/alibaba/druid/) Database Connection Pool
 - [CodeMirror](http://codemirror.net) Editor
 - [MetaQ](https://github.com/killme2008/Metamorphosis) Message Queue
 - [Socket IO](http://socket.io)

### The Origin of Verwandlung

In 2011, the LinkedIn Inc. published a message queue product called [Kafka](http://kafka.apache.org/). It's implemented in Scala and open-sourced.

In 2012, Alibaba Inc. published a message queue product called [MetaQ](https://github.com/killme2008/Metamorphosis), which is based on Kafka. It's implemented in Java.

MetaQ stands for *Metamorphosis*, which is a famous literature written by the author Franz Kafka.

As the message queue is one of the important components in the application, so I named the application *Verwandlung*, which is the German name of  Metamorphosis.

### Architecture

The application contains three modules:

- User Interface
- Message Queue
- Native Judger(Running Program)

As you see, you the Online Judge System can contain multiple judgers. The judgers communicate with the Web application via message queue.

### License

This project is open sourced under GNU GPL v3.
