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
| `judger.apiToken` | `change-me-shared-secret` | Shared secret a judger must send in the `X-Judger-Token` header to download test data from `/api/judger/checkpoints/{problemId}`. Must match `judger.apiToken` on every judger. An **empty value disables the endpoint** (all requests refused). Use a long random string in production. |

### Web service

| Property | Default | Purpose |
| --- | --- | --- |
| `url.base` | `http://localhost:8080/voj` | Public base URL; used to build absolute links (e.g. in emails). |
| `url.cdn` | `http://localhost:8080/voj/assets` | Base URL for static assets. Point at a CDN in production. |

## Judger

The judger shares the database (`jdbc.*`) and message-queue (`jms.broker.url`)
settings above. Judger-specific keys:

### Identity

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.username` | `voj@judger` | Account this judger authenticates as. |
| `judger.password` | *(empty)* | Password for that account. |
| `judger.description` | *(empty)* | Free-text label shown in the admin console (helps tell judgers apart). |

### Directories

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.workDir` | `/tmp` | Parent of per-submission work directories. The judger creates a private `0700` subdirectory per submission. |
| `judger.checkpointDir` | `/tmp/voj-testpoints` | Local cache of downloaded reference inputs/outputs, kept readable only by the judger. |

### Checkpoint download

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.web.baseUrl` | `http://localhost:8080/voj` | Web application root the judger downloads test data from. |
| `judger.apiToken` | `change-me-shared-secret` | Shared secret; **must match** `judger.apiToken` on the web app. |

### Sandbox

| Property | Default | Purpose |
| --- | --- | --- |
| `judger.sandbox` | `native` | Sandbox backend. `native` is the in-process JNI runner (Linux: `setrlimit` + network namespace + seccomp + privilege drop; Windows: separate low-privilege account). `isolate` (Linux only) delegates to the external [isolate](https://github.com/ioi/isolate) tool. See [`deployment.md`](deployment.md). |
| `judger.isolate.executable` | `isolate` | Path to the `isolate` binary. Used only when `judger.sandbox = isolate`. |
| `judger.isolate.boxId` | `0` | isolate box id for this judger. Give each judger on the same host a distinct id. |
| `judger.isolate.maxProcesses` | `64` | Process/thread cap inside the isolate box. |
| `judger.isolate.wallTimeBufferSec` | `1.0` | Extra wall-clock seconds added on top of the CPU-time limit before isolate hard-kills a run. |

### Privilege drop

| Property | Default | Purpose |
| --- | --- | --- |
| `system.username` | `nobody` | Unprivileged account the native sandbox drops to. On Linux any non-root account works; `nobody` is present out of the box. When the judger runs as root, dropping privileges is mandatory and fails closed. See [`deployment.md`](deployment.md). |
| `system.password` | *(empty)* | Unused on Linux; required on Windows for the separate low-privilege logon account. |
