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
#include <initializer_list>
#include <iterator>
#include <sstream>
#include <string>
#include <system_error>
#include <thread>
#include <vector>

#include <elf.h>
#include <fcntl.h>
#include <grp.h>
#include <link.h>
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

// madvise(2) advice of Linux 5.4+, which older C libraries may not define; the same on every
// architecture this builds for.
#ifndef MADV_COLD
#define MADV_COLD 20
#endif
#ifndef MADV_PAGEOUT
#define MADV_PAGEOUT 21
#endif

// Landlock (Linux 5.13+), declared here: the kernel headers of older C libraries lack <linux/landlock.h>
// or the access rights of its later ABI versions. The system calls have the same numbers on every
// architecture. See https://docs.kernel.org/userspace-api/landlock.html
#ifndef SYS_landlock_create_ruleset
#define SYS_landlock_create_ruleset 444
#endif
#ifndef SYS_landlock_add_rule
#define SYS_landlock_add_rule 445
#endif
#ifndef SYS_landlock_restrict_self
#define SYS_landlock_restrict_self 446
#endif
namespace landlock {
struct RulesetAttr {
  uint64_t handledAccessFs;
  uint64_t handledAccessNet; // ABI 4
  uint64_t scoped;           // ABI 6
};
struct PathBeneathAttr {
  uint64_t allowedAccess;
  int32_t  parentFd;
} __attribute__((packed));
static const uint32_t CREATE_RULESET_VERSION     = 1U << 0;
static const int      RULE_PATH_BENEATH          = 1;
static const uint64_t ACCESS_FS_WRITE_FILE       = 1ULL << 1;
static const uint64_t ACCESS_FS_REMOVE_DIR       = 1ULL << 4;
static const uint64_t ACCESS_FS_REMOVE_FILE      = 1ULL << 5;
static const uint64_t ACCESS_FS_MAKE_CHAR        = 1ULL << 6;
static const uint64_t ACCESS_FS_MAKE_DIR         = 1ULL << 7;
static const uint64_t ACCESS_FS_MAKE_REG         = 1ULL << 8;
static const uint64_t ACCESS_FS_MAKE_SOCK        = 1ULL << 9;
static const uint64_t ACCESS_FS_MAKE_FIFO        = 1ULL << 10;
static const uint64_t ACCESS_FS_MAKE_BLOCK       = 1ULL << 11;
static const uint64_t ACCESS_FS_MAKE_SYM         = 1ULL << 12;
static const uint64_t ACCESS_FS_REFER            = 1ULL << 13; // ABI 2
static const uint64_t ACCESS_FS_TRUNCATE         = 1ULL << 14; // ABI 3
static const uint64_t SCOPE_ABSTRACT_UNIX_SOCKET = 1ULL << 0;  // ABI 6
static const uint64_t SCOPE_SIGNAL               = 1ULL << 1;  // ABI 6
}

/**
 * Tunables for the sandbox.
 */
static const mode_t    OUTPUT_FILE_MODE       = 0644;
static const int       POLL_INTERVAL_US       = 1000;        // watchdog poll (1 ms)
static const int       WALL_TIME_FACTOR       = 3;           // wall-clock backstop multiplier
static const long long WALL_TIME_SLACK_MS     = 1000;        // wall-clock backstop slack
static const rlim_t    OUTPUT_FILE_SIZE_LIMIT = (rlim_t)256 * 1024 * 1024; // RLIMIT_FSIZE (256 MB)
static const rlim_t    PROCESS_NUMBER_LIMIT   = 64;          // default RLIMIT_NPROC (threads)
// RLIMIT_NOFILE (soft = hard, so it cannot be raised): open pipes and sockets hold kernel memory
// that no memory limit sees (a socket pair with full buffers holds hundreds of KB), so a run gets
// few descriptors. Its runtimes need a handful (Java, Go, Python, ...) to a few dozen: Node.js
// opens about 24, and about 6 more for each worker thread, so 128 leaves room for some 16 workers.
// A compilation runs trusted toolchains that open many files at once (javac and kotlinc their
// jars, go build its packages), so it gets more, as a backstop against a toolchain gone wrong.
static const rlim_t    RUN_FILE_LIMIT         = 128;
static const rlim_t    COMPILE_FILE_LIMIT     = 4096;
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
    "GOROOT", "GOENV", "GOTOOLCHAIN", "GOFLAGS",
    "RUSTUP_HOME", "RUSTUP_TOOLCHAIN", "CARGO_HOME",
};
/**
 * The Go toolchain's writable state, which must never be shared between submissions: a cache that
 * one submission (compiled or run as the sandbox user) can write is one it can poison for every
 * later Go build. These are not inherited but placed in the work directory, which is fresh and
 * private for every submission (the judger may seed the build cache there from a read-only copy,
 * see judger.goBuildCacheSeed).
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
  std::string              workDirectory;  // working directory, HOME and the only writable
                                           // directory (empty without an output file: none)
  bool                     compileStep;    // compile: stderr also goes to outputFilePath
  int                      timeLimit;      // ms (0 = no limit)
  rlim_t                   dataLimit;      // RLIMIT_DATA in bytes (0 = none)
  unsigned long long       pageSize;       // the system's page size in bytes
  rlim_t                   processLimit;   // RLIMIT_NPROC of the sandbox user (processes + threads)
  rlim_t                   fileLimit;      // RLIMIT_NOFILE (open descriptors per process)
  bool                     dropPrivileges; // whether to setuid/setgid
  uid_t                    dropUid;
  gid_t                    dropGid;
  std::vector<std::string> seccompPrograms; // classic BPF filters, in the order to install them
};

/**
 * The limit a program exceeded, reported as "limitExceeded" in the result (the Java side maps it to
 * the verdict before looking at the exit code).
 */
enum LimitExceeded {
  LIMIT_NONE = 0, LIMIT_TIME = 1, LIMIT_MEMORY = 2, LIMIT_OUTPUT = 3,
};

/**
 * The steps that prepare the sandbox before exec. A step that fails is reported to the judger, with
 * its errno, through the sync pipe, and the judger refuses the run with an exception (a system
 * error), so that a broken sandbox is never mistaken for a fault of the submission.
 */
enum ChildStep {
  STEP_DESCRIPTORS, STEP_INPUT, STEP_PROCESS_GROUP, STEP_RESOURCE_LIMIT, STEP_GROUPS, STEP_GID,
  STEP_UID, STEP_WORK_DIRECTORY, STEP_OUTPUT, STEP_DEV_NULL, STEP_NO_NEW_PRIVS, STEP_LANDLOCK,
  STEP_LANDLOCK_TRUNCATE, STEP_SECCOMP, STEP_EXEC,
  // Not a failure: the program's static data alone exceeds RLIMIT_DATA (see getWritableImageSize).
  STEP_STATIC_DATA_LIMIT,
};
static const char* const CHILD_STEP_NAMES[] = {
  "marking inherited descriptors close-on-exec", "opening the input file", "setsid()",
  "setrlimit()", "setgroups()", "setgid()", "setuid()", "entering the work directory",
  "opening the output file", "opening /dev/null", "prctl(PR_SET_NO_NEW_PRIVS)",
  "confining writes to the work directory with Landlock (needs Linux 5.13+ with Landlock enabled)",
  "confining truncation to the work directory with Landlock (needs Linux 6.2+, Landlock ABI 3, "
  "unless the judger runs as root and drops privileges to the sandbox user)",
  "installing the seccomp filters", "execve()",
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
static std::string getWorkDirectory(const std::string& outputFilePath);
static std::vector<std::string> buildEnvironment(const std::string& workDirectory);
static std::vector<std::string> buildSeccompPrograms(bool runOfSubmission, rlim_t dataLimit);
static long long getMonotonicMillis();
static long long readCpuTimeMillis(pid_t);
static int  readPeakMemoryKb(pid_t);
static rlim_t getDataLimit(int memoryLimitKb);
static bool reachedOutputLimit(int status, const std::string& outputFilePath);
static void awaitProcessGroupExit(pid_t);
static void runProgram(const ChildPlan&, int syncWriteFd) __attribute__((noreturn));
static void reportChildFailure(int syncWriteFd, int step) __attribute__((noreturn));
static bool setResourceLimit(int resource, rlim_t soft, rlim_t hard);
static int  openOutputFile(const char*);
static bool markInheritedDescriptorsCloseOnExec();
static long getLandlockAbi();
static bool confineFileSystemWrites(const std::string& workDirectory, long landlockAbi);
static unsigned long long getWritableImageSize(const char* path, unsigned long long pageSize);

/**
 * JNI call entry point.
 * Runs the given command in a sandboxed child process and measures its CPU time and peak memory
 * usage. Limits are enforced both by the kernel (setrlimit) and by a fine-grained watchdog;
 * isolation is layered: the seccomp syscall filters (an allowlist for a run of the submission, a
 * deny-list for a compilation, see buildSeccompPrograms), the Landlock rules that confine writes to
 * the work directory and (when running as root) the privilege drop are mandatory - the run is
 * refused if any of them cannot be applied - while the network namespace is best-effort, as the
 * filters already refuse every socket but AF_UNIX. Without the privilege drop (a judger that is not
 * root), the Landlock rules must also cover truncation, which needs Linux 6.2 (see runProgram).
 *
 * Every compilation and run is the same sandbox user, so whatever one of them could leave behind
 * outside its own work directory (a file in /tmp or /dev/shm, a System V shared memory segment, a
 * POSIX message queue, a key in the user's keyring, ...) would outlive it, unaccounted for, and
 * could be read by a later submission (e.g. a run copying its hidden input to /tmp, for a later
 * compilation to #include it into its compile log). Landlock (see confineFileSystemWrites) and the
 * filters (System V IPC, POSIX message queues and kernel keyrings) rule that out; they need no
 * privilege, so they hold in a default Docker container as well. The work directory is removed once
 * the submission has been judged.
 *
 * A run of the submission is a single process: the filters let it create threads, but no other
 * process (see buildSeccompPrograms). A compilation runs a trusted compiler, which may run others;
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
 * @return a Map<String, Object> with the CPU time ("usedTime", ms), the peak memory ("usedMemory",
 *         KB), the exit code ("exitCode", 128 + the signal if killed) and the limit the program
 *         exceeded ("limitExceeded", see LimitExceeded); throws a Java exception instead if the
 *         sandbox could not be set up
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
    plan.workDirectory  = getWorkDirectory(plan.outputFilePath);
    // A compiler's diagnostics (stderr) are the compile log, but a submission's stderr must never
    // reach the judged output.
    plan.compileStep    = (isCompilation == JNI_TRUE);
    plan.timeLimit      = timeLimit;
    plan.dataLimit      = getDataLimit(memoryLimit);
    long pageSize       = sysconf(_SC_PAGESIZE);
    plan.pageSize       = pageSize > 0 ? (unsigned long long) pageSize : 4096;
    plan.processLimit   = (processLimit > 0) ? (rlim_t) processLimit : PROCESS_NUMBER_LIMIT;
    plan.fileLimit      = plan.compileStep ? COMPILE_FILE_LIMIT : RUN_FILE_LIMIT;
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

    plan.environment = buildEnvironment(plan.workDirectory);
    for ( std::string& variable : plan.environment ) {
        plan.envp.push_back(const_cast<char*>(variable.c_str()));
    }
    plan.envp.push_back(nullptr);

    // The filters are what keep a submission off the network when the judger has no CAP_SYS_ADMIN
    // (a default Docker container), and away from ptrace, namespaces, setsid and other processes
    // everywhere: a run without them is refused rather than started unconfined.
    plan.seccompPrograms = buildSeccompPrograms(!plan.compileStep, plan.dataLimit);
    if ( plan.seccompPrograms.empty() ) {
        throwStringException(jniEnv, "Judger: failed to build the seccomp filters; refusing to run");
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
        while ( waitpid(pid, nullptr, 0) < 0 && errno == EINTR ) {
            // Retry on interrupted system call.
        }
        if ( syncBytes == (ssize_t) sizeof(childFailure) && childFailure.step == STEP_STATIC_DATA_LIMIT ) {
            // The program's static data alone exceeds the memory backstop: execve would have
            // failed past its point of no return, killing the program with SIGSEGV.
            JHashMap result;
            result.put("usedTime", 0);
            result.put("usedMemory", memoryLimit);
            result.put("exitCode", 128 + SIGSEGV);
            result.put("limitExceeded", (jint) LIMIT_MEMORY);
            return result.toJObject(jniEnv);
        }
        // The sandbox could not be set up, so the program never ran: a system error, never a
        // verdict on the submission.
        std::string message = "Judger: the sandbox failed to start the program";
        if ( syncBytes == (ssize_t) sizeof(childFailure) && childFailure.step >= 0 &&
             childFailure.step < (int) (sizeof(CHILD_STEP_NAMES) / sizeof(CHILD_STEP_NAMES[0])) ) {
            message = std::string("Judger: ") + CHILD_STEP_NAMES[childFailure.step] +
                      " failed in the sandbox: " + strerror(childFailure.error);
        }
        throwStringException(jniEnv, message + " (" + commandLine + ")");
        return NULL;
    }

    // The program has been exec'd and is in its own session and process group (both pid), set up
    // before exec.
    std::atomic<bool> finished(false);
    std::atomic<int>  limitExceeded(LIMIT_NONE); // the first limit the watchdog enforced
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
                        int none = LIMIT_NONE;
                        limitExceeded.compare_exchange_strong(none, LIMIT_TIME);
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
                    int none = LIMIT_NONE;
                    limitExceeded.compare_exchange_strong(none, LIMIT_MEMORY);
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
    // are denied by the seccomp filters, so nothing can have left the group.
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

    // What the watchdog did not stop still counts, whatever the exit code: CPU time it had not
    // seen yet (it polls), a mapping beyond the memory backstop, which the seccomp filters stop, and
    // the output limit, which the kernel enforces.
    int limit = limitExceeded.load();
    if ( limit == LIMIT_NONE && timeLimit > 0 && usedTime > timeLimit ) {
        limit = LIMIT_TIME;
    }
    if ( limit == LIMIT_NONE && !plan.compileStep && plan.dataLimit > 0 && WIFSIGNALED(status) &&
         WTERMSIG(status) == SIGSYS ) {
        limit = LIMIT_MEMORY; // a single mapping beyond RLIMIT_DATA (see buildSeccompPrograms)
    }
    if ( limit == LIMIT_NONE && reachedOutputLimit(status, plan.outputFilePath) ) {
        limit = LIMIT_OUTPUT;
    }
    // A program stopped at a limit is reported as having used at least that much.
    if ( limit == LIMIT_TIME && usedTime < timeLimit ) {
        usedTime = timeLimit;
    }
    if ( limit == LIMIT_MEMORY && memoryLimit > 0 && usedMemory < memoryLimit ) {
        usedMemory = memoryLimit;
    }

    JHashMap result;
    result.put("usedTime", usedTime);
    result.put("usedMemory", usedMemory);
    result.put("exitCode", exitCode);
    result.put("limitExceeded", limit);
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

    // Run in a session of its own, which is also a process group of its own (both the child's pid),
    // so that the watchdog can signal the whole tree. A new session has no controlling terminal: in
    // the judger's session, the program could open the judger's terminal (/dev/tty) and, where the
    // kernel still allows TIOCSTI, push input into the shell that started the judger. (As the
    // session's leader, it cannot take a terminal another session controls.)
    if ( setsid() < 0 ) {
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
    // container lacks; the seccomp filters, which refuse every socket but AF_UNIX, do not.
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

    // Run in the submission's own work directory, never in the judger's (e.g. /root in its Docker
    // image, which the sandbox user cannot enter: the Go toolchain runs its compiler there).
    if ( chdir(plan.workDirectory.empty() ? "/" : plan.workDirectory.c_str()) != 0 ) {
        reportChildFailure(syncWriteFd, STEP_WORK_DIRECTORY);
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

    // A program whose static data (initialised data and bss, e.g. `static int a[1 << 30];`) alone
    // exceeds the backstop below cannot even be loaded: execve would fail past its point of no
    // return, and the kernel would kill the program with a SIGSEGV that looks like a runtime error.
    // Like a single mapping beyond the backstop (see buildSeccompPrograms), it is Memory Limit
    // Exceeded. The file is read with the sandbox user's credentials, as execve would read it.
    if ( !plan.compileStep && plan.dataLimit > 0 &&
         getWritableImageSize(plan.executable.c_str(), plan.pageSize) > (unsigned long long) plan.dataLimit ) {
        reportChildFailure(syncWriteFd, STEP_STATIC_DATA_LIMIT);
    }

    // Tier 1: a per-process kernel memory backstop, inherited by every child. RLIMIT_DATA (private
    // writable mappings, i.e. roughly committed memory) is used rather than RLIMIT_AS, because the
    // JVM, V8 (Node.js) and the Go runtime reserve far more address space than they ever commit and
    // do not start under a tight RLIMIT_AS. It is generous on purpose (see getDataLimit), so the
    // watchdog normally reports the overrun as MLE first; the backstop only bounds how far a single
    // process can balloon between two polls (a run's single mapping beyond it is MLE as well, see
    // buildSeccompPrograms). Set last: the forked JVM image itself is far larger.
    if ( plan.dataLimit > 0 && !setResourceLimit(RLIMIT_DATA, plan.dataLimit, plan.dataLimit) ) {
        reportChildFailure(syncWriteFd, STEP_RESOURCE_LIMIT);
    }

    if ( prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0) != 0 ) {
        reportChildFailure(syncWriteFd, STEP_NO_NEW_PRIVS);
    }
    // Mandatory as well (it needs no_new_privs): nothing may outlive the program outside its work
    // directory. Before Landlock ABI 3 (Linux 6.2), truncation is not a right Landlock handles, so
    // truncate(2) and open(O_TRUNC) still reach every file the program's user may write. That is
    // acceptable for the sandbox user, a separate account that cannot write the judger's files. A
    // program running as the judger's own user (a judger that is not root) could empty them
    // instead, e.g. the cached expected output, so that an empty output would be accepted: such a
    // run is refused.
    long landlockAbi = getLandlockAbi();
    if ( landlockAbi < 1 ) {
        reportChildFailure(syncWriteFd, STEP_LANDLOCK);
    }
    if ( landlockAbi < 3 && !plan.dropPrivileges ) {
        errno = EOPNOTSUPP;
        reportChildFailure(syncWriteFd, STEP_LANDLOCK_TRUNCATE);
    }
    if ( !confineFileSystemWrites(plan.workDirectory, landlockAbi) ) {
        reportChildFailure(syncWriteFd, STEP_LANDLOCK);
    }
    // Tier 1: the open descriptors, each of which may hold kernel memory no limit sees (a pipe or a
    // socket pair with full buffers), soft = hard so the program cannot raise it. Set after every
    // step that opens one: the JVM's own descriptors, which close only on exec, may already be above
    // the limit, so that a later open would fail with EMFILE. The filters keep a run's threads on
    // one descriptor table (see buildSeccompPrograms), so the limit holds for the run as a whole.
    if ( !setResourceLimit(RLIMIT_NOFILE, plan.fileLimit, plan.fileLimit) ) {
        reportChildFailure(syncWriteFd, STEP_RESOURCE_LIMIT);
    }
    // Install the pre-built seccomp filters via the raw system call, so the child does not have to
    // call into libseccomp (which would allocate), after Landlock's rules, whose system calls are no
    // concern of the filters, and in their order (see buildSeccompPrograms). Mandatory: if one
    // cannot be installed, the program is not run.
    for ( const std::string& program : plan.seccompPrograms ) {
        struct sock_fprog prog;
        prog.len    = (unsigned short) (program.size() / sizeof(struct sock_filter));
        prog.filter = (struct sock_filter*) program.data();
        if ( syscall(SYS_seccomp, SECCOMP_SET_MODE_FILTER, 0, &prog) != 0 ) {
            reportChildFailure(syncWriteFd, STEP_SECCOMP);
        }
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
 * Gets the version of the Landlock ABI the running kernel supports. Async-signal-safe.
 * @return the ABI version (1 for Linux 5.13, 3 for Linux 6.2, 6 for Linux 6.12, ...), or -1 if
 *         Landlock is unavailable (errno tells why: ENOSYS without Landlock, EOPNOTSUPP if it is
 *         disabled at boot, EPERM if a container's seccomp profile denies it)
 */
static long getLandlockAbi() {
    return syscall(SYS_landlock_create_ruleset, nullptr, 0, landlock::CREATE_RULESET_VERSION);
}

/**
 * Confines the file system writes of the calling process, and of everything it runs, to the work
 * directory with Landlock: outside it, files cannot be created, written, truncated, renamed, linked
 * or removed (only /dev/null may be written). Descriptors opened before, such as standard output,
 * are not affected, and nothing is hidden: the program can still read whatever the sandbox user can
 * read. Landlock is unprivileged (it only needs no_new_privs), so this holds in a default Docker
 * container too, where a private mount namespace cannot be created. The rights handled are those of
 * the running kernel's Landlock ABI; from ABI 6 (Linux 6.12) on, the program is also scoped: it can
 * neither signal nor connect to an abstract Unix socket of a process outside its own Landlock
 * domain (such as a concurrent run on another judger, also running as the sandbox user). Whatever
 * the ABI, Landlock denies ptrace access, including to /proc/<pid>/fd and the like, to processes
 * outside the domain. Async-signal-safe.
 *
 * TMPDIR and HOME point into the work directory (see buildEnvironment), so the compilers' and
 * runtimes' temporary files and caches land there.
 * See https://docs.kernel.org/userspace-api/landlock.html
 *
 * Before ABI 3 (Linux 6.2), truncation is not handled: files outside the work directory can still
 * be truncated (truncate, ftruncate, open with O_TRUNC) as far as file permissions allow, which is
 * why a run as the judger's own user needs ABI 3 (see runProgram).
 *
 * @param  workDirectory - the only directory to allow writes beneath (empty for none)
 * @param  abi           - the kernel's Landlock ABI version, at least 1 (see getLandlockAbi)
 * @return whether the rules are in force (errno tells why not)
 */
static bool confineFileSystemWrites(const std::string& workDirectory, long abi) {
    uint64_t writeAccess =
        landlock::ACCESS_FS_WRITE_FILE | landlock::ACCESS_FS_REMOVE_DIR |
        landlock::ACCESS_FS_REMOVE_FILE | landlock::ACCESS_FS_MAKE_CHAR | landlock::ACCESS_FS_MAKE_DIR |
        landlock::ACCESS_FS_MAKE_REG | landlock::ACCESS_FS_MAKE_SOCK | landlock::ACCESS_FS_MAKE_FIFO |
        landlock::ACCESS_FS_MAKE_BLOCK | landlock::ACCESS_FS_MAKE_SYM;
    if ( abi >= 2 ) {
        writeAccess |= landlock::ACCESS_FS_REFER;
    }
    if ( abi >= 3 ) {
        writeAccess |= landlock::ACCESS_FS_TRUNCATE;
    }
    landlock::RulesetAttr rulesetAttr;
    memset(&rulesetAttr, 0, sizeof(rulesetAttr));
    rulesetAttr.handledAccessFs = writeAccess;
    if ( abi >= 6 ) {
        rulesetAttr.scoped = landlock::SCOPE_ABSTRACT_UNIX_SOCKET | landlock::SCOPE_SIGNAL;
    }
    int ruleset = (int) syscall(SYS_landlock_create_ruleset, &rulesetAttr, sizeof(rulesetAttr), 0);
    if ( ruleset < 0 ) {
        return false;
    }

    // Adds a rule for a path; O_NOFOLLOW: the work directory itself is never a link.
    auto allow = [ruleset](const char* path, int flags, uint64_t access) {
        int fd = open(path, O_PATH | O_CLOEXEC | O_NOFOLLOW | flags);
        if ( fd < 0 ) {
            return false;
        }
        landlock::PathBeneathAttr pathBeneath;
        pathBeneath.allowedAccess = access;
        pathBeneath.parentFd      = fd;
        bool added = syscall(SYS_landlock_add_rule, ruleset, landlock::RULE_PATH_BENEATH, &pathBeneath, 0) == 0;
        int error = errno;
        close(fd);
        errno = error;
        return added;
    };
    bool confined =
        (workDirectory.empty() || allow(workDirectory.c_str(), O_DIRECTORY, writeAccess)) &&
        allow("/dev/null", 0, writeAccess & (landlock::ACCESS_FS_WRITE_FILE | landlock::ACCESS_FS_TRUNCATE)) &&
        syscall(SYS_landlock_restrict_self, ruleset, 0) == 0;
    int error = errno;
    close(ruleset);
    errno = error;
    return confined;
}

/**
 * Gets the private writable memory the kernel maps for an ELF executable as it loads it: its
 * writable PT_LOAD segments (initialised data and bss), in whole pages. All of it counts against
 * RLIMIT_DATA. Only the main executable is looked at; the dynamic loader and shared libraries add
 * comparatively little. Async-signal-safe.
 * @param  path     - the executable
 * @param  pageSize - the system's page size in bytes
 * @return the size in bytes, or 0 if it is not a native ELF file or cannot be read (e.g. a script)
 */
static unsigned long long getWritableImageSize(const char* path, unsigned long long pageSize) {
    // O_NONBLOCK: never block opening a FIFO planted in place of the program.
    int fd = open(path, O_RDONLY | O_CLOEXEC | O_NOCTTY | O_NONBLOCK);
    if ( fd < 0 ) {
        return 0;
    }
    unsigned long long size = 0;
    struct stat st;
    ElfW(Ehdr) header;
    if ( fstat(fd, &st) == 0 && S_ISREG(st.st_mode) &&
         pread(fd, &header, sizeof(header), 0) == (ssize_t) sizeof(header) &&
         memcmp(header.e_ident, ELFMAG, SELFMAG) == 0 &&
         header.e_ident[EI_CLASS] == (sizeof(void*) == 8 ? ELFCLASS64 : ELFCLASS32) &&
         header.e_phentsize == sizeof(ElfW(Phdr)) ) {
        for ( unsigned int i = 0; i < header.e_phnum; ++i ) {
            ElfW(Phdr) segment;
            if ( pread(fd, &segment, sizeof(segment), (off_t) (header.e_phoff + i * sizeof(segment))) !=
                 (ssize_t) sizeof(segment) ) {
                size = 0;
                break;
            }
            if ( segment.p_type == PT_LOAD && (segment.p_flags & PF_W) != 0 ) {
                unsigned long long start = segment.p_vaddr & ~(pageSize - 1);
                unsigned long long end   = (segment.p_vaddr + segment.p_memsz + pageSize - 1) & ~(pageSize - 1);
                size += end - start;
            }
        }
    }
    close(fd);
    return size;
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
 * Gets the work directory of a compilation or run: the directory holding its output file, which
 * the sandbox user owns.
 * @param  outputFilePath - the output file path (may be empty)
 * @return the work directory, or an empty string without an output file (the program then runs in
 *         /, with nowhere to write)
 */
static std::string getWorkDirectory(const std::string& outputFilePath) {
    size_t lastSlash = outputFilePath.rfind('/');
    return (lastSlash != std::string::npos && lastSlash > 0) ? outputFilePath.substr(0, lastSlash)
                                                             : std::string();
}

/**
 * Builds the minimal environment of the sandboxed process: the whitelisted variables of the judger
 * (INHERITED_ENVIRONMENT_VARIABLES), defaults for PATH and LANG, and HOME and TMPDIR. The program can
 * only write in its work directory (see confineFileSystemWrites), so HOME (the judger's own, e.g.
 * /root, would be useless anyway), TMPDIR (compilers' temporary files: gcc, rustc, go build, ...),
 * the Go build cache (GOCACHE) and GOPATH all point there; the latter two would otherwise be shared
 * between submissions.
 * @param  workDirectory - the work directory (empty for none: HOME is then /, and TMPDIR unset)
 * @return the list of "NAME=value" strings
 */
static std::vector<std::string> buildEnvironment(const std::string& workDirectory) {
    std::vector<std::string> environment;
    for ( const char* name : INHERITED_ENVIRONMENT_VARIABLES ) {
        const char* value = getenv(name);
        if ( value != nullptr ) {
            environment.push_back(std::string(name) + "=" + value);
        }
    }
    if ( getenv("PATH") == nullptr ) {
        environment.push_back(std::string("PATH=") + DEFAULT_PATH);
    }
    if ( getenv("LANG") == nullptr ) {
        environment.push_back(std::string("LANG=") + DEFAULT_LANG);
    }
    if ( workDirectory.empty() ) {
        environment.push_back("HOME=/");
        return environment;
    }
    environment.push_back("HOME=" + workDirectory);
    environment.push_back("TMPDIR=" + workDirectory);
    environment.push_back("GOCACHE=" + workDirectory + GO_BUILD_CACHE_DIRECTORY);
    environment.push_back("GOPATH=" + workDirectory + GO_PATH_DIRECTORY);
    return environment;
}

/**
 * The system calls a run of the submission may make: any other fails with ENOSYS, which lets the C
 * library and the runtimes fall back where they can (clone3 to clone, rseq, io_uring, ...). The
 * list starts from what Docker's default seccomp profile allows unconditionally (seccomp/default.json
 * in https://github.com/moby/profiles; nothing it allows only with a capability) and leaves out:
 *  - what the deny-list filter refuses compilations too (ptrace & co., setsid and setpgid, System V
 *    IPC, POSIX message queues, keyrings, ...: see buildSeccompPrograms), which still answers EPERM;
 *  - other processes: fork, vfork, clone3, execveat, pidfd_open, pidfd_send_signal, kcmp and
 *    process_mrelease (a run is a single process: clone is allowed for threads only);
 *  - memory a limit would not see, or that would leave the resident set: memfd_create, memfd_secret,
 *    process_madvise, mlock & co., remap_file_pages and the NUMA policy calls;
 *  - the network: bind, listen, accept and connect (socket and socketpair are allowed for socket
 *    pairs, the deny-list filter refuses every address family but AF_UNIX), and sending to an
 *    address: sendto is allowed without one only (see buildSeccompPrograms), and sendmsg and
 *    sendmmsg, whose address is in memory, where seccomp cannot see it, not at all (no runtime
 *    sends with them on its own; send and write work on a socket pair). Otherwise a run could send
 *    datagrams to the host's Unix sockets, such as /dev/log or the journal's, without connecting;
 *  - files beyond reading and writing them: their owner, mode, times and extended attributes (which
 *    Landlock does not confine), links, device nodes and FIFOs, watching files (inotify, fanotify),
 *    the writeback of the whole system (sync, syncfs), splice, tee and vmsplice, Linux AIO
 *    (io_setup & co.) and io_uring;
 *  - other processes' resource limits: prlimit64 is allowed on the caller only (see
 *    buildSeccompPrograms);
 *  - credentials, capabilities and scheduling: set*uid, set*gid, setgroups, capset, setpriority,
 *    sched_setaffinity & co., ioprio_set, personality, Landlock; and what else no runtime needs
 *    (cachestat, listmount, statmount, ...).
 * seccomp is allowed: the deny-list filter is installed after this one, and a filter the program
 * adds can only restrict it further. Names are resolved for the native architecture: the list is for
 * amd64 and arm64, where calls that do not exist (open, fork, ... on arm64) are left out.
 */
static const char* const RUN_ALLOWED_SYSCALLS[] = {
    // Files (Landlock confines writes to the work directory).
    "read", "readv", "pread64", "preadv", "preadv2", "write", "writev", "pwrite64", "pwritev",
    "pwritev2", "lseek", "open", "openat", "openat2", "creat", "close", "close_range", "dup",
    "dup2", "dup3", "fcntl", "ioctl", "flock", "pipe", "pipe2", "stat", "fstat", "lstat",
    "newfstatat", "statx", "statfs", "fstatfs", "access", "faccessat", "faccessat2", "getdents",
    "getdents64", "getcwd", "chdir", "fchdir", "readlink", "readlinkat", "mkdir", "mkdirat", "rmdir",
    "unlink", "unlinkat", "rename", "renameat", "renameat2", "truncate", "ftruncate", "fallocate",
    "fadvise64", "readahead", "fsync", "fdatasync", "sendfile", "copy_file_range", "umask",
    // Memory (the deny-list filter checks mmap and madvise).
    "brk", "mmap", "munmap", "mremap", "mprotect", "madvise", "mincore", "msync", "membarrier",
    "pkey_alloc", "pkey_free", "pkey_mprotect", "map_shadow_stack", "mseal",
    // Threads (the deny-list filter allows clone for threads only), futexes and signals.
    "clone", "exit", "exit_group", "set_tid_address", "set_robust_list", "rseq", "futex",
    "futex_waitv", "futex_wait", "futex_wake", "futex_requeue", "sched_yield", "sched_getaffinity",
    "sched_getparam", "sched_getscheduler", "sched_getattr", "sched_get_priority_max",
    "sched_get_priority_min", "sched_rr_get_interval", "getcpu", "getpriority", "rt_sigaction",
    "rt_sigprocmask", "rt_sigreturn", "rt_sigpending", "rt_sigsuspend", "rt_sigtimedwait",
    "rt_sigqueueinfo", "rt_tgsigqueueinfo", "sigaltstack", "kill", "tkill", "tgkill", "pause",
    "restart_syscall", "wait4", "waitid",
    // Waiting, clocks and timers.
    "poll", "ppoll", "select", "pselect6", "epoll_create", "epoll_create1", "epoll_ctl",
    "epoll_wait", "epoll_pwait", "epoll_pwait2", "eventfd", "eventfd2", "signalfd", "signalfd4",
    "nanosleep", "clock_nanosleep", "clock_gettime", "clock_getres", "gettimeofday", "time",
    "times", "alarm", "getitimer", "setitimer", "timer_create", "timer_delete", "timer_gettime",
    "timer_settime", "timer_getoverrun", "timerfd_create", "timerfd_gettime", "timerfd_settime",
    // Socket pairs (the deny-list filter refuses every address family but AF_UNIX; sendto, see
    // buildSeccompPrograms).
    "socket", "socketpair", "recvfrom", "recvmsg", "recvmmsg", "shutdown", "getsockopt",
    "setsockopt", "getsockname", "getpeername",
    // The process itself.
    "execve", "prctl", "arch_prctl", "seccomp", "getrandom", "uname", "sysinfo", "getrusage",
    "getrlimit", "setrlimit", "getpid", "getppid", "gettid", "getpgid", "getpgrp",
    "getsid", "getuid", "geteuid", "getgid", "getegid", "getgroups", "getresuid", "getresgid",
    "capget", "uretprobe",
};

/**
 * The system calls the deny-list filter refuses (EPERM), compilations and runs alike. A compilation
 * runs a trusted compiler, which may run others, too varied to allowlist; these are what neither has
 * any business with but what serves sandbox escape, host tampering or persistence:
 *  - ptrace & co., mounting, modules, the clock, and namespaces (unshare, setns; clone with a
 *    CLONE_NEW* flag is refused by an argument rule): an unprivileged user namespace hands the
 *    program a full set of capabilities inside it, which turns every namespaced kernel interface
 *    into attack surface and lets it undo the filesystem and network confinement;
 *  - leaving the process group (setsid, setpgid), which would escape the kill(-pgid) that ends a
 *    compilation, letting a process outlive it and tamper with later runs;
 *  - System V IPC (shared memory, semaphores, message queues), POSIX message queues and the kernel
 *    keyrings: they outlive the process that created them (a user's keyrings belong to the user), so
 *    a run could leave memory behind that no limit accounts for, or data for a later submission to
 *    pick up (their file system counterparts, such as /dev/shm, are kept out by Landlock).
 * ipc (the System V multiplexer of some 32-bit architectures) is resolved away on amd64 and arm64.
 */
static const char* const DENIED_SYSCALLS[] = {
    "ptrace", "process_vm_readv", "process_vm_writev", "mount", "umount2", "pivot_root", "chroot",
    "reboot", "kexec_load", "init_module", "finit_module", "delete_module", "swapon", "swapoff",
    "settimeofday", "clock_settime", "adjtimex", "setns", "unshare", "setsid", "setpgid", "shmget",
    "shmat", "shmdt", "shmctl", "semget", "semop", "semtimedop", "semctl", "msgget", "msgsnd",
    "msgrcv", "msgctl", "ipc", "mq_open", "mq_unlink", "mq_timedsend", "mq_timedreceive",
    "mq_notify", "mq_getsetattr", "add_key", "request_key", "keyctl",
};

namespace {
/**
 * A seccomp filter being built with libseccomp, which remembers whether every one of its rules
 * made it in: one that silently failed to be added would leave a hole (e.g. sockets allowed) in a
 * filter that otherwise looks fine, so such a filter is never exported.
 */
class SeccompFilter {
  public:
    explicit SeccompFilter(uint32_t defaultAction)
        : ctx_(seccomp_init(defaultAction)), complete_(ctx_ != nullptr) {
        // A system call of a foreign ABI (such as i386's socketcall on amd64) fails rather than
        // killing the program (libseccomp's default), so that SIGSYS only ever means the memory
        // rule; and no rule can be bypassed through a compat entry point.
        if ( ctx_ != nullptr ) {
            check(seccomp_attr_set(ctx_, SCMP_FLTATR_ACT_BADARCH, SCMP_ACT_ERRNO(ENOSYS)));
        }
    }
    ~SeccompFilter() {
        if ( ctx_ != nullptr ) {
            seccomp_release(ctx_);
        }
    }
    SeccompFilter(const SeccompFilter&) = delete;
    SeccompFilter& operator=(const SeccompFilter&) = delete;

    void add(uint32_t action, int syscallNr, std::initializer_list<scmp_arg_cmp> conditions = {}) {
        if ( ctx_ != nullptr ) {
            check(seccomp_rule_add_array(ctx_, action, syscallNr, (unsigned int) conditions.size(),
                                         conditions.begin()));
        }
    }

    // Unconditional rules, by name: a system call that this architecture lacks (fork on arm64) or
    // that this libseccomp does not know is left out.
    template <size_t N> void addEach(uint32_t action, const char* const (&names)[N]) {
        for ( const char* name : names ) {
            int syscallNr = seccomp_syscall_resolve_name(name);
            if ( syscallNr >= 0 ) {
                add(action, syscallNr);
            }
        }
    }

    // Compiles the rules into a binary search tree rather than a linear list (libseccomp 2.5+), so
    // that a long allowlist costs a run a handful of comparisons per system call. Best-effort.
    void optimize() {
#if SCMP_VER_MAJOR > 2 || (SCMP_VER_MAJOR == 2 && SCMP_VER_MINOR >= 5)
        if ( ctx_ != nullptr ) {
            seccomp_attr_set(ctx_, SCMP_FLTATR_CTL_OPTIMIZE, 2);
        }
#endif
    }

    // Exports the filter as a classic BPF program, here in the parent, so that the child can install
    // it without calling into libseccomp. Empty if any rule is missing or the export failed.
    std::string exportProgram() const {
        std::string program;
        int memFd = complete_ ? memfd_create("voj-seccomp", 0) : -1;
        if ( memFd < 0 ) {
            return program;
        }
        if ( seccomp_export_bpf(ctx_, memFd) == 0 ) {
            off_t size = lseek(memFd, 0, SEEK_END);
            if ( size > 0 ) {
                lseek(memFd, 0, SEEK_SET);
                program.resize((size_t) size);
                if ( read(memFd, &program[0], (size_t) size) != size ) {
                    program.clear();
                }
            }
        }
        close(memFd);
        return program;
    }

  private:
    void check(int rc) {
        if ( rc != 0 ) {
            complete_ = false;
        }
    }

    scmp_filter_ctx ctx_;
    bool            complete_;
};
}

/**
 * Builds the seccomp filters of a compilation or of a run of the submission, as classic BPF programs
 * to install in this order. The kernel runs every filter a process has, and the most restrictive
 * action wins (a kill over an errno over allowing); between two errnos, that of the filter installed
 * last.
 *
 * A run gets two: the allowlist (default ENOSYS, see RUN_ALLOWED_SYSCALLS), so that a system call
 * nobody thought of, including one of a future kernel, is refused by default; then the deny-list
 * filter, which a compilation gets on its own. The deny-list filter allows by default and refuses
 * DENIED_SYSCALLS (EPERM) and the arguments below; it comes last, so its errno is the one a run sees
 * for a call both refuse. The argument rules need a filter of their own: libseccomp silently drops
 * the conditional rules of a system call that also has an unconditional one, so the allowlist cannot
 * allow mmap and refuse some of its arguments, and its comparisons cannot express the complement of
 * each rule (allowing what it does not refuse). The allowlist checks arguments only of the calls it
 * allows in a narrow case alone (prlimit64 on the caller itself, sendto without an address), which
 * have no unconditional rule.
 *
 * Both:
 *  - clone fails with EPERM when it asks for a new namespace (CLONE_NEW*), so threads (and a
 *    compiler's fork) keep working. clone3 passes its flags in memory, which seccomp cannot
 *    inspect, so it fails with ENOSYS, and the C library (glibc 2.34+ creates threads with clone3)
 *    falls back to clone;
 *  - socket and socketpair fail with EACCES for every address family but AF_UNIX: the private
 *    network namespace cannot be created without CAP_SYS_ADMIN (e.g. in a default Docker container),
 *    and without it a submission could reach the judger's database and message queue or the
 *    internet. Socket pairs stay available (runtimes use them). io_uring, whose IORING_OP_SOCKET
 *    would create a socket without calling socket(), fails with ENOSYS, so runtimes such as libuv
 *    fall back to plain system calls.
 *
 * A run of the submission only:
 *  - is a single process: clone without CLONE_THREAD (how the C library forks and posix_spawn
 *    spawns) fails with EPERM, as fork and vfork are not allowlisted. Its CPU time and memory are
 *    then exactly those of the program, and nothing is left behind once it is gone. A thread must
 *    share the descriptor table as well (CLONE_FILES, as every C library and runtime does), or
 *    clone fails with EPERM: a thread with a table of its own would get a further RLIMIT_NOFILE of
 *    descriptors (unshare, which could split the table later, is refused);
 *  - cannot move its memory out of its resident set while keeping it, which would let it use far
 *    more than its memory limit, measured on the resident set (VmHWM), without ever reaching it: on
 *    a host with swap, madvise(MADV_PAGEOUT) swaps pages out at once and MADV_COLD makes them the
 *    first to go, so both fail with EPERM (process_madvise, which applies them through a pidfd, is
 *    not allowlisted). The other advice the runtimes' allocators use (glibc, the JVM, Go, V8) either
 *    lets the kernel drop private pages, contents and all (MADV_DONTNEED, MADV_FREE), or keeps them
 *    resident or only tunes their handling. MADV_DONTNEED on shared anonymous memory (MAP_SHARED |
 *    MAP_ANONYMOUS) only unmaps the pages, which stay in memory, with their contents, until touched
 *    again, even without swap; and RLIMIT_DATA does not count shared mappings. So such a mapping
 *    cannot be created (EPERM): a single process has no use for it, and no runtime allocates one. (A
 *    shared mapping of /dev/zero cannot be written, as Landlock refuses to open it for writing, and
 *    one of a file in the work directory holds no more than writing the file would, which
 *    RLIMIT_FSIZE bounds per file);
 *  - is killed (SIGSYS) when it maps more private writable memory in one go than RLIMIT_DATA allows
 *    in all (see getDataLimit), e.g. std::vector<int>(1e9) under a 256 MB limit. The kernel would
 *    refuse that mapping anyway; the program would then fail in its own way (std::bad_alloc,
 *    MemoryError, a JVM or Go runtime abort, ...), which cannot be told apart from a runtime error,
 *    whereas a SIGSYS is only ever this rule (nothing else in the filters kills), so the judger
 *    reports Memory Limit Exceeded. A program that would have handled the failure is stopped there
 *    instead. Mappings that only exceed RLIMIT_DATA together with earlier ones, brk and mremap cannot
 *    be checked: they still fail with ENOMEM, but are rare, as the backstop is more than twice the
 *    limit and the watchdog stops any program whose touched memory exceeds the limit first.
 * @param  runOfSubmission - whether the filters are for a run of the submission (not a compilation)
 * @param  dataLimit       - the run's RLIMIT_DATA in bytes (0 = none)
 * @return the programs, or none if any filter (or any one of its rules) could not be built - a
 *         partial filter is never returned
 */
static std::vector<std::string> buildSeccompPrograms(bool runOfSubmission, rlim_t dataLimit) {
    std::vector<std::string> programs;
    if ( runOfSubmission ) {
        SeccompFilter allowlist(SCMP_ACT_ERRNO(ENOSYS));
        allowlist.addEach(SCMP_ACT_ALLOW, RUN_ALLOWED_SYSCALLS);
        // prlimit64(pid, ...) only on the caller itself (pid 0, which is how the C library's
        // getrlimit and setrlimit call it): another process of the same user, such as the judger
        // itself when it is not root, would otherwise be open to it. Not in RUN_ALLOWED_SYSCALLS, as
        // an unconditional rule would swallow this one.
        allowlist.add(SCMP_ACT_ALLOW, SCMP_SYS(prlimit64), {SCMP_A0(SCMP_CMP_EQ, 0)});
        // sendto(fd, buf, len, flags, dest_addr, addrlen) only without an address (as the C
        // library's send calls it), i.e. on a connected socket: a socket pair.
        allowlist.add(SCMP_ACT_ALLOW, SCMP_SYS(sendto), {SCMP_A4(SCMP_CMP_EQ, 0)});
        allowlist.optimize();
        programs.push_back(allowlist.exportProgram());
    }

    SeccompFilter filter(SCMP_ACT_ALLOW);
    filter.addEach(SCMP_ACT_ERRNO(EPERM), DENIED_SYSCALLS);
    // clone(flags, ...): the flags are the first argument on every architecture this builds for
    // (amd64/arm64; s390 swaps the first two). One rule per flag: any of them set means EPERM.
    const unsigned long long namespaceFlags[] = {
        CLONE_NEWUSER, CLONE_NEWNS, CLONE_NEWPID, CLONE_NEWNET, CLONE_NEWUTS, CLONE_NEWIPC,
        CLONE_NEWCGROUP,
    };
    for ( unsigned long long flag : namespaceFlags ) {
        filter.add(SCMP_ACT_ERRNO(EPERM), SCMP_SYS(clone),
                   {SCMP_A0(SCMP_CMP_MASKED_EQ, flag, flag)});
    }
    // socket(domain, ...) and socketpair(domain, ...): only AF_UNIX. The comparison is on the full
    // 64-bit argument, so a value with stray upper bits is refused as well.
    const int socketSyscalls[] = { SCMP_SYS(socket), SCMP_SYS(socketpair) };
    for ( int syscallNr : socketSyscalls ) {
        filter.add(SCMP_ACT_ERRNO(EACCES), syscallNr, {SCMP_A0(SCMP_CMP_NE, (scmp_datum_t) AF_UNIX)});
    }
    // By name, so this still builds against a libseccomp that predates them.
    const char* const unimplementedSyscalls[] = {
        "clone3", "io_uring_setup", "io_uring_enter", "io_uring_register",
    };
    filter.addEach(SCMP_ACT_ERRNO(ENOSYS), unimplementedSyscalls);

    if ( runOfSubmission ) {
        filter.add(SCMP_ACT_ERRNO(EPERM), SCMP_SYS(clone),
                   {SCMP_A0(SCMP_CMP_MASKED_EQ, CLONE_THREAD, 0)});
        filter.add(SCMP_ACT_ERRNO(EPERM), SCMP_SYS(clone),
                   {SCMP_A0(SCMP_CMP_MASKED_EQ, CLONE_THREAD | CLONE_FILES, CLONE_THREAD)});
        // madvise(addr, length, advice): the advice is an int, of which the kernel ignores the upper
        // half of the register, so only the lower 32 bits are compared.
        const int pageOutAdvice[] = { MADV_COLD, MADV_PAGEOUT };
        for ( int advice : pageOutAdvice ) {
            filter.add(SCMP_ACT_ERRNO(EPERM), SCMP_SYS(madvise),
                       {SCMP_A2(SCMP_CMP_MASKED_EQ, 0xFFFFFFFFULL, (scmp_datum_t) advice)});
        }
        // mmap(addr, length, prot, flags, ...): a shared anonymous mapping, whatever its protection
        // (mprotect could make it writable later). MAP_SHARED is the only shared type the kernel
        // accepts for an anonymous mapping (MAP_SHARED_VALIDATE fails with EINVAL).
        filter.add(SCMP_ACT_ERRNO(EPERM), SCMP_SYS(mmap), {SCMP_A3(SCMP_CMP_MASKED_EQ,
                   MAP_TYPE | MAP_ANONYMOUS, MAP_SHARED | MAP_ANONYMOUS)});
        if ( dataLimit > 0 ) {
            // A private (MAP_TYPE), writable mapping that is not a stack (MAP_GROWSDOWN) counts
            // against RLIMIT_DATA.
            filter.add(SCMP_ACT_KILL_PROCESS, SCMP_SYS(mmap),
                       {SCMP_A1(SCMP_CMP_GT, (scmp_datum_t) dataLimit),
                        SCMP_A2(SCMP_CMP_MASKED_EQ, PROT_WRITE, PROT_WRITE),
                        SCMP_A3(SCMP_CMP_MASKED_EQ, MAP_TYPE | MAP_GROWSDOWN, MAP_PRIVATE)});
        }
    }
    programs.push_back(filter.exportProgram());

    for ( const std::string& program : programs ) {
        if ( program.empty() ) {
            return std::vector<std::string>();
        }
    }
    return programs;
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
 * Checks whether a program ran into the output limit (RLIMIT_FSIZE): either it was killed by the
 * SIGXFSZ the kernel sends, or - if it ignored the signal, so that the write failed with EFBIG
 * instead and the program went on to exit some way - its output file has reached the limit.
 * @param  status         - the program's wait status
 * @param  outputFilePath - the file that received its standard output (may be empty)
 * @return whether the output limit was reached
 */
static bool reachedOutputLimit(int status, const std::string& outputFilePath) {
    if ( WIFSIGNALED(status) && WTERMSIG(status) == SIGXFSZ ) {
        return true;
    }
    // lstat: the program may have replaced the file with a link; never follow it.
    struct stat st;
    return !outputFilePath.empty() && lstat(outputFilePath.c_str(), &st) == 0 &&
           S_ISREG(st.st_mode) && (rlim_t) st.st_size >= OUTPUT_FILE_SIZE_LIMIT;
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
