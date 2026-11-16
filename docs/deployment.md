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
  network sockets and System V IPC), Landlock rules that confine its writes to
  its own work directory, a private network namespace where one can be created,
  and a privilege drop. It needs Linux 5.13 or later with Landlock enabled (see
  [Kernel requirements](#kernel-requirements)). On Windows it runs the
  submission under a separate low-privilege account. This is the only option on
  Windows.
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

The judger never touches other processes of the account: nothing a submission
starts can outlive its run (see below), so there is nothing to sweep up
afterwards, and `nobody` works on a bare host as well as in the Docker image.
On a host where other services run as `nobody`, a dedicated account is still the
better choice, for two reasons: `RLIMIT_NPROC`, which caps a submission's
threads, counts every process and thread of the account, so busy `nobody`
services eat into it (a run that cannot even start is then a system error); and
processes of the same account that are not sandboxed can signal a submission
(a submission itself can only signal processes outside its sandbox on kernels
older than 6.12, see below).

```
sudo useradd --system --no-create-home --shell /usr/sbin/nologin voj-sandbox
# then set system.username = voj-sandbox in voj.properties
```

### What a submission is held to

- **System calls.** A run of the submission may only make the system calls on
  an allowlist, which starts from Docker's default seccomp profile and leaves
  out what a judged program has no business with (other processes, the
  resource limits of any process but itself, the network beyond socket pairs,
  links, file owners, modes and extended attributes, inotify, `sync`, Linux
  AIO, `io_uring`, credentials, scheduling, ...); any other system call,
  including one a future kernel adds, fails with `ENOSYS`, so the C library
  and the runtimes fall back where they can. Compilations run trusted
  toolchains, too varied to allowlist, under a deny-list instead. The
  rules below hold for both: where they refuse a compilation a system call with
  `EPERM` or `EACCES`, a run gets the same error, even when its allowlist alone
  would refuse the call.
- **Processes.** A run of the submission is a single process: it may create
  threads, but the seccomp filters make `clone` without `CLONE_THREAD` (the C
  library's `fork`, `posix_spawn`, `system`) fail with `EPERM`, and the `fork`
  and `vfork` system calls are not on its allowlist. Its threads are capped by
  the sandbox user's `RLIMIT_NPROC`, `judger.processLimit` (64), which counts
  every process and thread of that user.
  A compilation runs a trusted toolchain, which may start other programs (the
  `gcc` driver, `go build`), under `judger.compileProcessLimit` (1024). Every
  process of a compilation stays in the compiler's process group, since the
  filters deny leaving it (`setsid`, `setpgid`); the whole group is killed when
  a limit is hit and as soon as the compiler exits, so nothing outlives a
  compile or a run. The filters also deny `ptrace`, `mount`, module loading
  and the like, and creating namespaces (`unshare`, `setns`, `clone` with a
  `CLONE_NEW*` flag; `clone3` fails with `ENOSYS` so the C library falls back
  to `clone`), so a submission cannot obtain the capabilities of an
  unprivileged user namespace.
- **CPU time.** A run's time is the CPU time (user + system, every thread) of
  its process, as the kernel accounts it when the process is reaped. A watchdog
  polls it every millisecond and kills the program once it exceeds the limit,
  backed by `RLIMIT_CPU` (the limit rounded up to whole seconds, plus one) and a
  wall-clock limit of three times the time limit plus one second (for a program
  that sleeps or blocks). A program that finishes over the limit before the
  watchdog stops it is Time Limit Exceeded all the same. A compilation's time is
  that of the compiler's top-level process, including the children it waited
  for, under `judger.compileTimeLimit`.
- **Memory.** A run's memory is the peak resident set size of its process
  (`VmHWM`, polled from `/proc` every millisecond; the program is killed once it
  exceeds the limit). The kernel backs this with `RLIMIT_DATA` at twice the
  limit plus max(256 MB, RAM / 32), which bounds how far a process can grow
  between two polls; a single allocation larger than that (say,
  `std::vector<int>(1e9)` under 256 MB) is stopped by the seccomp filters and is
  Memory Limit Exceeded too, rather than the runtime error its failure would
  look like. A program that would have handled such a failed allocation is
  stopped there instead. Allocations that exceed the backstop only together
  with earlier untouched ones, or by growing with `brk`/`mremap`, still just
  fail and are judged by how the program then ends; with the backstop at more
  than twice the limit, that is rare. Memory that is reserved but never touched
  does not count. A compilation's memory is that of the compiler's top-level
  process, under `judger.compileMemoryLimit`; every process it runs is held to
  the same `RLIMIT_DATA` backstop, derived from that limit. A program whose
  static data alone (`static int a[1 << 30];`) exceeds the backstop cannot even
  be loaded; it is not run and is Memory Limit Exceeded, rather than the
  `SIGSEGV` the kernel would kill it with while loading it. A run cannot create
  memory files (`memfd_create` is not on its allowlist) or shared anonymous
  mappings, whose contents would count against no limit once dropped from its
  resident set, nor push its pages out of that set (`madvise` with
  `MADV_PAGEOUT` or `MADV_COLD`; `process_madvise` is not allowlisted) to keep
  its peak low on a host with swap.
- **Output.** A file the program writes, standard output included, may reach
  256 MB (`RLIMIT_FSIZE`; fixed). A run that hits the limit is Output Limit
  Exceeded, whether the kernel's `SIGXFSZ` kills it or it ignores the signal,
  so that its writes fail, and its output has reached the limit.
- **Standard streams and descriptors.** A run reads its standard input from the
  test's input file and a compilation from `/dev/null`; standard output goes to
  the output file (for a compilation, standard error too: the compile log), and
  a run's standard error is discarded. No other descriptor of the judger is
  inherited. A run may have at most 128 descriptors open at once and a
  compilation's processes 4096 each (`RLIMIT_NOFILE`, soft and hard alike, so it
  cannot be raised): an open pipe or socket pair can hold kernel memory that no
  memory limit sees. A run's threads must share its descriptor table (`clone`
  with `CLONE_THREAD` but without `CLONE_FILES` fails with `EPERM`), so the
  limit is the run's, not each thread's. Compilations and runs start in the
  submission's work directory.
- **Environment.** Only the locale and toolchain variables of the judger are
  passed on (`PATH`, `LANG`, `JAVA_HOME`, the Go and Rust settings, ...), never
  its secrets, with `HOME` and `TMPDIR` set to the submission's work directory.
- **Files and shared state.** Every compilation and run is the same sandbox
  user, so whatever one could leave behind outside its work directory would
  outlive it and could be read by a later submission (say, a run that copies its
  hidden input to `/tmp`, for a later compilation to `#include` it into its
  compile log), and could hold memory or disk no limit accounts for. So
  compilations and runs can create, write, truncate, rename or remove files only
  in their own work directory, which is removed once the submission is judged
  (enforced with [Landlock](https://docs.kernel.org/userspace-api/landlock.html);
  `/dev/null` stays writable): not in `/tmp`, `/var/tmp`, `/dev/shm` or anywhere
  else the sandbox user could otherwise write. Compilers find their temporary
  directory and caches there through `TMPDIR` and `HOME`. System V shared
  memory, semaphores and message queues, POSIX message queues and the kernel
  keyrings (`add_key`, `request_key`, `keyctl`: a user's keyrings belong to the
  user, not to a process, so they outlive every run) are refused (`EPERM`) by
  the seccomp filters, for compilations and runs alike. Landlock also denies a
  submission `ptrace` access (including `/proc/<pid>/fd`, `/proc/<pid>/mem`,
  ...) to any process outside its own sandbox, such as a concurrent run on
  another judger, and from Linux 6.12 (Landlock ABI 6) on keeps it from
  signalling such processes or connecting to their abstract Unix sockets.
  Reading is not restricted: a submission can read whatever the sandbox user
  can (see below), including, while they exist, the work directories of
  submissions judged at the same time by other judgers on the host that use the
  same `system.username` — give each judger on a host its own sandbox account if
  that matters.
- **Network.** The seccomp filters refuse `socket` and `socketpair` with
  `EACCES` for every address family but `AF_UNIX`, and `io_uring` (which can
  create sockets on its own) with `ENOSYS`, for compilations and runs alike, so
  a submission cannot reach the judger's database or message queue, other hosts
  or the internet. This needs no privilege, so it holds in a Docker container
  without `CAP_SYS_ADMIN` too, where the private network namespace, still tried
  first, cannot be created. `AF_UNIX` socket pairs stay available (runtimes use
  `socketpair`). A run cannot `connect`, `bind`, `listen` or `accept` at all
  (they are not on its allowlist), but a compilation can, so do not leave a Unix
  socket that the sandbox user may connect to (e.g. a database's or Docker's)
  where it can reach it.
- **Failing closed.** The seccomp filters, the Landlock rules and, when the
  judger runs as root, the privilege drop (including dropping supplementary
  groups) are mandatory, as are the resource limits and the standard streams.
  If any of them cannot be set up, or the program cannot be executed at all, it
  is not run and is judged a system error, never a runtime or compile error.
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
| `judger.checkpointDir` | `/tmp/voj-testpoints` | Reference inputs and expected outputs, and the lock files of the judgers' slots | Must belong to the judger and not be writable by its group or others (the judger refuses to start otherwise, and creates a missing one with mode `0700`) |

How it behaves at runtime:

- For each submission the judger creates a private work directory in its own
  directory under `judger.workDir` (`voj-judger-<h>-<n>/`, see
  [`configuration.md`](configuration.md#directories)), sets it to mode `0700`
  and, when running as root, hands it to `system.username`. The compiler and the submitted program run with dropped
  privileges and write their output here, so the work directory must be owned by
  (or writable by) the sandbox user. The judger does this automatically; you only
  need to ensure `judger.workDir` itself lets the judger create subdirectories.
- The judger writes each problem's reference data into `judger.checkpointDir` and
  sets the per-problem directory to mode `0700`. The submission never reads these
  files (it receives its input on stdin, and the judge compares the expected
  output itself), so the sandbox user is denied access and cannot read the
  answers.

Do not place secrets where the sandbox user can read them. The native backend
confines what a submission writes, not what it reads, so a submission running as
the sandbox user can still read world-readable files on the host. Keep
`voj.properties` and the judger jar readable only by the judger account (for
example mode `0640`), and use `judger.sandbox = isolate` if you need to prevent
reads of arbitrary host files entirely.

### Kernel requirements

The native sandbox needs no privilege for its seccomp filter and Landlock rules,
so it works the same on a bare host, as root or not, and in a Docker container
without extra capabilities. It needs:

- **Linux 5.13 or later with Landlock enabled.** Most distributions enable it
  (`landlock` is then listed in `/sys/kernel/security/lsm`); otherwise add it to
  the `lsm=` kernel parameter. Linux 6.12 or later adds the signal and abstract
  Unix socket scoping described above.
- **Linux 6.2 or later (Landlock ABI 3) when the judger does not run as root.**
  Before that, Landlock does not confine `truncate(2)` or `open(O_TRUNC)`, and a
  submission that runs as the judger's own user (see
  [Running without root](#running-without-root-development-and-ci)) could empty
  the judger's files, such as the cached expected output. On Linux 5.13 to 6.1
  a judger that is not root therefore judges every compilation and run a system
  error, with a log message that names the truncation. A judger running as root
  drops privileges to `system.username`, which cannot write the judger's files,
  so 5.13 is enough there.
- **A seccomp profile that allows Landlock**, in a container. Docker's default
  profile allows the `landlock_*` system calls (verified with Docker 29); a
  custom or older profile that denies them, like a kernel without Landlock,
  makes every compilation and run a system error, with a log message that names
  Landlock. The judger never runs a submission without it.

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
- **Limits and verdicts.** A run may have `judger.processLimit` processes and
  threads at once (`--processes`), a compilation `judger.compileProcessLimit`.
  Files are capped at `judger.isolate.fileSizeLimitKb` (`--fsize`): a run whose
  output reaches it is judged Output Limit Exceeded, whether `SIGXFSZ` kills it
  or it ignores the signal, so that its writes fail and it exits on its own. A
  program killed by the out-of-memory killer of its box's cgroup
  (`cg-oom-killed`), or that fails at or above its memory limit, is judged
  Memory Limit Exceeded.
- **Boxes.** Every judger on a host needs a box of its own
  (`judger.isolate.boxId`). The box is cleaned up after every compilation and
  run, which frees the captured output right away; `--init` empties a box that a
  judger which died mid-run left behind.

## Several judgers on one host

Judgers on one host may share `judger.checkpointDir` and `judger.workDir`, each
or both: each judger claims a slot of its own under `judger.checkpointDir`, with
a lock file there, which gives it a cache directory there and a work directory
under `judger.workDir`, named after the slot and `judger.checkpointDir` (see
[`configuration.md`](configuration.md#directories)). Their sandboxes, though,
must be their own:

- **Native sandbox:** give each judger a sandbox account of its own
  (`system.username`). Runs of the same account could signal one another's
  processes, read one another's work directories, and share one `RLIMIT_NPROC`,
  so the runs of a busy judger could leave another's without threads. A judger
  running as root holds a lock named after the account's uid,
  `/run/voj-sandbox-<uid>.lock` (in the temporary directory when `/run` is not
  writable), and refuses to start when another judger on the host already uses
  the account.
- **isolate:** give each judger a box of its own (`judger.isolate.boxId`).

Judgers in separate Docker containers do not see each other's lock, and need
not: their PID and mount namespaces keep their runs apart, so they may all use
`nobody`. Without user namespace remapping, however, an account is the same uid
on the host in every container, and `RLIMIT_NPROC` counts every process and
thread of that uid on the host: the runs of all those judgers share one
`judger.processLimit`. Run a judger's submissions under a uid of its own (a
different `system.username` per container, or remapped user namespaces) if
runs that use many threads may overlap.

## Native build prerequisites (Linux)

Building the judger compiles its JNI library, which needs a C++ toolchain and
libseccomp:

```
sudo apt-get install -y g++ make libseccomp-dev
```

`JAVA_HOME` must point at a JDK (not a JRE): the build includes the JNI headers
from `$JAVA_HOME/include` and `$JAVA_HOME/include/linux`.

## Docker

The published judger image already runs as root and drops to `nobody`, and it
ships with `libseccomp` installed, so native sandboxing works without extra
configuration. See [`../docker/README.md`](../docker/README.md).

## Running without root (development and CI)

When the judger is not running as root it cannot drop privileges, so submissions
run as the same user as the judger. The seccomp filter and the Landlock rules
still apply (which needs Linux 6.2 or later, see
[Kernel requirements](#kernel-requirements)), but the submission can read
whatever the judger's account can. This is fine for development and continuous integration but is not
isolation, so do not use it in production.

## Windows native sandbox

On Windows the native backend is the only option (`isolate` is Linux-only). Each
submission runs under a **separate low-privilege Windows account** inside a
[Job Object](https://learn.microsoft.com/windows/win32/procthread/job-objects)
that applies UI restrictions and kills the whole process tree when the run
ends. While the program runs, the judger polls it every 2 ms and kills the Job
as soon as a limit is exceeded:

- **CPU time**: that of every process of a run (a compilation: the compiler's
  top-level process), plus a wall-clock guard of the time limit + 1 s for a
  program that sleeps or blocks.
- **Memory**: the peak working set (resident memory) of the program's process,
  like the peak resident set size on Linux (a compilation: the compiler's
  top-level process). Memory that is committed but never touched does not
  count: the JVM commits its initial heap (RAM / 64 by default) as it starts.
  The Job also has a hard cap on commit, the counterpart of Linux's
  `RLIMIT_DATA`: twice the limit plus max(256 MB, RAM / 32), for all of a run's
  processes together (a compilation: for each process). An allocation refused
  by the cap is judged Memory Limit Exceeded as well. Unlike on Linux, a program
  can lower its own working set (`SetProcessWorkingSetSize(-1, -1)`,
  `EmptyWorkingSet`), which moves pages to the standby list or the page file
  without freeing them; Windows offers no measure of touched memory it cannot
  lower that does not also count the JVM's untouched heap. Such a program is
  stopped by the cap only, so on Windows the memory limit is enforced up to
  that cap rather than exactly.
- **Output**: 256 MB written to standard output (the Linux runner's
  `RLIMIT_FSIZE`). Other files the program writes are not limited.
- **Processes**: a run of the submission may not create processes (its Job
  holds the program and its console host only); a compilation may have up to
  `judger.compileProcessLimit`.

The final CPU time, memory and output size are checked again after the program
exits, so a program that exceeds a limit between two polls is never judged on
its output. Compilations and runs start in the submission's work directory. The
output file is created anew for every run: whatever the sandbox account may have
left at its path (such as a hard link to another file) is removed first, never
written through.

The submission's environment is the sandbox account's own (system variables,
the account's variables and profile paths such as `TEMP` and `APPDATA`) plus
only these variables of the judger: `PATH`, `LANG`, `LC_ALL`, `TZ`,
`JAVA_HOME`, `KOTLIN_HOME`, `GOROOT`, `GOENV`, `GOTOOLCHAIN`, `GOFLAGS`,
`RUSTUP_HOME`, `RUSTUP_TOOLCHAIN` and `CARGO_HOME`. Nothing else of the
judger's environment (such as `VOJ_DB_PASSWORD` or `VOJ_JUDGER_API_TOKEN`)
reaches it. `GOCACHE` and `GOPATH` point into the submission's work directory,
so every Go build starts from its own empty cache. Toolchains must therefore be
reachable through the judger's `PATH` and readable by the sandbox account.

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
- Keep it a plain member of *Users* (no admin rights). It only needs to enter,
  read and write the submission's work directory under `judger.workDir` (each
  compilation and run starts there; the compiler writes the program and, for Go,
  its build cache there) and execute the compiled program. Make sure the ACL of
  `judger.workDir` lets the work directories the judger creates inherit that
  access for the account.

Because the credentials live in `voj.properties`, keep that file readable only by
the judger account.

### Native build prerequisites (Windows)

The JNI library (`JudgerCore.dll`) is built by `judger/Makefile` with the
64-bit [MinGW-w64](https://www.mingw-w64.org/) `g++` and GNU `make`, as in CI
(`appveyor.yml`): put MinGW-w64's `bin` directory on `PATH` (its make is
`mingw32-make.exe`; CI copies it to `make.exe`, which the Maven build runs),
have a POSIX `sh`, `mkdir` and `rm` on `PATH` for the Makefile's recipes (CI
uses Cygwin's), and set `JAVA_HOME` to a 64-bit JDK. The build includes the JNI headers from
`%JAVA_HOME%\include` and `%JAVA_HOME%\include\win32`; nothing needs to be
copied. The MinGW runtime is linked into the DLL statically, so the judger does
not need MinGW on its `PATH` to load it (only to compile C/C++ submissions with
`gcc`/`g++`). MSVC is not supported by the Makefile.

## Upgrading the web application

The database is created by `sql/schema.sql` and `sql/seed.sql` on the first
launch only; there is no automatic migration on startup. When a release needs a
one-time data change on an existing installation, it ships a script under
[`../sql/upgrades/`](../sql/upgrades/) that you run once against the database.
New installations never need these scripts. Each script is safe to run again:
it records that it has run in `voj_options`, and running it once more changes
nothing (a script that changes the schema checks the current schema before each
step, so running it again also completes a run that was interrupted).

Back up the database first, then run the script while the old version is
stopped (or just before you switch to the new one), using the same credentials
as the web application:

```bash
mysql -u voj -p voj < sql/upgrades/<script>.sql
# Bundled Docker database (the old container is still running):
docker compose exec -T web mysql voj < sql/upgrades/<script>.sql
```

The scripts expect the schema of 1.0.0 or later: 1.0.0 redesigned the schema,
and there is no upgrade path from 0.9.x.

| Upgrading from | Script | What it does |
| --- | --- | --- |
| 1.0.0 to 1.0.2 | `decode-legacy-html-entities.sql` | Up to 1.0.2, discussion titles and replies, display names, locations, websites, social links and bios were stored HTML-escaped, so after the upgrade old rows show `&amp;`, `&lt;`, `&gt;` and `&nbsp;` literally. The script decodes them once. Run it before users post on the new version: its first run decodes every such row, so text typed as `&amp;` on the new version before that would be decoded too. |
| 1.0.0 to 1.0.2 | `upgrade-from-1.0.2.sql` | Required: the new version reads and writes the new schema. It makes the foreign keys of `voj_contest_contestants` and `voj_contest_submissions` cascade, like every other foreign key to users, problems and submissions (without it, deleting a user who attended a contest, a contest submission, or a problem that has contest submissions fails). It replaces the two columns that described a contest's ranking (`contest_mode` and `contest_scoring`) by `contest_scoring` alone, set to the rule each contest was actually ranked by (Codeforces contests and contests created before the scoring setting existed get `ICPC`, or `IOI` if they were ranked OI-style), and drops the unused `contest_is_rated` and `voj_contest_contestants.code_snippet`. It adds `voj_submissions.submission_judge_generation`, which tells the judging task of a rejudge from the older tasks of the submission, and `voj_submissions.submission_queued_time`, the time the current judging task of a submission was queued (the submit time for existing submissions), by which the web application marks a submission that waited too long for a judger as a System Error; it drops the foreign key from `voj_email_validation` to `voj_users`, so that the daily limit of verification and password-reset emails stays with an address when an account moves to another one. It points Kotlin's default compile command at a temporary directory inside the work directory (`-J-Djava.io.tmpdir=.`), the only place the native sandbox lets a compiler write; a changed command is left alone. Finally, problem descriptions, input and output formats, samples and contest notes were written into the pages as HTML up to 1.0.2 and are now written as text, so entities such as `&lt;`, `&gt;` and `&amp;` in them would show literally (in samples, code and math); the script decodes `&lt;`, `&gt;`, `&quot;`, `&#39;`, `&nbsp;` and `&amp;` once. Other entities (e.g. `&le;` or `&#60;`) are left as they are, and so are HTML tags such as `<br>` or `<sup>`, which the pages now show literally as well: look through the statements that used them and rewrite them in Markdown (or as the characters themselves). Run it before administrators edit statements on the new version: its first run decodes every such field, so an entity typed on the new version before that would be decoded too. |
