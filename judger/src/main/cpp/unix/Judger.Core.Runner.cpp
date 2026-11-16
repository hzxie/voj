/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2014-2026 Haozhe Xie <root@haozhexie.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#pragma GCC diagnostic ignored "-Wwrite-strings"

#include "../org_verwandlung_voj_jni_hashmap.h"
#include "../org_verwandlung_voj_jni_library.h"
#include "../org_verwandlung_voj_judger_core_Runner.h"

#include <atomic>
#include <cerrno>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <iterator>
#include <sstream>
#include <string>
#include <system_error>
#include <thread>
#include <vector>

#include <fcntl.h>
#include <grp.h>
#include <pwd.h>
#include <sched.h>
#include <signal.h>
#include <sys/mman.h>
#include <sys/prctl.h>
#include <sys/resource.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

#include <linux/filter.h>
#include <seccomp.h>

// SECCOMP_SET_MODE_FILTER lives in <linux/seccomp.h>, but that header can clash with libseccomp's
// <seccomp.h> on some distributions, so define it here instead of including the kernel header.
#ifndef SECCOMP_SET_MODE_FILTER
#define SECCOMP_SET_MODE_FILTER 1
#endif

// close_range(2) (Linux 5.9+, CLOSE_RANGE_CLOEXEC since 5.11) has the same number on every
// architecture; older C libraries may not define it.
#ifndef SYS_close_range
#define SYS_close_range 436
#endif
#ifndef CLOSE_RANGE_CLOEXEC
#define CLOSE_RANGE_CLOEXEC (1U << 2)
#endif

/**
 * Tunables for the sandbox.
 */
static const mode_t    OUTPUT_FILE_MODE       = 0644;
static const int       POLL_INTERVAL_US       = 1000;        // watchdog poll (1 ms)
static const int       WALL_TIME_FACTOR       = 3;           // wall-clock backstop multiplier
static const long long WALL_TIME_SLACK_MS     = 1000;        // wall-clock backstop slack
static const rlim_t    OUTPUT_FILE_SIZE_LIMIT = (rlim_t)256 * 1024 * 1024; // RLIMIT_FSIZE (256 MB)
static const rlim_t    PROCESS_NUMBER_LIMIT   = 64;          // default RLIMIT_NPROC (threads)
static const int       CLEANUP_ATTEMPTS       = 1000;        // wait rounds (x 1 ms) after a run
// RLIMIT_DATA backstop: 2 x the memory limit plus a headroom of max(256 MB, physical RAM / 32). The
// headroom matters for the JVM, whose committed initial heap defaults to physical RAM / 64.
static const unsigned long long DATA_LIMIT_MIN_HEADROOM_KB = 256ULL * 1024;
static const unsigned long long DATA_LIMIT_RAM_DIVISOR     = 32;

/**
 * The only environment variables the sandboxed process inherits from the judger. The judger's own
 * environment holds secrets (VOJ_DB_PASSWORD, VOJ_JUDGER_API_TOKEN, ...) that a submission could
 * otherwise read, e.g. with `#include "/proc/self/environ"` echoed into the compile log. Only
 * locale and toolchain settings that compilers and runtimes genuinely need are passed through.
 */
static const char* const INHERITED_ENVIRONMENT_VARIABLES[] = {
    "PATH", "LANG", "LC_ALL", "TZ",
    "JAVA_HOME", "KOTLIN_HOME",
    "GOROOT", "GOPATH", "GOCACHE", "GOENV", "GOTOOLCHAIN", "GOFLAGS",
    "RUSTUP_HOME", "RUSTUP_TOOLCHAIN", "CARGO_HOME",
};
/**
 * The Go toolchain's writable state, which must never be shared between submissions: a cache that
 * one submission (compiled or run as the sandbox user) can write is one it can poison for every
 * later Go build. When privileges are dropped these are not inherited but placed in the work
 * directory, which is fresh and private for every submission (the judger may seed the build cache
 * there from a read-only copy, see judger.goBuildCacheSeed).
 */
static const char* const GO_BUILD_CACHE_DIRECTORY = "/.cache/go-build"; // GOCACHE (work dir relative)
static const char* const GO_PATH_DIRECTORY        = "/go";              // GOPATH (work dir relative)
static const char* const DEFAULT_PATH = "/usr/local/bin:/usr/bin:/bin";
static const char* const DEFAULT_LANG = "C.UTF-8";

/**
 * Everything the (async-signal-safe) child needs is resolved in the parent before fork, so that
 * the child never has to call malloc, NSS or the C++ runtime while the rest of the JVM's threads
 * are frozen by fork.
 */
struct ChildPlan {
  std::string              executable;     // absolute path or PATH-resolved program to exec
  std::vector<char*>       argv;           // NULL-terminated argument vector
  std::vector<std::string> environment;    // sanitized "NAME=value" environment
  std::vector<char*>       envp;           // NULL-terminated pointers into environment
  std::string              inputFilePath;  // stdin redirect (empty = /dev/null)
  std::string              outputFilePath; // stdout redirect (empty = /dev/null)
  bool                     compileStep;    // compile: stderr also goes to outputFilePath
  int                      timeLimit;      // ms (0 = no limit)
  rlim_t                   dataLimit;      // RLIMIT_DATA in bytes (0 = none)
  rlim_t                   processLimit;   // RLIMIT_NPROC of the sandbox user (processes + threads)
  bool                     dropPrivileges; // whether to setuid/setgid
  uid_t                    dropUid;
  gid_t                    dropGid;
  std::string              seccompProgram; // exported classic BPF bytes (empty = no filter)
};

/**
 * The steps that prepare the sandbox before exec. A step that fails is reported to the judger, with
 * its errno, through the sync pipe, and the judger refuses the run with an exception (a system
 * error), so that a broken sandbox is never mistaken for a fault of the submission.
 */
enum ChildStep {
  STEP_DESCRIPTORS, STEP_INPUT, STEP_PROCESS_GROUP, STEP_RESOURCE_LIMIT, STEP_GROUPS, STEP_GID,
  STEP_UID, STEP_OUTPUT, STEP_DEV_NULL, STEP_NO_NEW_PRIVS, STEP_SECCOMP, STEP_EXEC,
};
static const char* const CHILD_STEP_NAMES[] = {
  "marking inherited descriptors close-on-exec", "opening the input file", "setpgid()",
  "setrlimit()", "setgroups()", "setgid()", "setuid()", "opening the output file",
  "opening /dev/null", "prctl(PR_SET_NO_NEW_PRIVS)", "installing the seccomp filter", "execve()",
};
struct ChildFailure {
  int step;
  int error;
};

/**
 * Function Prototypes.
 */
static std::vector<std::string> splitCommandLine(const std::string&);
static std::string resolveExecutable(const std::string&);
static std::vector<std::string> buildEnvironment(bool dropPrivileges, const std::string& outputFilePath);
static std::string buildSeccompProgram(bool runOfSubmission);
static long long getMonotonicMillis();
static long long readCpuTimeMillis(pid_t);
static int  readPeakMemoryKb(pid_t);
static rlim_t getDataLimit(int memoryLimitKb);
static void awaitProcessGroupExit(pid_t);
static void runProgram(const ChildPlan&, int syncWriteFd) __attribute__((noreturn));
static void reportChildFailure(int syncWriteFd, int step) __attribute__((noreturn));
static bool setResourceLimit(int resource, rlim_t soft, rlim_t hard);
static int  openOutputFile(const char*);
static bool markInheritedDescriptorsCloseOnExec();

/**
 * JNI call entry point.
 * Runs the given command in a sandboxed child process and measures its CPU time and peak memory
 * usage. Limits are enforced both by the kernel (setrlimit) and by a fine-grained watchdog;
 * isolation is layered: the seccomp syscall filter and (when running as root) the privilege drop
 * are mandatory - the run is refused if either cannot be applied - while the network namespace is
 * best-effort, as the filter already refuses every socket but AF_UNIX.
 *
 * A run of the submission is a single process: the filter lets it create threads, but no other
 * process (see buildSeccompProgram). A compilation runs a trusted compiler, which may run others;
 * they all stay in the compiler's process group, which is killed as a whole.
 *
 * @param  jniEnv          - a reference to the JNI runtime environment
 * @param  selfReference   - a reference to the calling Java object
 * @param  jCommandLine    - the command line to execute
 * @param  jUsername       - the unprivileged user to drop to (when running as root)
 * @param  jPassword       - unused on Unix (privilege drop is resolved from the username)
 * @param  jInputFilePath  - the file to read stdin from (may be NULL: /dev/null)
 * @param  jOutputFilePath - the file receiving stdout (may be NULL: /dev/null); for a compilation it
 *                           also receives stderr, otherwise stderr is discarded
 * @param  timeLimit       - the program execution time limit (ms, 0 means no limit)
 * @param  memoryLimit     - the program execution memory limit (KB, 0 means no limit)
 * @param  isCompilation   - whether this is a compilation (as opposed to a run of the submission)
 * @param  processLimit    - the maximum number of processes and threads of the sandbox user
 *                           (RLIMIT_NPROC, applied when privileges are dropped; 0 means the default)
 * @return a Map<String, Object> object containing the runtime result; throws a Java exception
 *         instead if the sandbox could not be set up
 */
JNIEXPORT jobject JNICALL Java_org_verwandlung_voj_judger_core_Runner_getRuntimeResult(
    JNIEnv* jniEnv, jobject selfReference, jstring jCommandLine, jstring jUsername,
    jstring jPassword, jstring jInputFilePath, jstring jOutputFilePath, jint timeLimit,
    jint memoryLimit, jboolean isCompilation, jint processLimit) {
    (void) selfReference;
    (void) jPassword;
    std::string commandLine = getStringValue(jniEnv, jCommandLine);
    std::string username    = getStringValue(jniEnv, jUsername);

    // ----- Resolve everything the child needs before fork -----
    std::vector<std::string> args = splitCommandLine(commandLine);
    if ( args.empty() ) {
        throwStringException(jniEnv, "Judger: the command line is empty");
        return NULL;
    }

    ChildPlan plan;
    plan.executable     = resolveExecutable(args[0]);
    plan.inputFilePath  = getStringValue(jniEnv, jInputFilePath);
    plan.outputFilePath = getStringValue(jniEnv, jOutputFilePath);
    // A compiler's diagnostics (stderr) are the compile log, but a submission's stderr must never
    // reach the judged output.
    plan.compileStep    = (isCompilation == JNI_TRUE);
    plan.timeLimit      = timeLimit;
    plan.dataLimit      = getDataLimit(memoryLimit);
    plan.processLimit   = (processLimit > 0) ? (rlim_t) processLimit : PROCESS_NUMBER_LIMIT;
    plan.dropPrivileges = false;
    plan.dropUid        = 0;
    plan.dropGid        = 0;
    for ( std::string& arg : args ) {
        plan.argv.push_back(const_cast<char*>(arg.c_str()));
    }
    plan.argv.push_back(nullptr);

    // When running as root, dropping privileges is mandatory and fails closed.
    if ( geteuid() == 0 ) {
        struct passwd* pw = username.empty() ? nullptr : getpwnam(username.c_str());
        if ( pw == nullptr || pw->pw_uid == 0 ) {
            // Never run untrusted code as root.
            throwStringException(jniEnv, "Judger: the sandbox user \"" + username +
                                         (pw == nullptr ? "\" does not exist" : "\" is root") +
                                         "; refusing to run");
            return NULL;
        }
        plan.dropPrivileges = true;
        plan.dropUid        = pw->pw_uid;
        plan.dropGid        = pw->pw_gid;
    }

    plan.environment = buildEnvironment(plan.dropPrivileges, plan.outputFilePath);
    for ( std::string& variable : plan.environment ) {
        plan.envp.push_back(const_cast<char*>(variable.c_str()));
    }
    plan.envp.push_back(nullptr);

    // The filter is what keeps a submission off the network when the judger has no CAP_SYS_ADMIN
    // (a default Docker container), and away from ptrace, namespaces, setsid and other processes
    // everywhere: a run without it is refused rather than started unconfined.
    plan.seccompProgram = buildSeccompProgram(!plan.compileStep);
    if ( plan.seccompProgram.empty() ) {
        throwStringException(jniEnv, "Judger: failed to build the seccomp filter; refusing to run");
        return NULL;
    }

    // A close-on-exec pipe: it stays open until execve succeeds (then it auto-closes, the parent's
    // read sees EOF) or the child reports a failed step on it. The parent blocks on it so the
    // watchdog only starts once the real program is running, avoiding measuring the JVM image that
    // the freshly forked child still shares before exec.
    int syncPipe[2];
    if ( pipe2(syncPipe, O_CLOEXEC) != 0 ) {
        throwStringException(jniEnv, std::string("Judger: pipe2() failed: ") + strerror(errno));
        return NULL;
    }
    pid_t pid = fork();
    if ( pid < 0 ) {
        int error = errno;
        close(syncPipe[0]);
        close(syncPipe[1]);
        throwStringException(jniEnv, std::string("Judger: fork() failed: ") + strerror(error));
        return NULL;
    }
    if ( pid == 0 ) {
        close(syncPipe[0]);
        runProgram(plan, syncPipe[1]); // never returns
    }

    // ----- Parent process -----
    close(syncPipe[1]);
    ChildFailure childFailure;
    childFailure.step  = -1;
    childFailure.error = 0;
    ssize_t syncBytes  = 0;
    while ( (syncBytes = read(syncPipe[0], &childFailure, sizeof(childFailure))) < 0 && errno == EINTR ) {
        // Retry on interrupted read.
    }
    close(syncPipe[0]);
    if ( syncBytes != 0 ) {
        // The sandbox could not be set up, so the program never ran: a system error, never a
        // verdict on the submission.
        while ( waitpid(pid, nullptr, 0) < 0 && errno == EINTR ) {
            // Retry on interrupted system call.
        }
        std::string message = "Judger: the sandbox failed to start the program";
        if ( syncBytes == (ssize_t) sizeof(childFailure) && childFailure.step >= 0 &&
             childFailure.step < (int) (sizeof(CHILD_STEP_NAMES) / sizeof(CHILD_STEP_NAMES[0])) ) {
            message = std::string("Judger: ") + CHILD_STEP_NAMES[childFailure.step] +
                      " failed in the sandbox: " + strerror(childFailure.error);
        }
        throwStringException(jniEnv, message + " (" + commandLine + ")");
        return NULL;
    }

    // The program has been exec'd and is in its own process group (pid), set up before exec.
    std::atomic<bool> finished(false);
    std::atomic<bool> timedOut(false);
    std::atomic<bool> memoryExceeded(false);
    std::atomic<int>  peakMemoryKb(0);
    long long startMillis = getMonotonicMillis();
    std::thread watchdog;
    try {
        watchdog = std::thread([&]() {
            long long wallBackstopMs =
                (timeLimit > 0) ? (long long) timeLimit * WALL_TIME_FACTOR + WALL_TIME_SLACK_MS : 0;
            while ( !finished.load() ) {
                if ( timeLimit > 0 ) {
                    long long cpuMs  = readCpuTimeMillis(pid);
                    long long wallMs = getMonotonicMillis() - startMillis;
                    if ( cpuMs > timeLimit || (wallBackstopMs > 0 && wallMs > wallBackstopMs) ) {
                        timedOut.store(true);
                        kill(-pid, SIGKILL);
                    }
                }
                // The peak RSS (VmHWM), which also catches a spike between two polls. The watchdog
                // only starts once the program has been exec'd, so it never sees the JVM image the
                // child shared before exec. For a compilation, this is the compiler's top-level
                // process only; the others are each held to RLIMIT_DATA.
                int peakKb = readPeakMemoryKb(pid);
                if ( peakKb > peakMemoryKb.load() ) {
                    peakMemoryKb.store(peakKb);
                }
                if ( memoryLimit > 0 && peakKb > memoryLimit ) {
                    memoryExceeded.store(true);
                    kill(-pid, SIGKILL);
                }
                usleep(POLL_INTERVAL_US);
            }
        });
    } catch ( const std::system_error& ex ) {
        kill(-pid, SIGKILL);
        while ( waitpid(pid, nullptr, 0) < 0 && errno == EINTR ) {
            // Retry on interrupted system call.
        }
        throwStringException(jniEnv, std::string("Judger: failed to start the watchdog: ") + ex.what());
        return NULL;
    }

    // Wait for the program to exit, but leave it unreaped (a zombie) for now: as long as it is, its
    // process group cannot be reused by another process.
    siginfo_t exitInfo;
    while ( waitid(P_PID, (id_t) pid, &exitInfo, WEXITED | WNOWAIT) != 0 && errno == EINTR ) {
        // Retry on interrupted system call.
    }
    finished.store(true);
    watchdog.join();

    // Kill whatever is left of the process group (a compiler's children: a run of the submission
    // has no other process), in one go, before the caller looks at the output. setsid and setpgid
    // are denied by the seccomp filter, so nothing can have left the group.
    kill(-pid, SIGKILL);
    int           status = 0;
    struct rusage usage;
    memset(&usage, 0, sizeof(usage));
    pid_t reaped = -1;
    while ( (reaped = wait4(pid, &status, 0, &usage)) < 0 && errno == EINTR ) {
        // Retry on interrupted system call.
    }
    int waitError = errno;
    awaitProcessGroupExit(pid);
    if ( reaped != pid ) {
        throwStringException(jniEnv, std::string("Judger: failed to wait for the program: ") +
                                     strerror(waitError) + " (" + commandLine + ")");
        return NULL;
    }

    // Kernel-accounted CPU time (user + system) of the program and of the children it waited for
    // (a compiler's; a run of the submission has none), every thread included.
    long long cpuMicros = (long long) usage.ru_utime.tv_sec * 1000000 + usage.ru_utime.tv_usec +
                          (long long) usage.ru_stime.tv_sec * 1000000 + usage.ru_stime.tv_usec;
    int usedTime = (int) (cpuMicros / 1000);
    // Peak memory sampled from /proc after exec. rusage's ru_maxrss is deliberately NOT used: for a
    // child forked from the multi-threaded JVM it can include the parent's resident pages, briefly
    // shared through the fork/exec window, hugely over-reporting memory on some architectures (e.g.
    // a trivial program measured as the whole JVM heap).
    int usedMemory = peakMemoryKb.load();
    int exitCode   = 1;
    if ( WIFEXITED(status) ) {
        exitCode = WEXITSTATUS(status);
    } else if ( WIFSIGNALED(status) ) {
        exitCode = 128 + WTERMSIG(status);
    }

    // CPU time the watchdog could not see while the run lasted (it polls) still counts: a run
    // whose total exceeds the limit is a time-out.
    if ( timeLimit > 0 && usedTime > timeLimit ) {
        timedOut.store(true);
    }

    // Keep the verdict mapping on the Java side (exitCode / time / memory) consistent.
    if ( timedOut.load() && usedTime < timeLimit ) {
        usedTime = timeLimit;
    }
    if ( memoryExceeded.load() && memoryLimit > 0 && usedMemory < memoryLimit ) {
        usedMemory = memoryLimit;
    }
    if ( (timedOut.load() || memoryExceeded.load()) && exitCode == 0 ) {
        exitCode = 1; // a killed program must never be reported as a clean exit
    }

    JHashMap result;
    result.put("usedTime", usedTime);
    result.put("usedMemory", usedMemory);
    result.put("exitCode", exitCode);
    return result.toJObject(jniEnv);
}

/**
 * The body of the sandboxed program's process before exec. Runs only async-signal-safe operations
 * (no malloc, NSS or C++ runtime), because it is a fork of the multi-threaded JVM, whose locks
 * fork leaves frozen. Everything else was resolved into the ChildPlan before fork. On any failure
 * it reports the failed step through the sync pipe and exits; on success execve replaces the image
 * and closes the close-on-exec pipe, which the judger observes as EOF.
 *
 * @param  plan        - the pre-resolved execution plan
 * @param  syncWriteFd - the write end of the close-on-exec sync pipe
 */
static void runProgram(const ChildPlan& plan, int syncWriteFd) {
    // Drop the JVM's signal handlers and mask: the program would otherwise inherit the mask (and
    // any ignored signal) across exec.
    struct sigaction defaultAction;
    memset(&defaultAction, 0, sizeof(defaultAction));
    defaultAction.sa_handler = SIG_DFL;
    sigemptyset(&defaultAction.sa_mask);
    for ( int signalNumber = 1; signalNumber < NSIG; ++signalNumber ) {
        sigaction(signalNumber, &defaultAction, nullptr); // fails harmlessly for SIGKILL/SIGSTOP
    }
    sigset_t emptySet;
    sigemptyset(&emptySet);
    sigprocmask(SIG_SETMASK, &emptySet, nullptr);

    // Nothing the JVM has open (database and message-broker sockets, jars, logs, ...) may leak into
    // the sandbox, where it would bypass the network namespace and file permissions.
    if ( !markInheritedDescriptorsCloseOnExec() ) {
        reportChildFailure(syncWriteFd, STEP_DESCRIPTORS);
    }

    // Standard input is opened before the privilege drop: the checkpoint directory is private to the
    // judger, and the path is chosen by the judger, not by the submission. Without an input file
    // (a compilation) it is /dev/null: the judger's own standard input must never be readable, e.g.
    // by a submission that does #include "/dev/stdin".
    int inputFd = plan.inputFilePath.empty() ? open("/dev/null", O_RDONLY | O_CLOEXEC)
                                             : open(plan.inputFilePath.c_str(), O_RDONLY | O_CLOEXEC);
    if ( inputFd < 0 ) {
        reportChildFailure(syncWriteFd, plan.inputFilePath.empty() ? STEP_DEV_NULL : STEP_INPUT);
    }
    dup2(inputFd, STDIN_FILENO);
    close(inputFd);

    // Run in a dedicated process group so the watchdog can signal the whole tree.
    if ( setpgid(0, 0) != 0 ) {
        reportChildFailure(syncWriteFd, STEP_PROCESS_GROUP);
    }

    // Tier 1: kernel-enforced resource limits.
    if ( plan.timeLimit > 0 ) {
        rlim_t seconds = (rlim_t) ((plan.timeLimit + 999) / 1000) + 1;
        if ( !setResourceLimit(RLIMIT_CPU, seconds, seconds + 1) ) {
            reportChildFailure(syncWriteFd, STEP_RESOURCE_LIMIT);
        }
    }
    if ( !setResourceLimit(RLIMIT_FSIZE, OUTPUT_FILE_SIZE_LIMIT, OUTPUT_FILE_SIZE_LIMIT) ) {
        reportChildFailure(syncWriteFd, STEP_RESOURCE_LIMIT);
    }

    // Tier 2 (best-effort): cut off the network. This needs CAP_SYS_ADMIN, which a default Docker
    // container lacks; the seccomp filter, which refuses every socket but AF_UNIX, does not.
    unshare(CLONE_NEWNET);

    // Drop privileges to the unprivileged sandbox user (only when running as root). The process
    // (and thread) count cap is applied only here, because RLIMIT_NPROC is per real-UID: enforcing it without a
    // dedicated user would count, and throttle, every process owned by the shared account. It
    // counts threads too, so a compilation gets a higher cap than a run of the submission: the Go
    // toolchain runs one compiler per CPU, each with a thread per CPU.
    if ( plan.dropPrivileges ) {
        if ( !setResourceLimit(RLIMIT_NPROC, plan.processLimit, plan.processLimit) ) {
            reportChildFailure(syncWriteFd, STEP_RESOURCE_LIMIT);
        }
        // Every step fails closed: a process that kept root's supplementary groups (e.g. disk,
        // docker) could reach whatever they grant.
        if ( setgroups(0, nullptr) != 0 ) {
            reportChildFailure(syncWriteFd, STEP_GROUPS);
        }
        if ( setgid(plan.dropGid) != 0 ) {
            reportChildFailure(syncWriteFd, STEP_GID);
        }
        if ( setuid(plan.dropUid) != 0 ) {
            reportChildFailure(syncWriteFd, STEP_UID);
        }
    }

    // The output file is opened only now, with the sandbox user's credentials: the work directory is
    // writable by the submission, which may have planted a symlink (or a hard link, FIFO, ...) at
    // this path during an earlier checkpoint, and root must never follow it. Whatever is not
    // captured goes to /dev/null, never to the judger's own stdout or stderr.
    int nullFd = open("/dev/null", O_WRONLY | O_CLOEXEC);
    if ( nullFd < 0 ) {
        reportChildFailure(syncWriteFd, STEP_DEV_NULL);
    }
    int outputFd = nullFd;
    if ( !plan.outputFilePath.empty() ) {
        outputFd = openOutputFile(plan.outputFilePath.c_str());
        if ( outputFd < 0 ) {
            reportChildFailure(syncWriteFd, STEP_OUTPUT);
        }
    }
    dup2(outputFd, STDOUT_FILENO);
    dup2(plan.compileStep ? outputFd : nullFd, STDERR_FILENO);
    if ( outputFd != nullFd ) {
        close(outputFd);
    }
    close(nullFd);

    // Tier 1: a per-process kernel memory backstop, inherited by every child. RLIMIT_DATA (private
    // writable mappings, i.e. roughly committed memory) is used rather than RLIMIT_AS, because the
    // JVM, V8 (Node.js) and the Go runtime reserve far more address space than they ever commit and
    // do not start under a tight RLIMIT_AS. It is generous on purpose (see getDataLimit), so the
    // watchdog normally reports the overrun as MLE first; the backstop only bounds how far a single
    // process can balloon between two polls. Set last: the forked JVM image itself is far larger.
    if ( plan.dataLimit > 0 && !setResourceLimit(RLIMIT_DATA, plan.dataLimit, plan.dataLimit) ) {
        reportChildFailure(syncWriteFd, STEP_RESOURCE_LIMIT);
    }

    // Install the pre-built seccomp syscall filter via the raw syscall, so the child does not have
    // to call into libseccomp (which would allocate). Mandatory: if it cannot be installed the
    // program is not run.
    struct sock_fprog prog;
    prog.len    = (unsigned short) (plan.seccompProgram.size() / sizeof(struct sock_filter));
    prog.filter = (struct sock_filter*) plan.seccompProgram.data();
    if ( prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) != 0 ) {
        reportChildFailure(syncWriteFd, STEP_NO_NEW_PRIVS);
    }
    if ( syscall(SYS_seccomp, SECCOMP_SET_MODE_FILTER, 0, &prog) != 0 ) {
        reportChildFailure(syncWriteFd, STEP_SECCOMP);
    }

    execve(plan.executable.c_str(), plan.argv.data(), plan.envp.data());

    // execve only returns on failure.
    reportChildFailure(syncWriteFd, STEP_EXEC);
}

/**
 * Reports a failed setup step of the forked child, with the current errno, to the parent through
 * the sync pipe and exits. The parent then refuses the run (see ChildStep). Async-signal-safe.
 *
 * @param  syncWriteFd - the write end of the close-on-exec sync pipe
 * @param  step        - the ChildStep that failed
 */
static void reportChildFailure(int syncWriteFd, int step) {
    ChildFailure failure;
    failure.step  = step;
    failure.error = errno;
    ssize_t ignored = write(syncWriteFd, &failure, sizeof(failure));
    (void) ignored;
    _exit(127);
}

/**
 * Sets a resource limit. A hard limit the judger already has below the one asked for is kept (the
 * limit is then stricter than asked for). Async-signal-safe.
 *
 * @param  resource - the RLIMIT_* resource
 * @param  soft     - the soft limit
 * @param  hard     - the hard limit
 * @return whether the limit is in place
 */
static bool setResourceLimit(int resource, rlim_t soft, rlim_t hard) {
    struct rlimit limit;
    if ( getrlimit(resource, &limit) == 0 && limit.rlim_max != RLIM_INFINITY && limit.rlim_max < hard ) {
        hard = limit.rlim_max;
    }
    limit.rlim_cur = soft < hard ? soft : hard;
    limit.rlim_max = hard;
    return setrlimit(resource, &limit) == 0;
}

/**
 * Marks every file descriptor above standard error close-on-exec, so the sandboxed program only
 * inherits stdin, stdout and stderr; they are marked rather than closed because the sync pipe must
 * stay open until exec. Only the open descriptors are touched: the JVM raises its RLIMIT_NOFILE to
 * the hard limit (often 2^20), and a system call for every possible descriptor would cost the
 * program up to a second of CPU time before it even starts. Async-signal-safe.
 * @return whether every inherited descriptor is marked
 */
static bool markInheritedDescriptorsCloseOnExec() {
    if ( syscall(SYS_close_range, 3U, ~0U, CLOSE_RANGE_CLOEXEC) == 0 ) {
        return true;
    }
    // Linux < 5.11: list the open descriptors in /proc/self/fd, with the raw getdents64 system call
    // into a buffer on the stack (opendir would allocate).
    int directory = open("/proc/self/fd", O_RDONLY | O_DIRECTORY | O_CLOEXEC);
    if ( directory < 0 ) {
        return false;
    }
    struct LinuxDirent64 {
        uint64_t       d_ino;
        int64_t        d_off;
        unsigned short d_reclen;
        unsigned char  d_type;
        char           d_name[1];
    };
    alignas(LinuxDirent64) char buffer[4096];
    for ( ;; ) {
        long length = syscall(SYS_getdents64, directory, buffer, sizeof(buffer));
        if ( length <= 0 ) {
            close(directory);
            return length == 0;
        }
        for ( long offset = 0; offset < length; ) {
            const LinuxDirent64* entry = reinterpret_cast<const LinuxDirent64*>(buffer + offset);
            offset += entry->d_reclen;
            int  fd      = 0;
            bool numeric = entry->d_name[0] != '\0';
            for ( const char* c = entry->d_name; *c != '\0' && numeric; ++c ) {
                numeric = (*c >= '0' && *c <= '9');
                fd      = fd * 10 + (*c - '0');
            }
            if ( numeric && fd > STDERR_FILENO && fd != directory ) {
                fcntl(fd, F_SETFD, FD_CLOEXEC);
            }
        }
    }
}

/**
 * Opens (creating or truncating) the file that receives the program's output. Must be called after
 * the privilege drop, so that whatever the path resolves to is only writable if the sandbox user
 * could write it anyway. As defence in depth it also refuses to follow a symlink in the last path
 * component, never blocks on a FIFO and only accepts a regular file with a single link.
 * Async-signal-safe.
 *
 * @param  path - the output file path
 * @return the open file descriptor, or -1 (with errno set) on failure
 */
static int openOutputFile(const char* path) {
    int fd = open(path, O_CREAT | O_WRONLY | O_TRUNC | O_NOFOLLOW | O_NONBLOCK | O_CLOEXEC,
                  OUTPUT_FILE_MODE);
    if ( fd < 0 ) {
        return -1;
    }
    struct stat st;
    if ( fstat(fd, &st) != 0 || !S_ISREG(st.st_mode) || st.st_nlink != 1 ) {
        close(fd);
        errno = EPERM;
        return -1;
    }
    int flags = fcntl(fd, F_GETFL);
    if ( flags < 0 || fcntl(fd, F_SETFL, flags & ~O_NONBLOCK) != 0 ) {
        close(fd);
        return -1;
    }
    return fd;
}

/**
 * Splits a command line into a list of whitespace-separated arguments.
 * @param  commandLine - the command line
 * @return the list of arguments
 */
static std::vector<std::string> splitCommandLine(const std::string& commandLine) {
    std::istringstream iss(commandLine);
    return std::vector<std::string>{
        std::istream_iterator<std::string>{iss},
        std::istream_iterator<std::string>{}
    };
}

/**
 * Resolves a program name to an executable path. Names containing a slash are used verbatim;
 * otherwise PATH is searched, so the child can call execve (which does not search PATH and so does
 * not allocate) instead of execvp.
 * @param  file - the program name from the command line
 * @return the resolved path (or the original name if nothing matched)
 */
static std::string resolveExecutable(const std::string& file) {
    if ( file.find('/') != std::string::npos ) {
        return file;
    }
    const char* path = getenv("PATH");
    if ( path == nullptr ) {
        path = DEFAULT_PATH;
    }
    std::string pathValue(path);
    size_t start = 0;
    while ( start <= pathValue.size() ) {
        size_t colon = pathValue.find(':', start);
        std::string dir =
            (colon == std::string::npos) ? pathValue.substr(start) : pathValue.substr(start, colon - start);
        if ( !dir.empty() ) {
            std::string candidate = dir + "/" + file;
            if ( access(candidate.c_str(), X_OK) == 0 ) {
                return candidate;
            }
        }
        if ( colon == std::string::npos ) {
            break;
        }
        start = colon + 1;
    }
    return file;
}

/**
 * Builds the minimal environment of the sandboxed process: the whitelisted variables of the judger
 * (INHERITED_ENVIRONMENT_VARIABLES), defaults for PATH and LANG, and HOME. When privileges are
 * dropped, the judger's HOME (e.g. /root) is useless to the sandbox user, so HOME is set to the
 * work directory (the directory holding the output file), which the sandbox user owns, and so are
 * the Go build cache (GOCACHE) and GOPATH, which would otherwise be shared between submissions.
 * @param  dropPrivileges - whether the child drops to the sandbox user
 * @param  outputFilePath - the output file path (its directory is the work directory)
 * @return the list of "NAME=value" strings
 */
static std::vector<std::string> buildEnvironment(bool dropPrivileges, const std::string& outputFilePath) {
    size_t      lastSlash     = outputFilePath.rfind('/');
    std::string workDirectory = (lastSlash != std::string::npos && lastSlash > 0)
                                    ? outputFilePath.substr(0, lastSlash)
                                    : std::string("/tmp");

    std::vector<std::string> environment;
    for ( const char* name : INHERITED_ENVIRONMENT_VARIABLES ) {
        if ( dropPrivileges && (strcmp(name, "GOCACHE") == 0 || strcmp(name, "GOPATH") == 0) ) {
            continue;
        }
        const char* value = getenv(name);
        if ( value != nullptr ) {
            environment.push_back(std::string(name) + "=" + value);
        }
    }
    if ( dropPrivileges ) {
        environment.push_back("GOCACHE=" + workDirectory + GO_BUILD_CACHE_DIRECTORY);
        environment.push_back("GOPATH=" + workDirectory + GO_PATH_DIRECTORY);
    }
    if ( getenv("PATH") == nullptr ) {
        environment.push_back(std::string("PATH=") + DEFAULT_PATH);
    }
    if ( getenv("LANG") == nullptr ) {
        environment.push_back(std::string("LANG=") + DEFAULT_LANG);
    }

    const char* home = getenv("HOME");
    if ( dropPrivileges || home == nullptr ) {
        environment.push_back("HOME=" + workDirectory);
    } else {
        environment.push_back(std::string("HOME=") + home);
    }
    return environment;
}

/**
 * Builds the seccomp syscall filter and exports it as a classic BPF program. Uses a default-allow
 * policy and denies a curated set of syscalls that a competitive-programming submission never
 * legitimately needs but that are useful for sandbox escape, host tampering or persistence. Rules
 * are added by syscall name through libseccomp, so the filter is portable across architectures
 * (amd64/arm64). The program is exported here, in the parent, so the child can install it without
 * calling into libseccomp.
 *
 * Creating namespaces is denied as well. An unprivileged user namespace hands the submission a full
 * set of capabilities inside it, which turns every namespaced kernel interface (mount, netfilter,
 * ...) into attack surface and lets it undo the filesystem and network confinement. unshare and
 * setns are denied outright; clone is denied only when it asks for a new namespace, so threads (and
 * a compiler's fork) keep working; clone3 passes its flags in memory, which seccomp cannot inspect,
 * so it fails with ENOSYS, and the C library (glibc 2.34+ creates threads with clone3) falls back to
 * clone.
 *
 * A run of the submission may not create processes either, only threads: fork and vfork fail with
 * EPERM, and so does clone without CLONE_THREAD (which is how the C library forks and posix_spawn
 * spawns). The run is then a single process, whose CPU time and memory are exactly those of the
 * program, and which leaves nothing behind once it is gone. Threads count against RLIMIT_NPROC.
 * Compilers are trusted and legitimately run other programs (the gcc driver, go build, ...).
 *
 * Network sockets are denied too: socket and socketpair fail with EACCES for every address family
 * but AF_UNIX. The private network namespace cannot be created without CAP_SYS_ADMIN (e.g. in a
 * default Docker container), and without it a submission could reach the judger's database and
 * message queue or the internet. Local AF_UNIX sockets stay available (runtimes use socketpair) and
 * are guarded by file permissions. io_uring, whose IORING_OP_SOCKET would create a socket without
 * calling socket(), fails with ENOSYS, so runtimes such as libuv fall back to plain system calls.
 * Only native-architecture system calls pass the filter (libseccomp kills any other ABI, such as
 * i386's socketcall), so these rules cannot be bypassed through a compat entry point.
 * @param  runOfSubmission - whether the filter is for a run of the submission (not a compilation)
 * @return the exported BPF bytes, or an empty string if the filter (or any one of its rules) could
 *         not be built - a partial filter is never returned
 */
static std::string buildSeccompProgram(bool runOfSubmission) {
    scmp_filter_ctx ctx = seccomp_init(SCMP_ACT_ALLOW);
    if ( ctx == nullptr ) {
        return std::string();
    }
    // Every rule must make it into the filter: one that silently failed to be added would leave a
    // hole (e.g. sockets allowed) in a filter that otherwise looks fine.
    bool allRulesAdded = true;
    auto addRule = [&allRulesAdded](int rc) {
        if ( rc != 0 ) {
            allRulesAdded = false;
        }
    };
    const int deniedSyscalls[] = {
        SCMP_SYS(ptrace),            SCMP_SYS(process_vm_readv), SCMP_SYS(process_vm_writev),
        SCMP_SYS(mount),             SCMP_SYS(umount2),          SCMP_SYS(pivot_root),
        SCMP_SYS(chroot),            SCMP_SYS(reboot),           SCMP_SYS(kexec_load),
        SCMP_SYS(init_module),       SCMP_SYS(finit_module),     SCMP_SYS(delete_module),
        SCMP_SYS(swapon),            SCMP_SYS(swapoff),          SCMP_SYS(settimeofday),
        SCMP_SYS(clock_settime),     SCMP_SYS(adjtimex),         SCMP_SYS(setns),
        SCMP_SYS(unshare),
        // Leaving the process group would escape the kill(-pgid) that ends a compilation, letting
        // a process outlive it and tamper with later runs.
        SCMP_SYS(setsid),            SCMP_SYS(setpgid),
    };
    for ( int syscallNr : deniedSyscalls ) {
        if ( syscallNr >= 0 ) {
            addRule(seccomp_rule_add(ctx, SCMP_ACT_ERRNO(EPERM), syscallNr, 0));
        }
    }
    if ( runOfSubmission ) {
        // fork and vfork do not exist on every architecture (arm64 has clone only).
        const int processSyscalls[] = { SCMP_SYS(fork), SCMP_SYS(vfork) };
        for ( int syscallNr : processSyscalls ) {
            if ( syscallNr >= 0 ) {
                addRule(seccomp_rule_add(ctx, SCMP_ACT_ERRNO(EPERM), syscallNr, 0));
            }
        }
        addRule(seccomp_rule_add(ctx, SCMP_ACT_ERRNO(EPERM), SCMP_SYS(clone), 1,
                                 SCMP_A0(SCMP_CMP_MASKED_EQ, CLONE_THREAD, 0)));
    }

    // clone(flags, ...): the flags are the first argument on every architecture this builds for
    // (amd64/arm64; s390 swaps the first two). One rule per flag: any of them set means EPERM.
    const unsigned long long namespaceFlags[] = {
        CLONE_NEWUSER, CLONE_NEWNS, CLONE_NEWPID, CLONE_NEWNET, CLONE_NEWUTS, CLONE_NEWIPC,
        CLONE_NEWCGROUP,
    };
    for ( unsigned long long flag : namespaceFlags ) {
        addRule(seccomp_rule_add(ctx, SCMP_ACT_ERRNO(EPERM), SCMP_SYS(clone), 1,
                                 SCMP_A0(SCMP_CMP_MASKED_EQ, flag, flag)));
    }
    // Resolved by name, so this still builds against a libseccomp that predates clone3.
    int clone3Nr = seccomp_syscall_resolve_name("clone3");
    if ( clone3Nr != __NR_SCMP_ERROR ) {
        addRule(seccomp_rule_add(ctx, SCMP_ACT_ERRNO(ENOSYS), clone3Nr, 0));
    }

    // socket(domain, ...) and socketpair(domain, ...): an allow-list of AF_UNIX. The comparison is
    // on the full 64-bit argument, so a value with stray upper bits is refused as well.
    const int socketSyscalls[] = { SCMP_SYS(socket), SCMP_SYS(socketpair) };
    for ( int syscallNr : socketSyscalls ) {
        addRule(seccomp_rule_add(ctx, SCMP_ACT_ERRNO(EACCES), syscallNr, 1,
                                 SCMP_A0(SCMP_CMP_NE, (scmp_datum_t) AF_UNIX)));
    }
    const char* const ioUringSyscalls[] = { "io_uring_setup", "io_uring_enter", "io_uring_register" };
    for ( const char* name : ioUringSyscalls ) {
        int syscallNr = seccomp_syscall_resolve_name(name);
        if ( syscallNr != __NR_SCMP_ERROR ) {
            addRule(seccomp_rule_add(ctx, SCMP_ACT_ERRNO(ENOSYS), syscallNr, 0));
        }
    }
    if ( !allRulesAdded ) {
        seccomp_release(ctx);
        return std::string();
    }

    std::string program;
    int memFd = memfd_create("voj-seccomp", 0);
    if ( memFd >= 0 ) {
        if ( seccomp_export_bpf(ctx, memFd) == 0 ) {
            off_t size = lseek(memFd, 0, SEEK_END);
            if ( size > 0 ) {
                lseek(memFd, 0, SEEK_SET);
                program.resize((size_t) size);
                ssize_t got = read(memFd, &program[0], (size_t) size);
                if ( got != size ) {
                    program.clear();
                }
            }
        }
        close(memFd);
    }
    seccomp_release(ctx);
    return program;
}

/**
 * Waits (bounded) until a process group that has been sent SIGKILL is empty. A member whose parent
 * died is reparented to init or, when the judger is PID 1 (as in its Docker image), to the judger,
 * which then reaps it here: only this group is waited for, so the JVM's other children are never
 * touched.
 * @param  pgid - the process group ID (> 0)
 */
static void awaitProcessGroupExit(pid_t pgid) {
    for ( int attempt = 0; attempt < CLEANUP_ATTEMPTS; ++attempt ) {
        while ( waitpid(-pgid, nullptr, WNOHANG | __WALL) > 0 ) {
            // Keep reaping.
        }
        if ( kill(-pgid, 0) != 0 ) {
            return; // ESRCH: nothing is left in the group
        }
        usleep(POLL_INTERVAL_US);
    }
}

/**
 * Computes the RLIMIT_DATA backstop for a memory limit: 2 x the limit plus a headroom of
 * max(DATA_LIMIT_MIN_HEADROOM_KB, physical RAM / DATA_LIMIT_RAM_DIVISOR).
 * @param  memoryLimitKb - the memory limit in KB (0 means no limit)
 * @return the limit in bytes, or 0 for none
 */
static rlim_t getDataLimit(int memoryLimitKb) {
    if ( memoryLimitKb <= 0 ) {
        return 0;
    }
    unsigned long long headroomKb = DATA_LIMIT_MIN_HEADROOM_KB;
    long pages    = sysconf(_SC_PHYS_PAGES);
    long pageSize = sysconf(_SC_PAGESIZE);
    if ( pages > 0 && pageSize > 0 ) {
        unsigned long long ramKb = (unsigned long long) pages * (unsigned long long) pageSize / 1024;
        if ( ramKb / DATA_LIMIT_RAM_DIVISOR > headroomKb ) {
            headroomKb = ramKb / DATA_LIMIT_RAM_DIVISOR;
        }
    }
    return (rlim_t) ((2ULL * (unsigned long long) memoryLimitKb + headroomKb) * 1024);
}

/**
 * Gets a monotonic timestamp in milliseconds (immune to wall-clock adjustments).
 * @return the current monotonic time in milliseconds
 */
static long long getMonotonicMillis() {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (long long) ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}

/**
 * Reads a file of /proc/<pid> (in one read: the files read here are small) as a string.
 * @param  pid    - the process ID
 * @param  name   - the file name, e.g. "stat"
 * @param  buffer - receives the NUL-terminated content
 * @param  size   - the size of the buffer
 * @return whether anything could be read
 */
static bool readProcessFile(pid_t pid, const char* name, char* buffer, size_t size) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/%s", (int) pid, name);
    int fd = open(path, O_RDONLY | O_CLOEXEC);
    if ( fd < 0 ) {
        return false;
    }
    ssize_t length = read(fd, buffer, size - 1);
    close(fd);
    if ( length <= 0 ) {
        return false;
    }
    buffer[length] = '\0';
    return true;
}

/**
 * Reads the consumed CPU time (user + system, every thread) of a process from /proc.
 * @param  pid - the process ID
 * @return the consumed CPU time in milliseconds, or -1 if it cannot be read
 */
static long long readCpuTimeMillis(pid_t pid) {
    char buffer[1024];
    if ( !readProcessFile(pid, "stat", buffer, sizeof(buffer)) ) {
        return -1;
    }
    // The comm field (field 2) may contain spaces and parentheses, so parse after the last ')'.
    // utime and stime are fields 14 and 15.
    const char*        fields = strrchr(buffer, ')');
    unsigned long long utime  = 0;
    unsigned long long stime  = 0;
    if ( fields == nullptr ||
         sscanf(fields + 1, " %*s %*s %*s %*s %*s %*s %*s %*s %*s %*s %*s %llu %llu",
                &utime, &stime) != 2 ) {
        return -1;
    }
    long hertz = sysconf(_SC_CLK_TCK);
    if ( hertz <= 0 ) {
        hertz = 100;
    }
    return (long long) ((utime + stime) * 1000 / (unsigned long long) hertz);
}

/**
 * Reads the peak resident set size (high-water mark) of a process from /proc. VmHWM is reset by
 * exec, so - unlike rusage's ru_maxrss - it reflects only the program's own memory, not the JVM
 * image briefly shared with the child across the fork/exec window.
 * @param  pid - the process ID
 * @return the peak resident set size in KB, or -1 if it cannot be read
 */
static int readPeakMemoryKb(pid_t pid) {
    char buffer[4096];
    if ( !readProcessFile(pid, "status", buffer, sizeof(buffer)) ) {
        return -1;
    }
    const char* line = strstr(buffer, "\nVmHWM:");
    return line != nullptr ? atoi(line + 7) : -1;
}
