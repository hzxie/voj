# Verwandlung Online Judge — Web

The web application of [Verwandlung Online Judge](https://github.com/hzxie/voj),
a self-hostable, open-source online judge for competitive programming. It presents
algorithmic problems, accepts submissions in multiple languages, then has one or
more **judgers** ([`zjhzxhz/voj.judger`](https://hub.docker.com/r/zjhzxhz/voj.judger))
compile, run and evaluate them, reporting correctness, run time and memory usage.

This image bundles MySQL/MariaDB, an ActiveMQ broker and the web app (a
self-contained Spring Boot executable JAR with embedded Tomcat — no external
Tomcat needed) so a full stack runs from a single container.

## Quick Start

The judger talks to this container's MySQL (3306) and ActiveMQ (61616), so put
both on a shared user-defined network (`--link` is deprecated):

```bash
docker network create voj

# Web (MySQL + ActiveMQ + the Spring Boot web app all live here)
docker run -d --name voj.web --network voj -p 8080:8080 zjhzxhz/voj.web

# Judger (resolves "voj.web" over the shared network)
docker run -d --name voj.judger --network voj zjhzxhz/voj.judger
```

The web UI is then available at <http://localhost:8080/voj>.

> **Heads-up on secrets.** The published images are built with placeholder
> database passwords and a placeholder `JUDGER_API_TOKEN` (the shared secret the
> judger presents to download test data). They are fine for a local trial but are
> **not** secrets. For a real deployment, build your own images with
> `--build-arg` (see the build docs below); the web and judger images must share
> the same `MYSQL_USER_PASS` and `JUDGER_API_TOKEN`.

## Ports

| Port  | Service                          |
| ----- | -------------------------------- |
| 8080  | Web application (`/voj`)         |
| 3306  | MySQL/MariaDB                    |
| 61616 | ActiveMQ broker (TCP connector)  |

## Requirements

- **CPU/RAM:** any 64-bit host; ~1 GB RAM is comfortable for the single-container demo.
- Everything the app needs (JDK 25, MariaDB, ActiveMQ) is baked into the image.

## Build It Yourself

The image is built from the [repository](https://github.com/hzxie/voj) with the
**repo root** as the build context:

```bash
git clone https://github.com/hzxie/voj.git
cd voj
docker build \
  --build-arg MYSQL_ROOT_PASS=... \
  --build-arg MYSQL_USER_PASS=... \
  --build-arg JUDGER_API_TOKEN=... \
  -t zjhzxhz/voj.web -f docker/web/Dockerfile .
```

Or let `scripts/run-docker.sh` generate strong random passwords and a token, build
both images and start the containers for you.

## Links

- [GitHub repository](https://github.com/hzxie/voj)
- [Docker docs](https://github.com/hzxie/voj/tree/master/docker)
- [Configuration reference](https://github.com/hzxie/voj/blob/master/docs/configuration.md)
- [Official demo](https://verwandlung.org)

**License:** [GNU GPL v3](https://github.com/hzxie/voj/blob/master/LICENSE)
