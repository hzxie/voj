# Docker for Verwandlung Online Judge

The images are built from the **repository root** as the build context (the
Dockerfiles `COPY` in the source tree, they no longer clone from GitHub).

The published `zjhzxhz/voj.web` and `zjhzxhz/voj.judger` images on
[Docker Hub](https://hub.docker.com/r/zjhzxhz/voj.web/) are built and pushed
automatically by the [`docker.yml`](../.github/workflows/docker.yml) GitHub
Actions workflow on every push to `master` — Docker Hub itself does not build
anything.

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

You can also pull the prebuilt images from Docker Hub:

```
docker pull zjhzxhz/voj.web
docker pull zjhzxhz/voj.judger
```

## Run

The judger connects to the web container for MySQL (3306) and ActiveMQ (61616),
so put both on a shared user-defined network. `--link` is deprecated — use a
network instead.

```
# Create a network once
docker network create voj

# Web (MySQL + Tomcat + ActiveMQ live here)
docker run -d --name voj.web --network voj -p 8080:8080 zjhzxhz/voj.web

# Judger (resolves "voj.web" over the shared network)
docker run -d --name voj.judger --network voj zjhzxhz/voj.judger
```

The web application is available at
[http://localhost:8080/voj](http://localhost:8080/voj).
