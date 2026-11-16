# Verwandlung Online Judge — Judger

The judger component of [Verwandlung Online Judge](https://github.com/hzxie/voj),
a self-hostable, open-source online judge for competitive programming. It takes
the pending submissions from the shared database, then compiles, runs and
evaluates them against test data inside a sandbox, recording the verdicts in the
database and streaming the progress to the
[`zjhzxhz/voj.web`](https://hub.docker.com/r/zjhzxhz/voj.web) application over
[Apache ActiveMQ](https://activemq.apache.org/).

The image ships the toolchains for every language enabled out of the box: C/C++,
Pascal, Java, Python 3, Go, JavaScript/Node.js, Rust and Kotlin.

## Quick Start

The judger connects to the web container's MySQL (3306) and ActiveMQ (61616), so
put both on a shared user-defined network (`--link` is deprecated):

```bash
docker network create voj

# Web (MySQL + ActiveMQ + the Spring Boot web app all live here)
docker run -d --name voj.web --network voj -p 8080:8080 \
  -e VOJ_JUDGER_API_TOKEN=your-shared-secret zjhzxhz/voj.web

# Judger (resolves "voj.web" over the shared network; token must match the web's).
# A judger that runs into a fatal error exits for Docker to restart it.
docker run -d --name voj.judger --network voj --restart unless-stopped \
  -e VOJ_JUDGER_API_TOKEN=your-shared-secret zjhzxhz/voj.judger
```

You can run as many judgers in parallel as you need — they take the submissions
from the shared database. Judgers in containers on one host all run submissions
as `nobody`, the same uid on the host, so the thread cap of a run
(`RLIMIT_NPROC`) counts the runs of all of them together; see
[Several judgers on one host](https://github.com/hzxie/voj/blob/master/docs/deployment.md#several-judgers-on-one-host).

## Sandbox

The container runs as root and drops each submission to the unprivileged `nobody`
account, so native Linux sandboxing works out of the box: per-submission
`setrlimit` caps, peak-memory accounting, a seccomp syscall filter (which also
refuses network sockets, so submissions cannot reach the network even though the
container lacks the `CAP_SYS_ADMIN` a private network namespace needs, and System
V IPC), [Landlock](https://docs.kernel.org/userspace-api/landlock.html) rules that
let a submission write only in its own work directory (so nothing it leaves in
`/tmp`, `/dev/shm`, ... can outlive it or reach a later submission, again without
the mount namespace the container could not create) and privilege drop. None of
this needs extra capabilities or a custom seccomp profile, but the host kernel
must be Linux 5.13 or later with Landlock enabled. If the judger cannot drop
privileges or set up any part of the sandbox, a submission is **refused** (a
system error) rather than run unconfined.

## Configuration

The image is generic: every deployment knob is read from a `VOJ_*` environment
variable **at run time**. The defaults assume a shared Docker network with a
container named `voj.web`; point them at external services for a split deployment:

| Variable | Default | Purpose |
| --- | --- | --- |
| `VOJ_DB_HOST` / `VOJ_DB_PORT` / `VOJ_DB_NAME` | `voj.web` / `3306` / `voj` | Database location |
| `VOJ_DB_USERNAME` / `VOJ_DB_PASSWORD` | `voj` / `voj` | Database credentials |
| `VOJ_JMS_BROKER_URL` | `tcp://voj.web:61616` | ActiveMQ connector |
| `VOJ_WEB_BASE_URL` | `http://voj.web:8080/voj` | Where the judger downloads test data |
| `VOJ_JUDGER_API_TOKEN` | `verwandlung` | Shared test-data secret (**must match** the web) |

> **Heads-up on secrets.** Out of the box the image uses a non-secret default
> `VOJ_JUDGER_API_TOKEN` and the bundled web image's demo database password (`voj`).
> For a real deployment pass a long random `-e VOJ_JUDGER_API_TOKEN=...` (the **same
> value** as the web container) and matching `-e VOJ_DB_*` for your database.

## Requirements

- **OS:** Linux 5.13+ with Landlock enabled (native sandbox; `landlock` is listed in
  `/sys/kernel/security/lsm` on most distributions). The judger also runs on Windows when
  built from source.
- **CPU/RAM:** any 64-bit host; the judger JVM is capped small (~256 MB) since the
  judged program runs as a separate OS process.
- All compilers and the JDK are baked into the image.

## Build It Yourself

The image is built from the [repository](https://github.com/hzxie/voj) with the
**repo root** as the build context:

```bash
git clone https://github.com/hzxie/voj.git
cd voj
docker build -t zjhzxhz/voj.judger -f docker/judger/Dockerfile .
```

No credentials are baked in — configure the running container with `-e VOJ_*`
(see **Configuration** above), making sure `VOJ_JUDGER_API_TOKEN` and the database
settings match the web image. Or let `scripts/run-docker.sh` generate a token and
build/run both images for you.

## Links

- [GitHub repository](https://github.com/hzxie/voj)
- [Docker docs](https://github.com/hzxie/voj/tree/master/docker)
- [Deployment guide](https://github.com/hzxie/voj/blob/master/docs/deployment.md)
- [Official demo](https://verwandlung.org)

**License:** [GNU GPL v3](https://github.com/hzxie/voj/blob/master/LICENSE)
