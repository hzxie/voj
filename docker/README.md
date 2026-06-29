# Docker for Verwandlung Online Judge

The images are built from the **repository root** as the build context (the
Dockerfiles `COPY` in the source tree, they no longer clone from GitHub).

The published `zjhzxhz/voj.web` and `zjhzxhz/voj.judger` images on
[Docker Hub](https://hub.docker.com/r/zjhzxhz/voj.web/) are built and pushed
automatically by the [`docker.yml`](../.github/workflows/docker.yml) GitHub
Actions workflow on every push to `master`. Docker Hub itself does not build
anything.

## Quick Start

Two equivalent ways to bring up the same two-container stack (bundled web +
judger) — pick whichever you prefer:

```
# A) Docker Compose (declarative)
cp .env.example .env        # optional: set your URL, secrets, SMTP
docker compose up -d

# B) Plain `docker run`, no compose (imperative)
scripts/run-docker.sh
```

`scripts/run-docker.sh` builds both images, generates a strong random judger API
token (and, when building locally, a database password), and starts the `voj.web`
and `voj.judger` containers on a shared network, passing the secrets in as
environment variables (printed once at the end). To run the published images
instead of building locally, use `VOJ_PULL=1 scripts/run-docker.sh`.

`docker compose up` pulls the published images by default; uncomment the `build:`
blocks in [`../docker-compose.yml`](../docker-compose.yml) to build from this
checkout instead.

The rest of this document describes the underlying build and run steps.

## Persistence

Everything the web app stores — problems, users, submissions and the problem
**test data** — lives in MariaDB, so a single volume on its data directory
persists the whole installation. Both entry points mount one:

- Compose: the named volume `voj-db` (declared in `docker-compose.yml`).
- `scripts/run-docker.sh`: `-v voj-db:/var/lib/mariadb` (override the name with
  `DB_VOLUME=...`).

(`/var/lib/mariadb` is the MariaDB data directory in the image's Ubuntu base.)

On the **first** start Docker copies the image's bundled database into the empty
volume; afterwards the volume is the source of truth, so `docker compose pull &&
docker compose up -d` (or re-running `run-docker.sh` with a newer image) keeps
your data. The judger's downloaded test-data cache is not persisted — it is
re-fetched from the web app on demand.

> The bundled, single-container MariaDB is convenient but ties the database to the
> web container. For a larger or backup-friendly deployment, point `VOJ_DB_*` at a
> database you manage instead (the bundled one is then unused).

## Reverse proxy

Put TLS and a public hostname in front with nginx or Caddy; ready-to-adapt
examples are in [`reverse-proxy/`](reverse-proxy/). Two things matter:

1. Run the web container with the public URL so generated links, e-mails and
   assets are correct: `-e VOJ_BASE_URL=https://oj.example.edu/voj` (or
   `-e VOJ_CONTEXT_PATH=/ -e VOJ_BASE_URL=https://oj.example.edu` to serve at the
   site root).
2. The real-time judge result stream is **Server-Sent Events**, so the proxy must
   not buffer `/submission/getRealTimeJudgeResult.action` and must allow a long
   read timeout — otherwise results never reach the browser. The example configs
   handle this (nginx needs `proxy_buffering off`; Caddy does not buffer by
   default).

## Configuration model

The images are **generic**: every deployment knob (database, message queue, mail,
URLs, the judger token) is read from a `VOJ_*` environment variable **at run
time**, resolved by the app from `voj.properties`. Nothing is baked into the jar,
so you reconfigure with `docker run -e ...` — no rebuild to change a password, a
URL, or to move behind a reverse proxy. The most common variables:

| Variable | Applies to | Default | Purpose |
| --- | --- | --- | --- |
| `VOJ_DB_HOST` / `VOJ_DB_PORT` / `VOJ_DB_NAME` | web, judger | `voj.web` (image) / `3306` / `voj` | Database location |
| `VOJ_DB_USERNAME` / `VOJ_DB_PASSWORD` | web, judger | `voj` / `voj` | Database credentials |
| `VOJ_JMS_BROKER_URL` | web, judger | `tcp://0.0.0.0:61616` (web) | ActiveMQ connector |
| `VOJ_JMS_BROKER_EMBEDDED` | web | `true` | Host the broker in the web process |
| `VOJ_JUDGER_API_TOKEN` | web, judger | `verwandlung` | Shared test-data download secret (**must match** on both) |
| `VOJ_BASE_URL` | web | `http://localhost:8080/voj` | Public root used in e-mails/links |
| `VOJ_CONTEXT_PATH` | web | `/voj` | Servlet context path (set `/` to serve at root) |
| `VOJ_MAIL_HOST` / `VOJ_MAIL_USERNAME` / `VOJ_MAIL_PASSWORD` | web | empty | SMTP (empty disables e-mail) |

Behind a reverse proxy, set `VOJ_BASE_URL` (and optionally `VOJ_CONTEXT_PATH=/`)
to the externally visible URL so generated links, e-mails and assets are correct.

The only build-time setting that remains is the **bundled demo MariaDB** password,
which initialises the in-image database and defaults to `voj`.

## Build Locally

```
# Build both images (run from the repository root)
scripts/build-docker.sh

# Or build a single image
scripts/build-docker.sh web
scripts/build-docker.sh judger

# Equivalent raw docker command (note: context is the repo root "." )
docker build -t zjhzxhz/voj.web -f docker/web/Dockerfile .
docker build -t zjhzxhz/voj.judger -f docker/judger/Dockerfile .
```

The build no longer bakes any credentials. The only build-time argument is the
bundled demo MariaDB password (`MYSQL_USER_PASS`, default `voj`); if you change it
remember to start the web container with a matching `-e VOJ_DB_PASSWORD=...`:

```
docker build \
  --build-arg MYSQL_USER_PASS=s3cr3t \
  -t zjhzxhz/voj.web -f docker/web/Dockerfile .
```

You can also pull the prebuilt images from Docker Hub:

```
docker pull zjhzxhz/voj.web
docker pull zjhzxhz/voj.judger
```

## Run

The judger connects to the web container for MySQL (3306) and ActiveMQ (61616),
so put both on a shared user-defined network. `--link` is deprecated, so use a
network instead.

```
# Create a network once
docker network create voj

# Web (MySQL + ActiveMQ + the Spring Boot web app with embedded Tomcat live here)
# By default the broker is embedded in the web process (saves a JVM, ~150 MB+);
# run with -e VOJ_JMS_BROKER_EMBEDDED=false to use a standalone broker.
docker run -d --name voj.web --network voj -p 8080:8080 \
  -e VOJ_JUDGER_API_TOKEN=your-shared-secret \
  zjhzxhz/voj.web

# Judger (resolves "voj.web" over the shared network; token must match the web's)
docker run -d --name voj.judger --network voj \
  -e VOJ_JUDGER_API_TOKEN=your-shared-secret \
  zjhzxhz/voj.judger
```

For a real deployment behind a reverse proxy, also pass the web container
`-e VOJ_BASE_URL=https://oj.example.edu` (see the configuration table above).

The judger container runs as root and drops each submission to the `nobody`
account, so native sandboxing (resource limits, network namespace, seccomp and
privilege drop) works out of the box. See [`../docs/deployment.md`](../docs/deployment.md)
for the sandbox's filesystem expectations.

The web application is available at
[http://localhost:8080/voj](http://localhost:8080/voj).
