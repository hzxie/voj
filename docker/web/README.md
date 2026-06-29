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
docker run -d --name voj.web --network voj -p 8080:8080 \
  -e VOJ_JUDGER_API_TOKEN=your-shared-secret zjhzxhz/voj.web

# Judger (resolves "voj.web" over the shared network; token must match the web's)
docker run -d --name voj.judger --network voj \
  -e VOJ_JUDGER_API_TOKEN=your-shared-secret zjhzxhz/voj.judger
```

The web UI is then available at <http://localhost:8080/voj>.

## Configuration

The image is generic: every deployment knob is read from a `VOJ_*` environment
variable **at run time**, so you configure it with `docker run -e ...` — no
rebuild needed. Common variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `VOJ_DB_HOST` / `VOJ_DB_PORT` / `VOJ_DB_NAME` | bundled / `3306` / `voj` | Database location |
| `VOJ_DB_USERNAME` / `VOJ_DB_PASSWORD` | `voj` / `voj` | Database credentials |
| `VOJ_JUDGER_API_TOKEN` | `verwandlung` | Shared test-data secret (**must match** the judger) |
| `VOJ_BASE_URL` | `http://localhost:8080/voj` | Public root used in e-mails/links |
| `VOJ_CONTEXT_PATH` | `/voj` | Servlet context path (`/` serves at root) |
| `VOJ_JMS_BROKER_EMBEDDED` | `true` | Host the ActiveMQ broker in the web process |
| `VOJ_MAIL_HOST` / `VOJ_MAIL_USERNAME` / `VOJ_MAIL_PASSWORD` | empty | SMTP (empty disables e-mail) |

Behind a reverse proxy, set `VOJ_BASE_URL` to the externally visible URL so links,
e-mails and assets are correct.

> **Heads-up on secrets.** Out of the box the image uses a non-secret default
> `VOJ_JUDGER_API_TOKEN` and a bundled demo database with password `voj` — fine
> for a local trial, **not** for production. For a real deployment pass a long
> random `-e VOJ_JUDGER_API_TOKEN=...` (the same value on the web and judger
> containers) and point `-e VOJ_DB_HOST=... -e VOJ_DB_PASSWORD=...` at a database
> you manage.

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
docker build -t zjhzxhz/voj.web -f docker/web/Dockerfile .
```

No credentials are baked in — configure the running container with `-e VOJ_*`
(see **Configuration** above). The only build-time knob is the bundled demo
database password, `--build-arg MYSQL_USER_PASS=...` (default `voj`); if you change
it, start the container with a matching `-e VOJ_DB_PASSWORD=...`.

Or let `scripts/run-docker.sh` generate a strong random token and password, build
both images and start the containers for you.

## Links

- [GitHub repository](https://github.com/hzxie/voj)
- [Docker docs](https://github.com/hzxie/voj/tree/master/docker)
- [Configuration reference](https://github.com/hzxie/voj/blob/master/docs/configuration.md)
- [Official demo](https://verwandlung.org)

**License:** [GNU GPL v3](https://github.com/hzxie/voj/blob/master/LICENSE)
