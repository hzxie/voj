# Configuration Reference

Both components are configured through a `voj.properties` file on the classpath:

- Web application: [`web/src/main/resources/voj.properties`](../web/src/main/resources/voj.properties)
- Judger: [`judger/src/main/resources/voj.properties`](../judger/src/main/resources/voj.properties)

The committed files ship with local-development defaults and placeholder secrets.
Override them for any real deployment. The Docker images bake these values in at
build time via `--build-arg`; see [`../docker/README.md`](../docker/README.md).

## Web application

### Database

| Property | Default | Purpose |
| --- | --- | --- |
| `jdbc.driverClassName` | `com.mysql.cj.jdbc.Driver` | JDBC driver. Works for both MySQL and MariaDB. |
| `jdbc.url` | `jdbc:mysql://localhost:3306/voj?...` | Connection URL. Keep the `characterEncoding=UTF-8` and time-zone parameters. |
| `jdbc.username` | `root` | Database user. |
| `jdbc.password` | *(empty)* | Database password. |
| `jdbc.initialSize` / `jdbc.maxActive` / `jdbc.minIdle` / `jdbc.maxIdle` | `5` / `30` / `3` / `10` | Druid connection-pool sizing. |
| `jdbc.maxWait` | `30000` | Max wait (ms) for a pooled connection. |
| `jdbc.timeBetweenEvictionRunsMillis` | `60000` | Idle-connection eviction interval (ms). |
| `jdbc.minEvictableIdleTimeMillis` | `25200000` | Minimum idle time (ms) before a connection is evictable. |
| `jdbc.removeAbandoned` / `jdbc.removeAbandonedTimeout` | `true` / `1800` | Reclaim leaked connections after N seconds. |

### Mail

Email (verification, password reset, notifications) is disabled when `mail.host` is empty.

| Property | Default | Purpose |
| --- | --- | --- |
| `mail.host` | *(empty)* | SMTP host. Empty disables outbound email. |
| `mail.username` / `mail.password` | *(empty)* | SMTP credentials. |
| `mail.senderMail` | `noreply@example.com` | `From` address. |
| `mail.senderName` | `Verwandlung Online Judge` | `From` display name. |

### Message queue

| Property | Default | Purpose |
| --- | --- | --- |
| `jms.broker.url` | `tcp://localhost:61616` | ActiveMQ broker the web app publishes judging tasks to. |
| `jms.submissionTask.ttl` | `1800000` | Time-to-live (ms) for a judging task. If no capable judger claims it within this window the task is dead-lettered and the submission is marked failed instead of staying Pending forever. Keep it generous. |

### Checkpoint distribution

Judgers pull a problem's test data from the web app over HTTP rather than sharing a filesystem.

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.apiToken` | `${VOJ_JUDGER_API_TOKEN}` *(empty)* | Shared secret a judger must send in the `X-Judger-Token` header to download test data from `/api/judger/checkpoints/{problemId}`. Must match `judger.apiToken` on every judger. An **empty value disables the endpoint** (all requests refused). Use a long random string in production. |

### Web service

| Property | Default | Purpose |
| --- | --- | --- |
| `url.base` | `http://localhost:8080/voj` | Public base URL; used to build absolute links (e.g. in emails). |
| `url.cdn` | `http://localhost:8080/voj/assets` | Base URL for static assets. Point at a CDN in production. |

## Judger

The judger shares the database (`jdbc.*`) and message-queue (`jms.broker.url`)
settings above. Judger-specific keys:

### Message queue

The judger dials the same `jms.broker.url` as the web app, a plain `tcp://` URL.
It is built to share the queue with other judgers and to survive the broker
going away (in the Docker setup the broker lives in the web container, so it
restarts with it):

- When the broker becomes unreachable, the judger tries to reconnect every 5
  seconds until it is back, then receives tasks and sends results again.
- It takes a judging task only once it is idle (no prefetch), so a busy judger
  never holds back tasks that another judger could judge meanwhile.
- A task is acknowledged only after its verdict (or a System Error) has been
  recorded. If that is impossible, e.g. while the database is down, or the
  judger dies while judging, the broker delivers the task again, up to 5 times
  with a growing delay (10 s, 20 s, 40 s, ...), then moves it to `ActiveMQ.DLQ`,
  where the web app marks the submission as failed.

### Identity

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.username` | `voj@judger` | Account this judger authenticates as. |
| `judger.password` | *(empty)* | Password for that account. |
| `judger.description` | *(empty)* | Free-text label shown in the admin console (helps tell judgers apart). |

### Directories

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.workDir` | `/tmp` | Parent of the work directories. The judger creates a private `0700` subdirectory, `voj-<submissionId>-<random>`, for every judging and removes it afterwards; the random part keeps two judgers on one host apart when they judge the same submission. |
| `judger.checkpointDir` | `/tmp/voj-testpoints` | Local cache of downloaded reference inputs/outputs, kept readable only by the judger. Every judger on the host caches in a directory of its own, `judger-<n>/` (claimed with the lock file `judger-<n>.lock`, held while the judger runs), so judgers sharing this setting never replace test data another one is reading. Directories named after a problem id directly under it are the cache of an older version and can be deleted. |

### Checkpoint download

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.web.baseUrl` | `http://localhost:8080/voj` | Web application root the judger downloads test data from. |
| `judger.apiToken` | `${VOJ_JUDGER_API_TOKEN}` *(empty)* | Shared secret; **must match** `judger.apiToken` on the web app. Left empty, the web app refuses every download, so every submission is judged a System Error; the judger logs an error at startup. The Docker images and `docker-compose.yml` default it to `verwandlung` on both sides; use a long random string in production. |

### Sandbox

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.sandbox` | `native` | Sandbox backend. `native` is the in-process JNI runner (Linux: `setrlimit` + seccomp filter, which also refuses network sockets + network namespace where possible + privilege drop; Windows: separate low-privilege account). `isolate` (Linux only) delegates to the external [isolate](https://github.com/ioi/isolate) tool. See [`deployment.md`](deployment.md). |
| `judger.compileTimeLimit` | `20000` | CPU time (ms) a compilation may use (measured on the compiler's top-level process). |
| `judger.compileMemoryLimit` | `524288` | Memory (KB) a compilation may use, scaled by the language's memory multiplier like a run's limit. The native sandbox measures the compiler's top-level process against it and holds every process it runs to a kernel backstop of twice the limit plus headroom (`RLIMIT_DATA`). The base 512 MB caps C/C++/Go/Rust/Pascal "compiler bombs"; Kotlin (multiplier 2) gets 1 GB, since `kotlinc` is a JVM with a 512 MB heap of its own. |
| `judger.processLimit` | `64` | Processes a submission may have at once. The native sandbox on Linux does not let a run create processes at all; this is the sandbox user's `RLIMIT_NPROC`, which caps the run's threads. |
| `judger.compileProcessLimit` | `1024` | Processes and threads a compilation may have at once (both backends). Higher than `judger.processLimit` because the Go toolchain runs a compiler per CPU, each with up to a thread per CPU: a cold `go build` used ~100 threads on a 24-core host. |
| `judger.goBuildCacheSeed` | `${VOJ_GO_BUILD_CACHE_SEED}` *(empty)* | A read-only, root-owned Go build cache that each Go submission's private build cache is seeded from (hard links, or copies across file systems). Empty: every Go build starts from an empty cache. The Docker image builds one at `/opt/go-build-cache`. |
| `judger.isolate.executable` | `isolate` | Path to the `isolate` binary. Used only when `judger.sandbox = isolate`. |
| `judger.isolate.boxId` | `0` | isolate box id for this judger. Give each judger on the same host a distinct id. |
| `judger.isolate.firstUid` | `60000` | isolate's `first_uid`: box *n* runs as UID `first_uid + n`, and the judger hands each work directory to that UID. Must match isolate's configuration. |
| `judger.isolate.extraDirs` | `/etc/alternatives:maybe,/opt:maybe` | Extra directories bound read-only into the box (isolate `--dir` rules, comma-separated), for toolchains outside isolate's default mounts. |
| `judger.isolate.compileFiles` | `.fpc.cfg=/etc/fpc.cfg` | Host files copied into the box before a compilation (`boxName=hostPath`, comma-separated), for compiler configuration files isolate cannot bind (it binds directories only). The box is the compiler's working directory and `HOME`. |
| `judger.isolate.compileSyscalls` | `27` | isolate `--syscalls` flags for a compilation: every restriction but the one on file locks, which the Go build cache needs. Runs keep isolate's configured flags. Empty: use isolate's configuration (e.g. for an isolate without `--syscalls`). |
| `judger.isolate.maxProcesses` | `64` | Process/thread cap inside the isolate box for a run (a compilation uses `judger.compileProcessLimit`). |
| `judger.isolate.fileSizeLimitKb` | `262144` | Largest file (KB) the sandboxed program may create (`--fsize`); 0 means no limit. |
| `judger.isolate.wallTimeBufferSec` | `1.0` | Extra wall-clock seconds added on top of the CPU-time limit before isolate hard-kills a run. |

### Privilege drop

| Property | Default | Purpose |
| --- | --- | --- |
| `system.username` | `nobody` | Unprivileged account the native sandbox drops to. On Linux any non-root account works; `nobody` is present out of the box. When the judger runs as root, dropping privileges is mandatory and fails closed. A dedicated account is preferable on a host where other services use `nobody`. See [`deployment.md`](deployment.md). |
| `system.password` | *(empty)* | Unused on Linux; required on Windows for the separate low-privilege logon account. |
