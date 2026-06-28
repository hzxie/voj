# Docker for Verwandlung Online Judge

The images are built from the **repository root** as the build context (the
Dockerfiles `COPY` in the source tree, they no longer clone from GitHub).

The published `zjhzxhz/voj.web` and `zjhzxhz/voj.judger` images on
[Docker Hub](https://hub.docker.com/r/zjhzxhz/voj.web/) are built and pushed
automatically by the [`docker.yml`](../.github/workflows/docker.yml) GitHub
Actions workflow on every push to `master`. Docker Hub itself does not build
anything.

## Quick Start

From the repository root:

```
scripts/run-docker.sh
```

This generates strong random database passwords, builds both images with those
secrets and starts the `voj.web` and `voj.judger` containers on a shared network.
The generated secrets are printed once at the end. To run the published images
instead of building locally, use `VOJ_PULL=1 scripts/run-docker.sh`.

The rest of this document describes the underlying build and run steps.

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

### Credentials

Database passwords, the judger API token and mail settings are baked into the
images at build time through `--build-arg`. The committed defaults are
placeholders, not secrets, so supply real values when building for anything other
than a local trial:

```
docker build \
  --build-arg MYSQL_ROOT_PASS=... \
  --build-arg MYSQL_USER_PASS=... \
  --build-arg JUDGER_API_TOKEN=... \
  -t zjhzxhz/voj.web -f docker/web/Dockerfile .

# The judger must be built with the SAME MYSQL_USER_PASS and JUDGER_API_TOKEN as
# the web image.
docker build \
  --build-arg MYSQL_USER_PASS=... \
  --build-arg JUDGER_API_TOKEN=... \
  -t zjhzxhz/voj.judger -f docker/judger/Dockerfile .
```

`JUDGER_API_TOKEN` is the shared secret the judger presents (in the
`X-Judger-Token` header) to download a problem's test data from the web app; both
images must be built with the same value.

`scripts/run-docker.sh` does this for you and generates the passwords and token if
you do not provide them.

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
# build with --build-arg JMS_BROKER_EMBEDDED=false to run a standalone broker.
docker run -d --name voj.web --network voj -p 8080:8080 zjhzxhz/voj.web

# Judger (resolves "voj.web" over the shared network)
docker run -d --name voj.judger --network voj zjhzxhz/voj.judger
```

The judger container runs as root and drops each submission to the `nobody`
account, so native sandboxing (resource limits, network namespace, seccomp and
privilege drop) works out of the box. See [`../docs/deployment.md`](../docs/deployment.md)
for the sandbox's filesystem expectations.

The web application is available at
[http://localhost:8080/voj](http://localhost:8080/voj).
