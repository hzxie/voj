![Verwandlung Online Judge](https://raw.githubusercontent.com/hzxie/voj/master/web/src/main/webapp/assets/img/logo.png)

[![CircleCI](https://dl.circleci.com/status-badge/img/circleci/SxfxeEFFqyS3JM5ftpk7cK/DNyawsGdEXznk4GzmBs8dx/tree/master.svg?style=shield)](https://dl.circleci.com/status-badge/redirect/circleci/SxfxeEFFqyS3JM5ftpk7cK/DNyawsGdEXznk4GzmBs8dx/tree/master)
[![Build status](https://ci.appveyor.com/api/projects/status/j62ns9p8whttittm?svg=true)](https://ci.appveyor.com/project/hzxie/voj)
[![codecov](https://codecov.io/gh/hzxie/voj/branch/master/graph/badge.svg)](https://codecov.io/gh/hzxie/voj)
[![Docker Pulls](https://img.shields.io/docker/pulls/zjhzxhz/voj.web.svg)](https://hub.docker.com/r/zjhzxhz/voj.web)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

[**Official Website**](https://verwandlung.org) ·
[**Tech Support**](https://infinitescript.com/project/verwandlung-online-judge/) ·
[**Change Log**](https://github.com/hzxie/voj/commits/master)

## Introduction

Verwandlung Online Judge is a self-hostable, open-source online judge for
competitive programming. It presents algorithmic problems, accepts submissions in
multiple languages, then compiles, runs and evaluates them against predefined test
cases, reporting correctness, run time and memory usage.

Highlights:

- **Cross-platform native judging.** Submissions are judged natively on **Linux**
  and **Windows** through JNI, rather than relying on a Linux-only runtime.
- **Sandboxed execution.** On Linux each submission runs under kernel-enforced
  resource limits, a private network namespace and a seccomp syscall filter, as an
  unprivileged user; on Windows it runs under a separate low-privilege account
  inside a Job Object that caps the process count and enforces resource limits.
- **Real-time feedback** streamed to the browser with Server-Sent Events (SSE), so
  results appear progressively as each test case finishes.
- **Horizontally scalable judging.** The web app and the judgers are decoupled
  through an [Apache ActiveMQ](https://activemq.apache.org/) queue, so you can run
  as many judgers in parallel as you need.
- **Contests** and **internationalization** (English and Chinese) out of the box.

It is built on [Spring Boot](https://spring.io/projects/spring-boot) with the
[MyBatis](https://mybatis.org/mybatis-3/) persistence framework.

### Architecture

Verwandlung has two components, the **web application** and one or more
**judgers**, communicating over ActiveMQ:

![Software Architecture](https://www.infinitescript.com/projects/Verwandlung/Software-Architecture.webp)

## Getting Started

### Run with Docker (recommended)

The fastest way to stand up the whole stack (database, message queue, web app and a
judger):

```bash
git clone https://github.com/hzxie/voj.git
cd voj
scripts/run-docker.sh
```

`run-docker.sh` generates strong random database passwords, builds both images and
starts the `voj.web` and `voj.judger` containers on a shared network. The web UI is
then available at <http://localhost:8080/voj>.

To run the prebuilt images from Docker Hub instead of building locally:

```bash
VOJ_PULL=1 scripts/run-docker.sh
```

See [`docker/README.md`](docker/README.md) for image and configuration details.

### Build from source

**Web application** produces a self-contained executable JAR (embedded Tomcat):

```bash
cd web
mvn package -DskipTests        # -> web/target/voj.web.jar
java -jar target/voj.web.jar
```

**Judger** builds a Spring Boot jar plus the JNI native library, so a C++
toolchain is required:

```bash
# Linux build prerequisites: g++, make and libseccomp
sudo apt-get install -y g++ make libseccomp-dev

cd judger
mvn package -DskipTests        # -> judger/target/voj.judger.jar
```

> On some JDK layouts the JNI build needs `jni_md.h` / `jawt_md.h` copied from
> `$JAVA_HOME/include/linux` (or `.../win32`) up into `$JAVA_HOME/include`.

Convenience wrappers live in [`scripts/`](scripts/): `build-jars.sh`,
`build-docker.sh`, `run-web.sh` and `run-judger.sh` (the run scripts preflight
MySQL/ActiveMQ and import `sql/schema.sql`, `sql/seed.sql` and `sql/demo.sql` on
first launch).

Both components read a `voj.properties` file (database, mail, message queue,
sandbox, etc.). See [`docs/configuration.md`](docs/configuration.md) for the full
property reference.

### Requirements

| Component | Requirement |
| --- | --- |
| Java | [JDK](https://adoptium.net) 17+ (tested through JDK 25) |
| Database | [MySQL](https://www.mysql.com) 8.0+ or [MariaDB](https://mariadb.org/) 10.4+ |
| Message queue | [ActiveMQ](https://activemq.apache.org) 6.0+ (Jakarta JMS) |
| Judger (Linux build) | GCC/g++, make, libseccomp |
| Judger OS | Linux or Windows |

## Security model

Untrusted submissions run inside a sandbox so they cannot harm the judging host:

- **Linux (native).** Each submission runs in a forked child with `setrlimit`
  (CPU time, output size and process-count caps), accurate CPU-time and peak-memory
  accounting via `wait4`/`getrusage`, a private network namespace, a seccomp syscall
  filter (blocking `ptrace`, `mount`, module loading, etc.) and privileges dropped to
  an unprivileged user (`system.username`, defaulting to `nobody`). If the judger
  runs as root and cannot drop privileges, the submission is refused rather than run
  as root.
- **Linux (isolate).** Optionally set `judger.sandbox = isolate` to delegate to the
  external [isolate](https://github.com/ioi/isolate) tool instead.
- **Windows (native).** Each submission runs under a separate low-privilege Windows
  account (`CreateProcessWithLogonW`) inside a
  [Job Object](https://learn.microsoft.com/windows/win32/procthread/job-objects)
  that caps the process count (anti fork-bomb), enforces memory and wall-clock
  limits, applies UI restrictions (no access to the interactive desktop or
  clipboard), and kills the whole process tree when the run ends.

The native sandbox does not provide full filesystem isolation. Reference answers
and configuration are protected by file permissions (the work directory is handed
to the sandbox user, while the checkpoint directory stays readable only by the
judger), but a submission can still read world-readable host files. For complete
filesystem isolation, run the judger as root with `judger.sandbox = isolate`, which
confines each submission to its own mount namespace.

For shared, multi-tenant hosts, create a dedicated unprivileged account for
`system.username` instead of reusing `nobody`. See
[`docs/deployment.md`](docs/deployment.md) for the filesystem layout and
permissions a root deployment expects.

## The origin of "Verwandlung"

In 2011, LinkedIn released the [Kafka](https://kafka.apache.org/) message queue,
named after the author Franz Kafka. A year later, Alibaba introduced
[MetaQ](https://github.com/killme2008/Metamorphosis), built on Kafka, a nod to
Kafka's novella *Metamorphosis*. Since the message queue is central to this
application, it took the name "Verwandlung", the German title of *Metamorphosis*.

## Contributing

Contributions are welcome:

- Report bugs and request features on the [issues](https://github.com/hzxie/voj/issues) page.
- For new features, please open an issue to discuss before sending a pull request.
- Translators for additional languages are especially appreciated.

## License

This project is open-sourced under the [GNU GPL v3](LICENSE).
