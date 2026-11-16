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
| `jdbc.url` | `jdbc:mysql://localhost:3306/voj?...` | Connection URL. Keep the `characterEncoding=UTF-8` and time-zone parameters (`connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true`): times are stored and read in UTC whatever the time zones of the database server and the JVM. Days in the statistics (the activity heat map, the dashboard's daily chart and "today" counts) are those of the JVM's time zone (`TZ` or `-Duser.timezone`). Keep the timeouts too (`connectTimeout=10000&socketTimeout=120000`, in ms): a connection that went silent (e.g. half-open after a network failure) then fails instead of holding a request or a scheduled job up forever. Raise `socketTimeout` if a slow statistics query of the administration hits it. |
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

The judgers publish the progress of their judgings (shown live on a submission's
page) on the topic `vojJudgeProgressTopic`, which every instance of the web app
subscribes to: the viewers of a judging see its progress whichever instance
serves them. The messages are live updates, the database holds the verdicts:
they are not persistent, expire after 5 minutes (by the clocks of the hosts, which
must not be further apart than that), and an instance that is not running misses
them; a viewer's page reads the verdict from the database every 10 seconds
anyway. The web app publishes there too, the System Error of a submission it
marks itself (see below), so that every instance ends the live views of it. It
survives a restart of an external broker: its listener tries to subscribe again
every 5 seconds until the broker is back.

| Property | Default | Purpose |
| --- | --- | --- |
| `jms.broker.url` | `tcp://localhost:61616` | ActiveMQ broker the web app receives the judgers' messages from. |

### Judging queue

The queue of the submissions to judge is the database: a pending submission
waits in `voj_submissions` until a judger that supports its language takes it
(see [Judging queue](#judging-queue-1) of the judger). Every judger records a
heartbeat in `voj_judgers` every 10 seconds: the languages it supports, whether
it takes submissions, and the load of its host. A judger whose last heartbeat is
more than 30 seconds old (by the database's clock) counts as stopped; every
judger process has an entry of its own, and the first heartbeat of a process
removes those of the processes that stopped a day ago. The judgers pages of the
site and of the administration show the running judgers from it.

| Property | Default | Purpose |
| --- | --- | --- |
| `judging.noJudgerTimeout` | `300000` | How long (ms) the pending submissions in a language wait while no running judger takes submissions in it, before the web app marks them as a System Error that says so (an administrator can rejudge them): no judger supports the language, the ones that do stopped, or they are not ready as their judgings failed for reasons of their own (see [Judging queue](#judging-queue-1) of the judger). A queue however long is never failed while a judger takes submissions in its language, and a submission a judging holds is left to it. `0` (or less): a submission waits for a judger as long as it takes. |

Every 30 seconds, each instance of the web app looks for the pending submissions
that no judging holds in a language without a judger that takes submissions,
and marks them once it saw that language without one for the whole timeout, run
after run: a run that fails (e.g. the database is unreachable) and a start of the
instance start the timeout over, so that heartbeats that could not be recorded
meanwhile never make running judgers look stopped. The live views of a
submission it marks end with the System Error, on every instance.

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
settings above. Its `jdbc.url` has a `connectTimeout` of 10 seconds and a
`socketTimeout` of 15 seconds: every statement of a judger is short, and a
connection that went silent must fail well within the lease of a judging (60
seconds). Judger-specific keys:

### Message queue

The judger dials the same `jms.broker.url` as the web app, a plain `tcp://` URL,
and publishes the progress of its judgings on the topic `vojJudgeProgressTopic`
(see [Message queue](#message-queue) of the web app). It sends in the
background, so a judging never waits for the broker: a send that the broker
holds up by its flow control fails after 10 seconds, and while 1,000 messages
wait to be sent, further ones are dropped. When the broker goes away (in the
Docker setup it lives in the web container, so it restarts with it), a message
that cannot be sent is dropped and the next one is sent over a new connection:
the verdicts are in the database, the messages only stream the progress. Its
heartbeats go to the database, not to the broker.

### Judging queue

The judger takes the pending submissions from the database, in the languages
whose compilers it found at startup, the one submitted (or rejudged) first
first, and judges one at a time. Any number of judgers can share the database.
The policy of the queue is the same on every judger, so it has no settings:

- A judger claims a submission with a statement that only one judger can
  succeed with; the claim gives the judging a lease of 60 seconds. An idle
  judger looks for a submission every 2 seconds.
- While the judging runs, the judger renews its lease in the background every
  15 seconds, however long a compilation or a run takes, until its deadline:
  the download timeout, the wall-clock limits of the compilation and of every
  run (three times the time limit, plus a second), and the lease timeout. A
  judging past its deadline is stuck (e.g. on a connection that went silent):
  its lease is no longer renewed, so another judger takes the submission, and
  the judger is not ready until it is over.
- A judging that vanishes (its judger stopped, crashed, was killed or lost the
  database, until its lease expired) is counted as interrupted by the judger
  that takes the submission next. After 3 interrupted judgings, that judger
  judges the submission a System Error that says so, instead of judging it
  again: a submission that crashes every judger ends there too.
- A judging stops, without recording a verdict or sending anything more, once
  it finds (when it renews its lease) that it was superseded: the submission was
  rejudged, or taken by another judging as its lease had expired. The web app
  shows the messages of the current judging of a submission only.

Who is to blame for a judging that cannot be completed decides what becomes of
its submission:

| Failure | To blame | Outcome |
| --- | --- | --- |
| Compile error, wrong answer, time, memory or output limit, runtime error | The submission | Its verdict. |
| The web app has no test data for the problem (HTTP 404), its archive is malformed, or it has no checkpoints | The problem | System Error that says so. |
| An unexpected exception in the judger (a bug) | Unknown | System Error ("internal error"). |
| The test data cannot be downloaded: the web app is unreachable or restarting, a timeout, HTTP 5xx, 408 or 429 | The judger (or the web app) | Released for another judger, not recorded or counted; this judger leaves the problem to the others for 2 minutes. |
| The web app refuses this judger the test data (HTTP 401, 403 or another client error: e.g. a wrong `judger.apiToken`) | The judger | Same. |
| The judger cannot read its copy of the test data | The judger | Same. |
| The work directory cannot be used, or the source code not written | The judger | Released (or not claimed), not recorded or counted. |
| The sandbox cannot run the compiler or the program (it cannot be set up) | The judger | Same. |
| The database fails while the outcome is recorded | The judger or the database | The verdict is tried again (3 times in all, within a few seconds); then the submission is released, or, if the database is still unreachable, taken again once its lease expired (an interrupted judging). |
| The database refuses the outcome (e.g. a value its column does not take) | Unknown | System Error ("internal error"). |
| The judger stops, crashes or is killed while judging, or the judging runs past its deadline | Unknown | Taken again once its lease expired, as an interrupted judging; System Error after 3 of them. |
| An Error in the judger (e.g. `OutOfMemoryError`) | Unknown | The judger exits with status 70, for its supervisor to restart it (see [deployment](deployment.md#keeping-the-judgers-running)); the submission is an interrupted judging. |

A judger whose judgings fail for reasons of its own pauses: 5 seconds after the
first failure, twice as long after each next one in a row, at most 2 minutes.
After 3 failures in a row it is not ready (its heartbeat says so, which the web
app counts as no judger for its languages, see
[Judging queue](#judging-queue) of the web app) until one of its judgings
reaches an outcome again; it logs that once, and keeps trying every 2 minutes at
most. A failure of the database while it looks for a submission only makes it
look again every 2 seconds, logged once until it succeeds.

The leases and heartbeats are measured by the database's clock (`NOW()`), so the
clocks of the hosts do not matter.

### Identity

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.username` | `voj@judger` | Account this judger authenticates as (in the `judgers` group). Several judgers may run as one account: each process records heartbeats of its own; the public judgers page shows an account as online while any of them runs, the administration's every process. |
| `judger.password` | *(empty)* | Password for that account. |
| `judger.description` | *(empty)* | Free-text label shown in the admin console (helps tell judgers apart). |

### Directories

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.workDir` | — | No longer used: the judger judges in `work-<n>/` under `judger.checkpointDir`. A judger that finds it set (or `VOJ_JUDGER_WORK_DIR`) logs a warning and ignores it. |
| `judger.checkpointDir` | `/var/lib/voj-judger` (`%ProgramData%\voj-judger` on Windows) | The judger's own directory, never in the temporary directory (whose clean-up of old files would remove what the judger keeps there): the local cache of downloaded reference inputs/outputs, and the work directories of the judgings. Every judger on the host claims the first slot *n* here whose lock file `judger-<n>.lock` no other judger holds (held from its start until it exits). It caches in `judger-<n>/` (`0700`), so judgers sharing this setting never replace test data another one is reading, and judges in `work-<n>/` (`0711`): it creates a private `0700` subdirectory, `voj-<submissionId>-<random>`, there for every judging and removes it afterwards, however deep the tree a submission built there or whatever permissions it took away. When it claims its slot, the judger removes what a judger with that slot left in `work-<n>/` (one that was killed while judging), without following links; what it cannot remove does not keep it from starting: it moves the directory aside, to `trash-<n>-<random>/`, and removes that at a later start. The directory must belong to the judger's account, and other users (the group included) must not be able to write to it, as they could replace the test data or the lock files in it: the judger refuses to start otherwise, rather than change the permissions of a directory it did not create (e.g. `/tmp`). A missing one it creates with mode `0711`: others may pass through it, but neither list nor read anything in it but the work directory of a judging they are handed. A judger that may not create it (one that is not root may not create the default) does not start: create it for the judger's account, or set this (`VOJ_JUDGER_CHECKPOINT_DIR`) to a directory the judger may create. When the judger runs submissions as `system.username` (the native sandbox, as root), that account must be able to pass through every directory above `work-<n>/`: this directory and every one above it must let others pass (`o+x`), or the judger does not start. On Windows, it and the directories of the slots are created with ACLs of their own instead of modes (see [The checkpoint directory on Windows](deployment.md#the-checkpoint-directory-on-windows)). Directories named after a problem id directly under it are the cache of an older version and can be deleted. |

### Checkpoint download

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.web.baseUrl` | `http://localhost:8080/voj` | Web application root the judger downloads test data from. A problem the web app has no test data for (HTTP 404), or an archive that is malformed, is a System Error; any other failed download is a failure of the judger's own, and the submission is left to another judger (see [Judging queue](#judging-queue-1)). |
| `judger.web.downloadTimeout` | `300` | Seconds a download of a problem's test data may take as a whole, from the request to the last byte; a download that is not over by then (e.g. the web app stopped sending in the middle) fails. Raise it for very large test data on a slow link. |
| `judger.apiToken` | `${VOJ_JUDGER_API_TOKEN}` *(empty)* | Shared secret; **must match** `judger.apiToken` on the web app. Left empty (or wrong), the web app refuses every download (HTTP 403): the judger leaves the submissions to other judgers, logs that it was refused and to check `judger.apiToken`, and after three such failures in a row is not ready (it takes no submission but a retry every 2 minutes at most); if no judger is ready for a language, its pending submissions are judged a System Error after `judging.noJudgerTimeout`. The judger logs an error at startup when it is empty. (Any client error but 404, 408 and 429 is such a refusal; 404 means the problem has no test data.) The Docker images and `docker-compose.yml` default it to `verwandlung` on both sides; use a long random string in production. |

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
