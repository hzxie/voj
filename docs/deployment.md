# Deployment Guide

This document covers what the judger's sandbox expects from the host. The web
application has no special requirements beyond a reachable database and message
queue, so most of this is about running judgers securely.

## Sandbox backends

The judger picks a sandbox backend with `judger.sandbox` in `voj.properties`:

- `native` (default): an in-process JNI runner. On Linux it confines each
  submission with `setrlimit`, a private network namespace, a seccomp syscall
  filter and a privilege drop. On Windows it runs the submission under a separate
  low-privilege account. This is the only option on Windows.
- `isolate` (Linux only): delegates to the external
  [isolate](https://github.com/ioi/isolate) tool. Choose this when you need full
  filesystem isolation (each submission gets its own mount namespace). It requires
  `isolate` to be installed and the judger to run as root.

The rest of this guide describes the `native` backend on Linux.

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
interfering with other `nobody` processes:

```
sudo useradd --system --no-create-home --shell /usr/sbin/nologin voj-sandbox
# then set system.username = voj-sandbox in voj.properties
```

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
