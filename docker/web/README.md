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
docker run -d --name voj.web --network voj --restart unless-stopped -p 8080:8080 \
  -e VOJ_JUDGER_API_TOKEN=your-shared-secret zjhzxhz/voj.web

# Judger (resolves "voj.web" over the shared network; token must match the web's).
# A judger that runs into a fatal error exits for Docker to restart it.
docker run -d --name voj.judger --network voj --restart unless-stopped \
  -e VOJ_JUDGER_API_TOKEN=your-shared-secret zjhzxhz/voj.judger
```

The web UI is then available at <http://localhost:8080/voj>.

On its first start the web container creates an administrator with a random
password and prints it once: `docker logs voj.web`. If you leave out
`VOJ_JUDGER_API_TOKEN`, it also generates a token, prints it once and keeps it in
the database volume (read it again with
`docker exec voj.web cat /var/lib/mariadb/voj-judger-api-token`); run the judgers
with that value. The token must have at least 16 characters, or the judgers
cannot download test data.

To try VOJ out with sample problems, contests and accounts, add `-e VOJ_DEMO=1`
on the first start: every sample account's password is its username (`root` /
`root` is an administrator), so never use it for a real site.

## Configuration

The image is generic: every deployment knob is read from a `VOJ_*` environment
variable **at run time**, so you configure it with `docker run -e ...` — no
rebuild needed. Common variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `VOJ_DB_HOST` / `VOJ_DB_PORT` / `VOJ_DB_NAME` | bundled / `3306` / `voj` | Database location |
| `VOJ_DB_USERNAME` / `VOJ_DB_PASSWORD` | `voj` / `voj` | Database credentials |
| `VOJ_JUDGER_API_TOKEN` | generated on the first start | Shared test-data secret, at least 16 characters (**must match** the judger) |
| `VOJ_DEMO` | `0` | `1` loads the sample data (passwords = usernames) on the first start |
| `VOJ_BASE_URL` | `http://localhost:8080/voj` | Public root used in e-mails/links |
| `VOJ_CONTEXT_PATH` | `/voj` | Servlet context path (`/` serves at root) |
| `VOJ_JMS_BROKER_EMBEDDED` | `true` | Host the ActiveMQ broker in the web process |
| `VOJ_MAIL_HOST` / `VOJ_MAIL_USERNAME` / `VOJ_MAIL_PASSWORD` | empty | SMTP credentials (empty host disables e-mail) |
| `VOJ_MAIL_PORT` | `587` | SMTP submission port (STARTTLS) |

Behind a reverse proxy, set `VOJ_BASE_URL` to the externally visible URL so links,
e-mails and assets are correct.

> **Heads-up on secrets.** The bundled database's password is `voj` unless you
> build the image with another one; that is fine while port 3306 is only reachable
> by your judgers, **not** on a public network. For a real deployment point
> `-e VOJ_DB_HOST=... -e VOJ_DB_PASSWORD=...` at a database you manage, and publish
> port 8080 only to a reverse proxy (`-p 127.0.0.1:8080:8080`).

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
