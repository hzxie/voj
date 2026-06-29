# Verwandlung Online Judge — Judger

The judger component of [Verwandlung Online Judge](https://github.com/hzxie/voj),
a self-hostable, open-source online judge for competitive programming. It pulls
submissions off an [Apache ActiveMQ](https://activemq.apache.org/) queue, then
compiles, runs and evaluates them against test data inside a sandbox, reporting
correctness, run time and memory usage back to the
[`zjhzxhz/voj.web`](https://hub.docker.com/r/zjhzxhz/voj.web) application.

The image ships the toolchains for every language enabled out of the box: C/C++,
Pascal, Java, Python 3, Go, JavaScript/Node.js, Rust and Kotlin.

## Quick Start

The judger connects to the web container's MySQL (3306) and ActiveMQ (61616), so
put both on a shared user-defined network (`--link` is deprecated):

```bash
docker network create voj

# Web (MySQL + ActiveMQ + the Spring Boot web app all live here)
docker run -d --name voj.web --network voj -p 8080:8080 zjhzxhz/voj.web

# Judger (resolves "voj.web" over the shared network)
docker run -d --name voj.judger --network voj zjhzxhz/voj.judger
```

You can run as many judgers in parallel as you need — they are decoupled from the
web app through the queue.

## Sandbox

The container runs as root and drops each submission to the unprivileged `nobody`
account, so native Linux sandboxing works out of the box: per-submission
`setrlimit` caps, peak-memory accounting, a private network namespace, a seccomp
syscall filter and privilege drop. If the judger cannot drop privileges, a
submission is **refused** rather than run as root.

> **Heads-up on secrets.** The published images are built with a placeholder
> database password and a placeholder `JUDGER_API_TOKEN` (the shared secret the
> judger presents to download test data). For a real deployment, build your own
> images so the judger's `MYSQL_USER_PASS` and `JUDGER_API_TOKEN` **match the web
> image's** values.

## Requirements

- **OS:** Linux (native sandbox). The judger also runs on Windows when built from source.
- **CPU/RAM:** any 64-bit host; the judger JVM is capped small (~256 MB) since the
  judged program runs as a separate OS process.
- All compilers and the JDK are baked into the image.

## Build It Yourself

The image is built from the [repository](https://github.com/hzxie/voj) with the
**repo root** as the build context:

```bash
git clone https://github.com/hzxie/voj.git
cd voj
docker build \
  --build-arg MYSQL_USER_PASS=... \
  --build-arg JUDGER_API_TOKEN=... \
  -t zjhzxhz/voj.judger -f docker/judger/Dockerfile .
```

The `MYSQL_USER_PASS` and `JUDGER_API_TOKEN` must match the web image. Or let
`scripts/run-docker.sh` generate them and build/run both images for you.

## Links

- [GitHub repository](https://github.com/hzxie/voj)
- [Docker docs](https://github.com/hzxie/voj/tree/master/docker)
- [Deployment guide](https://github.com/hzxie/voj/blob/master/docs/deployment.md)
- [Official demo](https://verwandlung.org)

**License:** [GNU GPL v3](https://github.com/hzxie/voj/blob/master/LICENSE)
