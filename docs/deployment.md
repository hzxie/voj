# Deployment Guide

This document covers what the judger's sandbox expects from the host. The web
application has no special requirements beyond a reachable database and message
queue, so most of this is about running judgers securely; see
[Upgrading the web application](#upgrading-the-web-application) for the one-time
database scripts some releases need.

## Sandbox backends

The judger picks a sandbox backend with `judger.sandbox` in `voj.properties`:

- `native` (default): an in-process JNI runner. On Linux it confines each
  submission with `setrlimit`, a seccomp syscall filter (which also refuses
  network sockets), a private network namespace where one can be created, and a
  privilege drop. On Windows it runs the submission under a separate
  low-privilege account. This is the only option on Windows.
- `isolate` (Linux only): delegates to the external
  [isolate](https://github.com/ioi/isolate) tool. Choose this when you need full
  filesystem isolation (each submission gets its own mount namespace). It requires
  `isolate` to be installed and the judger to run as root. isolate runs box *n*
  as UID `first_uid + n` (its default `first_uid` is 60000), so the judger hands
  each submission's private work directory to that UID: set
  `judger.isolate.firstUid` to isolate's `first_uid` and `judger.isolate.boxId`
  to the box id. See [The isolate backend](#the-isolate-backend) for how the
  judger sets up each box.

Unless a section says otherwise, this guide describes the `native` backend on Linux.

## Running the native sandbox as root (recommended for production)

To isolate untrusted code, run the judger as root so it can drop privileges to an
unprivileged account before executing each submission.

### The unprivileged user

Set `system.username` to a non-root account that exists on the host. The default
is `nobody`, which is present on virtually every Linux system, so no setup is
needed. The password (`system.password`) is unused on Linux and is required only
on Windows.

When the judger runs as root, dropping privileges is mandatory and fails closed:
if the configured user does not exist, is root, or the switch fails, the
submission is refused rather than executed with root privileges. So if you change
`system.username`, make sure that account exists.

For a shared, multi-tenant host, prefer a dedicated account over `nobody`, because
`nobody` is shared with other system services. A dedicated account makes the
process-count limit (`RLIMIT_NPROC`) effective and keeps submissions from
interfering with other `nobody` processes.

**The account must be used by the judger only.** After every compile and run,
the native sandbox kills *every* process owned by `system.username`, so that a
submission cannot leave a process behind (for example via `setsid`) to tamper
with later submissions. Inside the Docker image nothing else runs as `nobody`, so
the default is safe there; on a bare host where other services run as `nobody`,
create a dedicated account instead, or those services will be killed:

```
sudo useradd --system --no-create-home --shell /usr/sbin/nologin voj-sandbox
# then set system.username = voj-sandbox in voj.properties
```

### What a submission is held to

- **Processes.** The sandbox user's `RLIMIT_NPROC` (which counts threads) is
  `judger.processLimit` (64) for a run and `judger.compileProcessLimit` (1024)
  for a compilation, whose toolchains are trusted but parallel (`go build`).
  The seccomp filter denies leaving the process group (`setsid`, `setpgid`) and
  creating namespaces (`unshare`, `setns`, `clone` with a `CLONE_NEW*` flag;
  `clone3` fails with `ENOSYS` so the C library falls back to `clone`), so a
  submission cannot obtain the capabilities of an unprivileged user namespace.
- **CPU time.** Every run gets a small supervisor process (a root-owned fork of
  the judger, so the submission can neither signal nor trace it) that is the
  *child subreaper* of the submission: every orphaned descendant is reparented to
  it and reaped by it, so the CPU time of a process that exits after being
  orphaned still counts. A run's time is the CPU time of all its processes; a
  compilation's is the compiler's top-level process (including what it waited
  for).
- **Memory.** A run's memory is the larger of the top-level process's peak RSS
  and the sum of the proportional set size (PSS, `/proc/<pid>/smaps_rollup`,
  Linux 4.14+) of all its processes, so pages shared after `fork` or by the
  runtime's shared libraries count once, not once per process. Without PSS
  (older kernels) the RSS is summed instead, which over-counts.
- **Network.** The seccomp filter refuses `socket` and `socketpair` with
  `EACCES` for every address family but `AF_UNIX`, and `io_uring` (which can
  create sockets on its own) with `ENOSYS`, so a submission cannot reach the
  judger's database or message queue, other hosts or the internet. This needs no
  privilege, so it holds in a Docker container without `CAP_SYS_ADMIN` too, where
  the private network namespace, still tried first, cannot be created. Compilers
  run under the same filter. `AF_UNIX` sockets stay available (runtimes use
  `socketpair`), so do not leave a Unix socket that the sandbox user may connect
  to (e.g. a database's or Docker's) where it can reach it.
- **Go.** `GOCACHE` and `GOPATH` point into the submission's own work directory,
  never at a cache other submissions can write: a shared build cache could be
  poisoned by one submission for every later Go build. Set
  `judger.goBuildCacheSeed` to a root-owned, read-only build cache (the Docker
  image builds one) to seed it, so the standard library is not rebuilt every time;
  the seeded files are hard links the sandbox user cannot modify.

### Filesystem layout and permissions

The judger manages two directories from `voj.properties`:

| Property | Default | Purpose | Permissions |
| --- | --- | --- | --- |
| `judger.workDir` | `/tmp` | Parent of the per-submission work directories | The judger (root) must be able to create subdirectories here |
| `judger.checkpointDir` | `/tmp/voj-testpoints` | Reference inputs and expected outputs | Kept readable only by the judger |

How it behaves at runtime:

- For each submission the judger creates a private work directory under
  `judger.workDir`, sets it to mode `0700` and, when running as root, hands it to
  `system.username`. The compiler and the submitted program run with dropped
  privileges and write their output here, so the work directory must be owned by
  (or writable by) the sandbox user. The judger does this automatically; you only
  need to ensure `judger.workDir` itself lets the judger create subdirectories.
- The judger writes each problem's reference data into `judger.checkpointDir` and
  sets the per-problem directory to mode `0700`. The submission never reads these
  files (it receives its input on stdin, and the judge compares the expected
  output itself), so the sandbox user is denied access and cannot read the
  answers.

Do not place secrets where the sandbox user can read them. The native backend
does not provide full filesystem isolation, so a submission running as the sandbox
user can still read world-readable files on the host. Keep `voj.properties` and
the judger jar readable only by the judger account (for example mode `0640`), and
use `judger.sandbox = isolate` if you need to prevent reads of arbitrary host
files entirely.

## The isolate backend

With `judger.sandbox = isolate` the judger (running as root) prepares an isolate
box for every compilation and run. isolate 2.7 built from source has been
verified with every language in `sql/seed.sql` (C, C++, Java, Pascal, Python 3,
Go, Kotlin, JavaScript and Rust).

- **Filesystem.** The box sees isolate's default mounts (`/bin`, `/lib`, `/usr`,
  ...) plus `judger.isolate.extraDirs` (by default `/etc/alternatives` and
  `/opt`, read-only), and the submission's work directory at `/work`, writable
  while compiling and read-only while running. The checkpoint directory is never
  bound: the input of the current run is copied into the box.
- **Compiler configuration.** isolate binds directories only, so configuration
  files a compiler expects outside the box are copied into it before a
  compilation (`judger.isolate.compileFiles`): Free Pascal's `/etc/fpc.cfg`, which
  holds its unit search paths, becomes `~/.fpc.cfg` in the box.
- **Environment.** As with the native backend, the box gets only the locale and
  toolchain variables of the judger (no secrets), `HOME=/box`, and a Go build
  cache and `GOPATH` of its own in the work directory, seeded from
  `judger.goBuildCacheSeed`.
- **System calls.** isolate's own seccomp filter makes file locks fail (`flock`
  with `ENOSYS`), which the Go build cache cannot do without. Compilations, which
  run a trusted compiler rather than the submission, are given
  `--syscalls=27` (`judger.isolate.compileSyscalls`: every restriction but the
  one on file locks); runs of the submission keep all of them. For an isolate
  without `--syscalls`, set `judger.isolate.compileSyscalls` empty.
- **Network.** isolate gives each box its own, empty network namespace.

## Native build prerequisites (Linux)

Building the judger compiles its JNI library, which needs a C++ toolchain and
libseccomp:

```
sudo apt-get install -y g++ make libseccomp-dev
```

On some JDK layouts the build also needs `jni_md.h` and `jawt_md.h` copied from
`$JAVA_HOME/include/linux` up into `$JAVA_HOME/include`.

## Docker

The published judger image already runs as root and drops to `nobody`, and it
ships with `libseccomp` installed, so native sandboxing works without extra
configuration. See [`../docker/README.md`](../docker/README.md).

## Running without root (development and CI)

When the judger is not running as root it cannot drop privileges, so submissions
run as the same user as the judger. This is fine for development and continuous
integration but provides no isolation, so do not use it in production.

## Windows native sandbox

On Windows the native backend is the only option (`isolate` is Linux-only). Each
submission runs under a **separate low-privilege Windows account** inside a
[Job Object](https://learn.microsoft.com/windows/win32/procthread/job-objects)
that caps the process count (anti fork-bomb), enforces memory and wall-clock
limits, applies UI restrictions, and kills the whole process tree when the run
ends.

### The sandbox account

Unlike Linux, Windows needs a real account *with a password* to log the
submission in. Set both `system.username` and `system.password` to a dedicated
low-privilege local account (do not reuse the judger's own account):

```
net user voj-sandbox <StrongPassword> /add
```

Then set `system.username = voj-sandbox` and `system.password = <StrongPassword>`
in `voj.properties`.

Requirements for that account:

- It must have the **"Log on as a batch job"** right
  (`SeBatchLogonRight`) — the judger logs it in with `LOGON32_LOGON_BATCH`. Grant
  it via *Local Security Policy → Local Policies → User Rights Assignment → Log
  on as a batch job*, or `secedit`.
- It must have a **loadable user profile**; the judger creates each submission
  process with `LOGON_WITH_PROFILE`. Logging in once interactively, or letting
  the first batch logon create it, is enough.
- Keep it a plain member of *Users* (no admin rights). It only needs to read the
  submission's work directory under `judger.workDir` and execute the compiled
  program.

Because the credentials live in `voj.properties`, keep that file readable only by
the judger account.

### Native build prerequisites (Windows)

The JNI library is built with the MSVC toolchain (the Windows runner uses Win32
APIs — `CreateProcessWithLogonW`, Job Objects). Build with a Visual Studio C++
toolchain installed and a JDK whose `include/win32` headers are available to the
compiler.

## Upgrading the web application

The database is created by `sql/schema.sql` and `sql/seed.sql` on the first
launch only; there is no automatic migration on startup. When a release needs a
one-time data change on an existing installation, it ships a script under
[`../sql/upgrades/`](../sql/upgrades/) that you run once against the database.
New installations never need these scripts. Each script is safe to run again:
it records that it has run in `voj_options` and does nothing on later runs.

Back up the database first, then run the script while the old version is
stopped (or just before you switch to the new one), using the same credentials
as the web application:

```bash
mysql -u voj -p voj < sql/upgrades/<script>.sql
# Bundled Docker database (the old container is still running):
docker compose exec -T web mysql voj < sql/upgrades/<script>.sql
```

| Upgrading from | Script | What it does |
| --- | --- | --- |
| 1.0.2 or earlier | `decode-legacy-html-entities.sql` | Up to 1.0.2, discussion titles and replies, display names, locations, websites, social links and bios were stored HTML-escaped, so after the upgrade old rows show `&amp;`, `&lt;`, `&gt;` and `&nbsp;` literally. The script decodes them once. Run it before users post on the new version: its first run decodes every such row, so text typed as `&amp;` on the new version before that would be decoded too. |
