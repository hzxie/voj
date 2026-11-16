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
| `jdbc.initialSize` / `jdbc.maxActive` / `jdbc.minIdle` | `2` / `8` / `2` | Druid connection-pool sizing. |
| `jdbc.maxWait` | `30000` | Max wait (ms) for a pooled connection. |
| `jdbc.timeBetweenEvictionRunsMillis` | `60000` | Idle-connection eviction interval (ms). |
| `jdbc.minEvictableIdleTimeMillis` | `25200000` | Minimum idle time (ms) before a connection is evictable. |
| `jdbc.removeAbandoned` / `jdbc.removeAbandonedTimeout` | `false` / `1800` | Whether to reclaim a connection held for longer than N seconds. |

### Mail

The web app sends email (password-reset and email-verification links) through the
SMTP server `mail.host`, with STARTTLS and authentication. When it is empty, the
site sends no email: the password-reset page tells users to ask an administrator,
and email verification cannot be required (the setting is refused, and one
enabled before is not applied to new addresses).

| Property | Default | Purpose |
| --- | --- | --- |
| `mail.host` | *(empty)* | SMTP host. Empty disables outbound email. |
| `mail.port` | `587` | SMTP port (submission with STARTTLS). |
| `mail.username` / `mail.password` | *(empty)* | SMTP credentials. |
| `mail.senderMail` | `noreply@example.com` | `From` address. |
| `mail.senderName` | `Verwandlung Online Judge` | `From` display name. |

### Message queue

The judgers publish the progress of their judgings (shown live on a submission's
page) on the topic `vojJudgeProgressTopic`, which every instance of the web app
subscribes to: the viewers of a judging see its progress whichever instance
serves them. The messages are live updates, the database holds the verdicts:
they are not persistent, and an instance that is not running misses them; a
viewer's page reads the verdict from the database every 10 seconds anyway. The
web app only receives from the broker. It survives a restart of an external
broker: its listener keeps trying to subscribe again until the broker is back.

| Property | Default | Purpose |
| --- | --- | --- |
| `jms.broker.url` | `tcp://localhost:61616` | ActiveMQ broker the web app receives the judgers' messages from. |
| `jms.broker.embedded` | `false` | `true` runs a non-persistent broker inside the web app, which the judgers reach at `jms.broker.url`, instead of a standalone one (do not start both: they bind the same address). For a single web app instance only (see [deployment](deployment.md#several-web-application-instances)). |

### Judging queue

The queue of the submissions to judge is the database: a pending submission
waits in `voj_submissions` until a judger that supports its language takes it
(see [Judging queue](#judging-queue-1) of the judger). Every judger records a
heartbeat in `voj_judgers` every 10 seconds: the languages it supports, whether
it takes submissions, and the load of its host. A judger whose last heartbeat is
more than 30 seconds old (by the database's clock) counts as stopped; every
judger process has an entry of its own, and the first heartbeat of a process
removes those of the processes that stopped a day ago. The judgers pages of the
site and of the administration show the running judgers from it. Every count of
the online judgers (the home page, the administration's dashboard and judgers
page, the judgers page) counts the judger accounts that at least one running
judger process runs as: several processes running as one account are one judger
online (the administration's judgers page lists each process, with whether it
takes submissions and the languages it supports).

| Property | Default | Purpose |
| --- | --- | --- |
| `judging.noJudgerTimeout` | `300000` | How long (ms) the pending submissions in a language wait while no running judger takes submissions in it, before the web app marks them as a System Error that says so (an administrator can rejudge them): no judger supports the language, the ones that do stopped, or they are not ready as too many of their judgings in a row failed (see [Judging queue](#judging-queue-1) of the judger). A queue however long is never failed while a judger takes submissions in its language, and a submission a judging holds is left to it. `0` (or less): a submission waits for a judger as long as it takes. |

Every 30 seconds, each instance of the web app notes the languages that a
running judger taking submissions supports, and marks the pending submissions
that no judging holds in a language it has not seen supported for the timeout. A
start of the instance and a run that fails (e.g. the database is unreachable)
count as seeing every language supported, so that heartbeats that could not be
recorded meanwhile never make running judgers look stopped. The pages of a submission it
marks show the System Error at their next read of the database.

### Checkpoint distribution

Judgers pull a problem's test data from the web app over HTTP rather than sharing a filesystem.

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.apiToken` | `${VOJ_JUDGER_API_TOKEN}` *(empty)* | Shared secret a judger must send in the `X-Judger-Token` header to download test data from `/api/judger/checkpoints/{problemId}`. Must match `judger.apiToken` on every judger. A value **shorter than 16 characters (or empty) disables the endpoint** (all requests refused with HTTP 403, and an error is logged at startup), so a guessable token never exposes the reference answers. Use a long random string. |

The endpoint answers a problem that does not exist with HTTP 404 and the header
`X-Checkpoints-Missing: true`, the only answer a judger takes as the problem's
(it judges the submission a System Error that says the problem has no test
data, as it does for a problem without checkpoints). Any other failure of a
download, a 404 without that header included (e.g. a wrong
`judger.web.baseUrl`), is a failed judging.

### Judger account

Every judger signs in as an account of the `judgers` group (its
`judger.username` and `judger.password`, see [Identity](#identity)), and does
not start without one. Such an account cannot sign in to the site.

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.username` | `judger` (`VOJ_JUDGER_USERNAME`) | The account the judgers sign in as. |
| `judger.password` | *(empty)* (`VOJ_JUDGER_PASSWORD`) | Its password, 6 to 64 characters. When it is set, the web app makes the account a judger account with this password on every start: it creates it when no user has the name, or sets its password; a user of that name who is not a judger is left alone (an error is logged). Empty: the web app leaves the accounts alone, and an administrator creates the judgers' accounts (user group *Judgers*) in the administration. |

Set the same three values (`VOJ_JUDGER_USERNAME`, `VOJ_JUDGER_PASSWORD`, and
`VOJ_JUDGER_API_TOKEN`) on the web app and on every judger. The Docker image of
the web app generates the password on its first start when it is not set (see
[`../docker/README.md`](../docker/README.md#first-start)).

### Web service

| Property | Default | Purpose |
| --- | --- | --- |
| `url.base` | `http://localhost:8080/voj` | Public base URL; used to build absolute links (e.g. in emails). |
| `url.cdn` | `http://localhost:8080/voj/assets` | Base URL for static assets. Point at a CDN in production. |

Behind a reverse proxy, the web app takes the client address and scheme from the
`X-Forwarded-For` and `X-Forwarded-Proto` headers, but only from the proxies that
`VOJ_TRUSTED_PROXIES` matches (`server.tomcat.remoteip.internal-proxies` in
[`application.properties`](../web/src/main/resources/application.properties)): a
regular expression of addresses, by default the loopback ones, for a proxy on the
same host. The Docker image adds the gateway of its network, which a proxy on the
Docker host connects from. Failed logins are counted per client address (an IPv6
one by its /64), so a client must never be able to choose it: match exactly the
address a proxy connects from (e.g. `10\.0\.0\.5`), never a whole network, and
have the proxy replace the `X-Forwarded-For` a client sends (nginx:
`proxy_set_header X-Forwarded-For $remote_addr;`; see
[`../docker/reverse-proxy/`](../docker/reverse-proxy/)).

## Judger

The judger shares the database (`jdbc.*`) and message-queue (`jms.broker.url`)
settings above. Its `jdbc.url` has a `connectTimeout` of 10 seconds and a
`socketTimeout` of 15 seconds: every statement of a judger is short, and a
connection that went silent must fail well within the lease of a judging (60
seconds). For the same reason its `jdbc.maxWait` is 5 seconds: a statement
waits that long at most for a connection of the pool. Judger-specific keys:

### Message queue

The judger dials the same `jms.broker.url` as the web app, a plain `tcp://` URL,
and publishes the progress of its judgings on the topic `vojJudgeProgressTopic`
(see [Message queue](#message-queue) of the web app). It sends in the
background, so a judging never waits for the broker: a send that the broker
holds up by its flow control fails after 10 seconds, and while 1,000 messages
wait to be sent, further ones are dropped. When the broker goes away (in the
Docker setup it lives in the web container, so it restarts with it), a message
that cannot be sent is dropped and the next one is sent over a new connection:
the verdicts are in the database, the messages only stream the progress. The
first message of an outage that cannot be sent is logged, and so is the end of
the outage, not every message. Its heartbeats go to the database, not to the
broker.

### Judging queue

The judger takes the pending submissions from the database, in the languages
whose compilers it found, the one queued first (when it was submitted,
rejudged, or released after a failed judging) first, and judges one at a time.
Any number of judgers can share the database. A language's compiler (the first
word of its compile command) is found when it prints its version and exits
with 0 within 30 seconds: `javac -version`, `kotlinc -version`, `go version`,
`fpc -iV`, otherwise `<compiler> --version`. A language whose source file name
(e.g. `Main.cpp`) is empty or not a plain file name is not judged, and the
judger logs an error for it. The judger looks when it starts, and again (in
the background, while it judges) once an administrator adds, removes or
changes a language (its commands or source file name), which it notices within
a heartbeat (10 seconds): a language added in the administration needs no
restart. Restart a judger after installing a compiler on its host, unless the
languages change anyway.
The policy of the queue is the same on every judger, so it has no settings:

- A judger claims a submission with a statement that only one judger can
  succeed with; the claim gives the judging a lease of 60 seconds. An idle
  judger looks for a submission every 2 seconds.
- While the judging runs, the judger renews its lease in the background every
  15 seconds, however long a compilation or a run takes. Each step of a
  judging is bounded by its own limit: the download timeout
  (`judger.web.downloadTimeout`), the timeouts of the database statements, and
  the wall-clock limit of a compilation or run (twice its time limit, plus a
  second).
- A judging stops, without recording a verdict or sending anything more, once
  it finds (when it renews its lease) that it was superseded: the submission was
  rejudged, or taken by another judging as its lease had expired. The web app
  shows the messages of the current judging of a submission only.
- A judger that is stopped (e.g. `docker stop`, a rolling update, `systemctl
  stop`) releases the submission it is judging at its place in the queue, for
  another judger to take at once, without counting a failed judging on it.

A judging either reaches an outcome or fails. The outcomes are the verdicts of
the submission (Accepted, Wrong Answer, Compile Error, time, memory or output
limit, runtime error, ...) and one System Error: the web app says that the
problem has no test data (a marked HTTP 404, see
[Checkpoint distribution](#checkpoint-distribution)), or its test data has no
checkpoints. Every other judging that ends without an outcome failed, in one of
two ways:

- **Possibly the submission's doing:** the sandbox fails while running its
  compiler or its program (e.g. as the submission broke its own runs, or the
  sandbox is broken on the host: a judger cannot tell), the database refuses
  its verdict, an unexpected exception (a bug it may trigger), or the judging
  vanished: its judger stopped, crashed or was killed while judging, and its
  lease expired. The judger releases the submission to the end of the queue (so
  that it holds no other submission up) and counts the failed judging on it.
- **The judger's or its environment's:** the test data cannot be downloaded
  (the web app is unreachable or refuses the judger, e.g. for a wrong
  `judger.apiToken`, a 404 without the mark, e.g. for a wrong
  `judger.web.baseUrl`, an archive that is malformed) or read, the source code
  cannot be written, or the database is unreachable when the verdict is recorded.
  The judger releases the submission to the end of the queue and counts nothing
  on it, so an outage of the web app or the database does not use up its budget,
  and a submission whose judgings fail this way every time (e.g. as its test
  data is broken) holds no other submission up. A submission that cannot even be
  released, as the database is down, is left to its lease, which counts one
  failed judging. The judgers then soon are not ready, and an outage longer than
  `judging.noJudgerTimeout` of the web app has the waiting submissions judged a
  System Error (see [Judging queue](#judging-queue)).

The judger tries a verdict, and the release of a submission, 3 times before it
gives up on it, a second after the first try and two seconds after the second;
a try fails once it got no connection of the pool within `jdbc.maxWait` (5
seconds) or no answer within the `socketTimeout` of `jdbc.url` (15 seconds). A
submission that the judger claimed but could not read (e.g. the database went
away meanwhile) is released at once, rather than held until its lease expired.

Two budgets, independent of each other, keep failed judgings from going on for
ever:

| Budget | Counts | When it is spent |
| --- | --- | --- |
| The submission's: 5 failed judgings | The failed judgings it may have caused, with their reasons; a vanished judging is counted by the judger that takes the submission once its lease expired. A rejudge starts it over (a submission still waiting for a judger is left alone). | The judger that takes it after the 5th judges it a System Error that gives the number of failed judgings and the reason of the last one (an administrator can rejudge it). |
| The judger's: 3 failed judgings in a row | Every failed judging of the judger, in memory (a restarted judger starts from none). A judging that reaches an outcome ends the run of failures. | The judger is not ready (its heartbeat says so, which the web app counts as no judger for its languages, see [Judging queue](#judging-queue) of the web app) until one of its judgings reaches an outcome again. After every failed judging it pauses: 5 seconds after the first one, twice as long after each next one in a row, at most a minute (so, once it is not ready, it tries again after 20 seconds, 40 seconds, then every minute). It logs every failure, and an error when it is no longer ready, and keeps trying. |

An `Error` in the judger (e.g. `OutOfMemoryError`), while it judges or renews
the lease of a judging, leaves it in no state to judge: it logs it and exits
with status 70, for its supervisor to restart it (see
[deployment](deployment.md#keeping-the-judgers-running)); its judging is a
vanished one. A failure of the database while a judger looks for a submission
only makes it look again every 2 seconds, logged once until it succeeds.

The leases and heartbeats are measured by the database's clock (`NOW()`), so the
clocks of the hosts do not matter.

### Identity

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.username` | `judger` | Account this judger authenticates as (in the `judgers` group, see [Judger account](#judger-account)); the judger does not start without it. Several judgers may run as one account: each process records heartbeats of its own; the account is one judger online while any of them runs (the administration's judgers page lists every process). |
| `judger.password` | *(empty)* | Password for that account. |
| `judger.description` | *(empty)* | Free-text label shown in the admin console (helps tell judgers apart). |

### Directories

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.checkpointDir` | `/var/lib/voj-judger` (`%ProgramData%\voj-judger` on Windows) | The judger's own directory, never in the temporary directory (whose clean-up of old files would remove what the judger keeps there): the local cache of downloaded reference inputs/outputs, and the work directories of the judgings. Every judger on the host claims the first slot *n* here whose lock file `judger-<n>.lock` no other judger holds (held from its start until it exits). It caches in `judger-<n>/` (`0700`), so judgers sharing this setting never replace test data another one is reading, and judges in `work-<n>/` (`0711`): it creates a subdirectory of its own, `voj-<submissionId>-<random>` (`0711`), there for every judging, with the submission's program directory and the work directory of its runs in it (see [Filesystem layout and permissions](deployment.md#filesystem-layout-and-permissions)), and removes it afterwards, however deep the tree a submission built there or whatever permissions it took away. When it claims its slot, the judger removes what a judger with that slot left in `work-<n>/` (one that was killed while judging), without following links; what it cannot remove does not keep it from starting: it moves the directory aside, to `trash-<n>-<random>/`, and removes that at a later start. The directory must belong to the judger's account, and other users (the group included) must not be able to write to it, as they could replace the test data or the lock files in it: the judger refuses to start otherwise, rather than change the permissions of a directory it did not create (e.g. `/tmp`). A missing one it creates with mode `0711`: others may pass through it, but neither list nor read anything in it but the work directory of a judging they are handed. A judger that may not create it (one that is not root may not create the default) does not start: create it for the judger's account, or set this (`VOJ_JUDGER_CHECKPOINT_DIR`) to a directory the judger may create. When the judger runs submissions as `system.username` (the native sandbox, as root), that account must be able to pass through every directory above `work-<n>/`: this directory and every one above it must let others pass (`o+x`), or the judger does not start. On Windows, it and the directories of the slots are created with ACLs of their own instead of modes (see [The checkpoint directory on Windows](deployment.md#the-checkpoint-directory-on-windows)). |

### Checkpoint download

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.web.baseUrl` | `http://localhost:8080/voj` | Web application root the judger downloads test data from. A problem the web app says it has no test data for (HTTP 404 with the header `X-Checkpoints-Missing`) is a System Error; any other failed download (a wrong URL, whose 404 has no such header, included) is a failed judging (see [Judging queue](#judging-queue-1)), and the judger logs the status and, for a 404, to check this setting. |
| `judger.web.downloadTimeout` | `300` | Seconds a download of a problem's test data may take as a whole, from the request to the last byte; a download that is not over by then (e.g. the web app stopped sending in the middle) fails. Raise it for very large test data on a slow link. |
| `judger.apiToken` | `${VOJ_JUDGER_API_TOKEN}` *(empty)* | Shared secret; **must match** `judger.apiToken` on the web app. Left empty (or wrong), the web app refuses every download (HTTP 403): every judging of the judger that needs test data it has not cached fails (the submission is released to the end of the queue, without counting the judging on it, for another judger), the judger logs the status and to check `judger.apiToken`, and after three failed judgings in a row it is not ready (it takes no submission but a retry after 20 seconds, 40 seconds, then every minute); if no judger is ready for a language, its pending submissions are judged a System Error after `judging.noJudgerTimeout`. The judger logs an error at startup when it is empty. The web application refuses tokens shorter than 16 characters. The web Docker image generates one when none is given, and `docker-compose.yml` requires it in `.env`; use a long random string. |

### Sandbox

Every limit here must be positive: nothing is compiled or run without a limit, and the judger does not start with a limit of zero or less.

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.sandbox` | `native` | Sandbox backend. `isolate` (Linux, as root, with [isolate](https://github.com/ioi/isolate) installed) runs every program in an isolate box: recommended for Linux in production. `native` is the JNI runner (Linux: rlimits, seccomp filters, Landlock, which needs Linux 5.13+ with Landlock enabled, and `system.username` when the judger is root; Windows: a restricted Low-integrity token of `system.username`, a Job Object and a network filter): the backend on Windows, and on Linux for a judger that is not root. Both enforce the same commands and limits and are judged alike. The judger does not start with any other value, nor with `isolate` when it is not root. See [`deployment.md`](deployment.md#sandbox-backends). |
| `judger.compileTimeLimit` | `20000` | CPU time (ms) a compilation may use: of the compiler and every program it runs. The wall-clock limit of a compilation or run is twice its time limit plus a second. |
| `judger.compileMemoryLimit` | `524288` | Memory (KB) a compilation may use, scaled by the language's memory multiplier like a run's limit. The native sandbox measures the compiler's top-level process against it and holds every process it runs to a kernel backstop of twice the limit plus max(256 MB, RAM / 32) (`RLIMIT_DATA`). The base 512 MB caps C/C++/Go/Rust/Pascal "compiler bombs"; Kotlin (multiplier 2) gets 1 GB, since `kotlinc` is a JVM with a 512 MB heap of its own. |
| `judger.processLimit` | `64` | Threads a run of a submission may have at once on Linux: the native sandbox holds a run to a single process, and the sandbox user's `RLIMIT_NPROC` counts its threads; isolate's `--processes` counts processes and threads alike, and lets a run fork within it (see [`deployment.md`](deployment.md#the-isolate-backend)). On Windows, whose Job Objects count processes only, a run is held to its one process (and the console host Windows starts for it) whatever this is set to. |
| `judger.compileProcessLimit` | `1024` | Processes and threads a compilation may have at once (both backends). Higher than `judger.processLimit` because the Go toolchain runs a compiler per CPU, each with up to a thread per CPU: a cold `go build` used ~100 threads on a 24-core host. |
| `judger.outputLimit` | `262144` | Largest file (KB) a compilation or run may write, its output included (`RLIMIT_FSIZE`, isolate's `--fsize`; on Windows, the output). A run whose output reaches it is Output Limit Exceeded. |
| `judger.goBuildCacheSeed` | `${VOJ_GO_BUILD_CACHE_SEED}` *(empty)* | A read-only, root-owned Go build cache that each Go submission's private build cache is seeded from (hard links, or copies across file systems). Empty: every Go build starts from an empty cache. The Docker image builds one at `/opt/go-build-cache`. |
| `judger.isolate.executable` | `isolate` | Path to the `isolate` binary. Used only when `judger.sandbox = isolate`. |
| `judger.isolate.boxId` | `0` | isolate box id for this judger. Give each judger on the same host a distinct id. |
| `judger.isolate.firstUid` | `60000` | isolate's first UID: box *n* runs as UID `first_uid + n`, and the judger hands each program directory (for the compilation) and the work directory of each run to that UID. Must match isolate's configuration: its `first_uid`, or, under isolate 2's default `subid_user = isolate`, the start of the `isolate` user's range in `/etc/subuid` (see [The isolate backend](deployment.md#the-isolate-backend)). |
| `judger.isolate.extraDirs` | `/etc/alternatives:maybe,/opt:maybe` | Extra directories bound read-only into the box (isolate `--dir` rules, comma-separated), for toolchains outside isolate's default mounts. |
| `judger.isolate.compileFiles` | `.fpc.cfg=/etc/fpc.cfg` | Host files copied into a compilation's work directory, its `HOME` (`name=hostPath`, comma-separated), for compiler configuration files isolate cannot bind (it binds directories only). |
| `judger.isolate.compileSyscalls` | `27` | isolate `--syscalls` flags for a compilation: every restriction but the one on file locks, which the Go build cache needs. Runs keep isolate's configured flags. Empty: use isolate's configuration (e.g. for an isolate without `--syscalls`). |

### Privilege drop

| Property | Default | Purpose |
| --- | --- | --- |
| `system.username` | `nobody` | Unprivileged account the native sandbox runs programs as. On Linux any non-root account works; `nobody` is present out of the box. When the judger runs as root, dropping privileges is mandatory and fails closed. A dedicated account is preferable on a host where other services use `nobody`. Every judger on a host needs an account of its own, as it needs an isolate box id of its own: on Linux a judger running as root holds a lock on `/run/voj-sandbox-<uid>.lock` (the temporary directory if `/run` is not writable) and refuses to start when another judger on the host already uses the account; on Windows a judger holds the mutex `Global\voj-sandbox-<SID>` and runs nothing while another one holds it. See [`deployment.md`](deployment.md#several-judgers-on-one-host). |
| `system.password` | *(empty)* | Windows only: the password the judger logs the account on with, once (it is not kept). A judger running as root on Linux switches to the account without one. |
