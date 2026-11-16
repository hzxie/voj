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
| `jdbc.url` | `jdbc:mysql://localhost:3306/voj?...` | Connection URL. Keep the `characterEncoding=UTF-8` and time-zone parameters (`connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true`): times are stored and read in UTC whatever the time zones of the database server and the JVM. Days in the statistics (the activity heat map, the dashboard's daily chart and "today" counts) are those of the JVM's time zone (`TZ` or `-Duser.timezone`). |
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

The web app survives a restart of an external broker: its listeners (the judgers'
messages, the dead-letter queue) try to reconnect every 5 seconds until the broker
is back, and the next judging task after the restart is sent over a new connection.

| Property | Default | Purpose |
| --- | --- | --- |
| `jms.broker.url` | `tcp://localhost:61616` | ActiveMQ broker the web app publishes judging tasks to. |
| `jms.submissionTask.ttl` | `1800000` | Time-to-live (ms) of a judging task: how long a submission may wait for a judger to take it. Every minute the web app marks as a System Error each pending submission that no judger took within this window after its task was queued (no running judger supports its language, all of them were busy, or the task was lost, e.g. when it could not be sent); an administrator can rejudge it. Keep it generous: a busy or briefly offline (but capable) cluster must not fail valid submissions. It does not limit how long a judging may take once a judger took the submission (see `judging.leaseTimeout`). `0` (or less): tasks never expire, and a submission waits for a judger as long as it takes. |
| `judging.leaseTimeout` | `600000` | How long (ms) a judging may go without renewing its lease. The judger that took a submission renews its lease after each step of the judging: the download of the test data, the compilation, and the run of each checkpoint. Every minute the web app marks as a System Error each pending submission whose lease is older than this (the judger stopped, or could not reach the database), however long the judging as a whole takes; the judging, should it still be running, stops at its next step. It must exceed the longest step: a download (`judger.web.downloadTimeout`, 300 s by default), a compilation (`judger.compileTimeLimit` of CPU time, and at most three times that plus a second of wall-clock time) and the run of one checkpoint (at most three times the problem's time limit, scaled by the language's time multiplier, plus a second). Must be positive. |

Both times are measured by the database's clock: the web app sets the queued
time, and the judger the lease, with `NOW()`, so the clocks of the hosts do not
matter.

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
  recorded. If that is impossible, e.g. while the database is down, if the
  test data cannot be downloaded for now (the web app restarting or
  unreachable, a 5xx status, 408 or 429, a download that times out), or
  if the judger dies while judging, the task goes back to the broker, which
  delivers it again to whichever judger asks for a task first. The judger whose
  judging failed waits before it asks again, 10 s after a failure and twice as
  long after each further failure in a row (up to 160 s), so an idle judger
  takes the task first, and a judger on its own retries it with a growing
  delay (10 s, 20 s, 40 s, ...). A task is delivered at most 6 times (5
  redeliveries). On its last delivery, a judging that could succeed later
  (e.g. the test data cannot be downloaded) is judged a System Error that says
  why, instead of being handed back. A judger that hands a task back releases
  the submission, which waits for a judger again. A task that is handed back
  nevertheless (the database is down, the judger dies) or expires
  (`jms.submissionTask.ttl`) ends on `ActiveMQ.DLQ`, from which the web app
  removes it; the submission is marked as a System Error once no judger took it
  in time, or its judging stopped renewing its lease (see
  `jms.submissionTask.ttl` and `judging.leaseTimeout`).
- A judging stops, without recording a verdict or sending anything more, once
  it finds (when it renews its lease) that it was superseded: the submission was
  rejudged, taken by a later delivery of the same task, or marked as a System
  Error. The web app shows the messages of the current judging of a submission
  only.

### Identity

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.username` | `voj@judger` | Account this judger authenticates as. |
| `judger.password` | *(empty)* | Password for that account. |
| `judger.description` | *(empty)* | Free-text label shown in the admin console (helps tell judgers apart). |

### Directories

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.workDir` | — | No longer used: the judger judges in `work-<n>/` under `judger.checkpointDir`. A judger that finds it set (or `VOJ_JUDGER_WORK_DIR`) logs a warning and ignores it. |
| `judger.checkpointDir` | `/var/lib/voj-judger` (`%ProgramData%\voj-judger` on Windows) | The judger's own directory, never in the temporary directory (whose clean-up of old files would remove what the judger keeps there): the local cache of downloaded reference inputs/outputs, and the work directories of the judgings. Every judger on the host claims the first slot *n* here whose lock file `judger-<n>.lock` no other judger holds (held from its start until it exits). It caches in `judger-<n>/` (`0700`), so judgers sharing this setting never replace test data another one is reading, and judges in `work-<n>/` (`0711`): it creates a private `0700` subdirectory, `voj-<submissionId>-<random>`, there for every judging and removes it afterwards, however deep the tree a submission built there or whatever permissions it took away. When it claims its slot, the judger removes what a judger with that slot left in `work-<n>/` (one that was killed while judging), without following links; what it cannot remove does not keep it from starting: it moves the directory aside, to `trash-<n>-<random>/`, and removes that at a later start. The directory must belong to the judger's account, and other users (the group included) must not be able to write to it, as they could replace the test data or the lock files in it: the judger refuses to start otherwise, rather than change the permissions of a directory it did not create (e.g. `/tmp`). A missing one it creates with mode `0711`: others may pass through it, but neither list nor read anything in it but the work directory of a judging they are handed. A judger that may not create it (one that is not root may not create the default) does not start: create it for the judger's account, or set this (`VOJ_JUDGER_CHECKPOINT_DIR`) to a directory the judger may create. When the judger runs submissions as `system.username` (the native sandbox, as root), that account must be able to pass through every directory above `work-<n>/`: this directory and every one above it must let others pass (`o+x`), or the judger does not start. Directories named after a problem id directly under it are the cache of an older version and can be deleted. |

### Checkpoint download

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.web.baseUrl` | `http://localhost:8080/voj` | Web application root the judger downloads test data from. A problem the web app has no test data for (HTTP 404), or an archive that is malformed, is a System Error; any other failed download is retried (see [Message queue](#message-queue)). |
| `judger.web.downloadTimeout` | `300` | Seconds a download of a problem's test data may take as a whole, from the request to the last byte; a download that is not over by then (e.g. the web app stopped sending in the middle) fails. Raise it for very large test data on a slow link. |
| `judger.apiToken` | `${VOJ_JUDGER_API_TOKEN}` *(empty)* | Shared secret; **must match** `judger.apiToken` on the web app. Left empty (or wrong), the web app refuses every download (HTTP 403), so every submission is judged a System Error at once, whose judge log says the judger was refused and to check `judger.apiToken`; the judger logs an error at startup when it is empty. (Any client error but 404, 408 and 429 is such a refusal; 404 means the problem has no test data.) The Docker images and `docker-compose.yml` default it to `verwandlung` on both sides; use a long random string in production. |

### Sandbox

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.sandbox` | `native` | Sandbox backend. `native` is the in-process JNI runner (Linux: `setrlimit` + seccomp filter, which also refuses network sockets and System V IPC + Landlock, which confines writes to the submission's work directory and needs Linux 5.13+ with Landlock enabled + network namespace where possible + privilege drop; Windows: separate low-privilege account). `isolate` (Linux only) delegates to the external [isolate](https://github.com/ioi/isolate) tool. See [`deployment.md`](deployment.md). |
| `judger.compileTimeLimit` | `20000` | CPU time (ms) a compilation may use (measured on the compiler's top-level process). |
| `judger.compileMemoryLimit` | `524288` | Memory (KB) a compilation may use, scaled by the language's memory multiplier like a run's limit. The native sandbox measures the compiler's top-level process against it and holds every process it runs to a kernel backstop of twice the limit plus headroom (`RLIMIT_DATA`). The base 512 MB caps C/C++/Go/Rust/Pascal "compiler bombs"; Kotlin (multiplier 2) gets 1 GB, since `kotlinc` is a JVM with a 512 MB heap of its own. |
| `judger.processLimit` | `64` | Processes and threads a run of a submission may have at once. The native sandbox on Linux does not let a run create processes at all, so this is the sandbox user's `RLIMIT_NPROC`, which caps the run's threads; isolate applies it with `--processes` (which counts threads too). On Windows a run's Job Object is capped at 2 processes (the program and its console host) whatever this is set to. |
| `judger.compileProcessLimit` | `1024` | Processes and threads a compilation may have at once (both backends). Higher than `judger.processLimit` because the Go toolchain runs a compiler per CPU, each with up to a thread per CPU: a cold `go build` used ~100 threads on a 24-core host. |
| `judger.goBuildCacheSeed` | `${VOJ_GO_BUILD_CACHE_SEED}` *(empty)* | A read-only, root-owned Go build cache that each Go submission's private build cache is seeded from (hard links, or copies across file systems). Empty: every Go build starts from an empty cache. The Docker image builds one at `/opt/go-build-cache`. |
| `judger.isolate.executable` | `isolate` | Path to the `isolate` binary. Used only when `judger.sandbox = isolate`. |
| `judger.isolate.boxId` | `0` | isolate box id for this judger. Give each judger on the same host a distinct id. |
| `judger.isolate.firstUid` | `60000` | isolate's `first_uid`: box *n* runs as UID `first_uid + n`, and the judger hands each work directory to that UID. Must match isolate's configuration. |
| `judger.isolate.extraDirs` | `/etc/alternatives:maybe,/opt:maybe` | Extra directories bound read-only into the box (isolate `--dir` rules, comma-separated), for toolchains outside isolate's default mounts. |
| `judger.isolate.compileFiles` | `.fpc.cfg=/etc/fpc.cfg` | Host files copied into the box before a compilation (`boxName=hostPath`, comma-separated), for compiler configuration files isolate cannot bind (it binds directories only). The box is the compiler's working directory and `HOME`. |
| `judger.isolate.compileSyscalls` | `27` | isolate `--syscalls` flags for a compilation: every restriction but the one on file locks, which the Go build cache needs. Runs keep isolate's configured flags. Empty: use isolate's configuration (e.g. for an isolate without `--syscalls`). |
| `judger.isolate.fileSizeLimitKb` | `262144` | Largest file (KB) the sandboxed program may create (`--fsize`); 0 means no limit. A run whose output reaches it is Output Limit Exceeded. |
| `judger.isolate.wallTimeBufferSec` | `1.0` | Extra wall-clock seconds added on top of the CPU-time limit before isolate hard-kills a run. |

### Privilege drop

| Property | Default | Purpose |
| --- | --- | --- |
| `system.username` | `nobody` | Unprivileged account the native sandbox drops to. On Linux any non-root account works; `nobody` is present out of the box. When the judger runs as root, dropping privileges is mandatory and fails closed. A dedicated account is preferable on a host where other services use `nobody`. Every judger on a host needs an account of its own, as it needs an isolate box id of its own: a judger running as root holds a lock on `/run/voj-sandbox-<uid>.lock` (the temporary directory if `/run` is not writable) and refuses to start when another judger on the host already uses the account. See [`deployment.md`](deployment.md#several-judgers-on-one-host). |
| `system.password` | *(empty)* | Unused on Linux; required on Windows for the separate low-privilege logon account. |
