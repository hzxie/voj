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
#define _GNU_SOURCE
#pragma GCC diagnostic ignored "-Wwrite-strings"

#include "../org_verwandlung_voj_jni_hashmap.h"
#include "../org_verwandlung_voj_jni_library.h"
#include "../org_verwandlung_voj_judger_core_Runner.h"

#include <algorithm>
#include <atomic>
#include <cerrno>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <iterator>
#include <sstream>
#include <string>
#include <thread>
#include <vector>

#include <dirent.h>
#include <fcntl.h>
#include <grp.h>
#include <poll.h>
#include <pwd.h>
#include <sched.h>
#include <signal.h>
#include <sys/mman.h>
#include <sys/prctl.h>
#include <sys/resource.h>
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
static const rlim_t    PROCESS_NUMBER_LIMIT   = 64;          // default RLIMIT_NPROC (anti fork-bomb)
static const int       CLEANUP_ATTEMPTS       = 1000;        // kill/reap rounds (x 1 ms) after a run
static const int       GROUP_SCAN_INTERVAL    = 10;          // scan the whole group every 10th poll
// Reading a process's PSS walks its page tables (~3 ms for a 550 MB JVM, ~1000 x reading its stat),
// so the group's PSS is read at most every 10 ms and at most every 10 x the last read's duration
// (<= 10% of the watchdog's time), and only when it can raise the peak (see the watchdog).
static const long long PSS_SCAN_MIN_INTERVAL_MS = 10;
static const long long PSS_SCAN_COST_FACTOR     = 10;
static const int       SUPERVISOR_EXIT_TIMEOUT_MS = 5000;    // wait for the supervisor after the sweeps
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
  std::string              inputFilePath;  // stdin redirect (empty = none)
  std::string              outputFilePath; // stdout redirect (empty = none)
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
 * A report of the supervisor (see runSupervisor) to the judger, written to a pipe in one atomic
 * write.
 */
enum SupervisorMessageType {
  SUPERVISOR_STARTED = 1, // value: the program's PID (and process group)
  SUPERVISOR_EXITED  = 2, // status: the program's wait status, value: its CPU time (us)
  SUPERVISOR_DONE    = 3, // value: the CPU time of every process of the run (us)
};
struct SupervisorMessage {
  int       type;
  int       status;
  long long value;
};

/**
 * Function Prototypes.
 */
static std::vector<std::string> splitCommandLine(const std::string&);
static std::string resolveExecutable(const std::string&);
static std::vector<std::string> buildEnvironment(bool dropPrivileges, const std::string& outputFilePath);
static std::string buildSeccompProgram();
static long long getMonotonicMillis();
static int  readCpuTimeMillis(pid_t);
static int  readPeakMemoryKb(pid_t);
static bool readProcessGroupUsage(pid_t, long long* cpuMillis,
                                  std::vector<std::pair<pid_t, long long>>* members);
static long long readProportionalSetSizeKb(pid_t);
static rlim_t getDataLimit(int memoryLimitKb);
static void reapProcessGroup(pid_t);
static void killProcessGroup(pid_t);
static void killSandboxUserProcesses(uid_t, pid_t);
static void runSupervisor(const ChildPlan&, int syncWriteFd, int reportWriteFd,
                          long long* reapedCpuMicros) __attribute__((noreturn));
static void runProgram(const ChildPlan&, int syncWriteFd) __attribute__((noreturn));
static void writeSupervisorMessage(int fd, int type, int status, long long value);
static bool readSupervisorMessage(int fd, SupervisorMessage*, int timeoutMs);
static void closeDescriptorsExcept(int keepFd1, int keepFd2);
static void reportChildFailure(int syncWriteFd, int error) __attribute__((noreturn));
static int  openOutputFile(const char*);
static void markInheritedDescriptorsCloseOnExec();

/**
 * JNI call entry point.
 * Runs the given command in a sandboxed child process and measures its CPU time and peak memory
 * usage. Limits are enforced both by the kernel (setrlimit) and by a fine-grained watchdog;
 * isolation is layered and best-effort (network namespace, privilege drop and a seccomp syscall
 * filter are applied when the environment permits).
 *
 * @param  jniEnv          - a reference to the JNI runtime environment
 * @param  selfReference   - a reference to the calling Java object
 * @param  jCommandLine    - the command line to execute
 * @param  jUsername       - the unprivileged user to drop to (when running as root)
 * @param  jPassword       - unused on Unix (privilege drop is resolved from the username)
 * @param  jInputFilePath  - the input file path when running the program (may be NULL)
 * @param  jOutputFilePath - the file receiving stdout (may be NULL); for a compilation it also
 *                           receives stderr, otherwise stderr is discarded
 * @param  timeLimit       - the program execution time limit (ms, 0 means no limit)
 * @param  memoryLimit     - the program execution memory limit (KB, 0 means no limit)
 * @param  isCompilation   - whether this is a compilation (as opposed to a run of the submission)
 * @param  processLimit    - the maximum number of processes and threads of the sandbox user
 *                           (RLIMIT_NPROC, applied when privileges are dropped; 0 means the default)
 * @return a Map<String, Object> object containing the runtime result
 */
JNIEXPORT jobject JNICALL Java_org_verwandlung_voj_judger_core_Runner_getRuntimeResult(
    JNIEnv* jniEnv, jobject selfReference, jstring jCommandLine, jstring jUsername,
    jstring jPassword, jstring jInputFilePath, jstring jOutputFilePath, jint timeLimit,
    jint memoryLimit, jboolean isCompilation, jint processLimit) {
    (void) selfReference;
    (void) jPassword;
    std::string commandLine = getStringValue(jniEnv, jCommandLine);
    std::string username    = getStringValue(jniEnv, jUsername);

    JHashMap result;
    int usedTime   = 0;
    int usedMemory = 0;
    int exitCode   = 127;

    // ----- Resolve everything the child needs while still single-threaded-safe -----
    std::vector<std::string> args = splitCommandLine(commandLine);
    if ( args.empty() ) {
        result.put("usedTime", 0);
        result.put("usedMemory", 0);
        result.put("exitCode", 127);
        return result.toJObject(jniEnv);
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
            result.put("usedTime", 0);
            result.put("usedMemory", 0);
            result.put("exitCode", 127); // never run untrusted code as root
            return result.toJObject(jniEnv);
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

    plan.seccompProgram = buildSeccompProgram();

    // A close-on-exec pipe: it stays open until execve succeeds (then it auto-closes, the parent's
    // read sees EOF) or the child reports an errno on failure. The parent blocks on it so the
    // watchdog only starts once the real program is running, avoiding measuring the JVM image that
    // the freshly forked child still shares before exec.
    int syncPipe[2];
    if ( pipe2(syncPipe, O_CLOEXEC) != 0 ) {
        throwStringException(jniEnv, std::string("Judger: pipe2() failed: ") + strerror(errno));
        return NULL;
    }
    // The supervisor's reports (see runSupervisor): the program's PID, its exit, and the end of the
    // run with the CPU time of every process it ran. A page shared with the supervisor carries the
    // running total of the CPU time of the processes reaped so far, for the watchdog.
    int reportPipe[2];
    if ( pipe2(reportPipe, O_CLOEXEC) != 0 ) {
        close(syncPipe[0]);
        close(syncPipe[1]);
        throwStringException(jniEnv, std::string("Judger: pipe2() failed: ") + strerror(errno));
        return NULL;
    }
    void* sharedPage = mmap(nullptr, sizeof(long long), PROT_READ | PROT_WRITE,
                            MAP_SHARED | MAP_ANONYMOUS, -1, 0);
    if ( sharedPage == MAP_FAILED ) {
        close(syncPipe[0]);
        close(syncPipe[1]);
        close(reportPipe[0]);
        close(reportPipe[1]);
        throwStringException(jniEnv, std::string("Judger: mmap() failed: ") + strerror(errno));
        return NULL;
    }
    long long* reapedCpuMicros = static_cast<long long*>(sharedPage);
    __atomic_store_n(reapedCpuMicros, 0LL, __ATOMIC_SEQ_CST);

    pid_t supervisor = fork();
    if ( supervisor < 0 ) {
        close(syncPipe[0]);
        close(syncPipe[1]);
        close(reportPipe[0]);
        close(reportPipe[1]);
        munmap(sharedPage, sizeof(long long));
        throwStringException(jniEnv, std::string("Judger: fork() failed: ") + strerror(errno));
        return NULL;
    }
    if ( supervisor == 0 ) {
        close(syncPipe[0]);
        close(reportPipe[0]);
        runSupervisor(plan, syncPipe[1], reportPipe[1], reapedCpuMicros); // never returns
    }

    // ----- Parent process -----
    close(syncPipe[1]);
    close(reportPipe[1]);

    // The supervisor reports the program's PID (which is also its process group) once it has forked
    // it; end-of-file instead means the supervisor failed before that, with the errno on syncPipe.
    SupervisorMessage message;
    pid_t pid = -1;
    if ( readSupervisorMessage(reportPipe[0], &message, -1) && message.type == SUPERVISOR_STARTED ) {
        pid = (pid_t) message.value;
    }

    int     childErrno = 0;
    ssize_t syncBytes  = 0;
    while ( (syncBytes = read(syncPipe[0], &childErrno, sizeof(childErrno))) < 0 && errno == EINTR ) {
        // Retry on interrupted read.
    }
    close(syncPipe[0]);

    std::atomic<bool> finished(false);
    std::atomic<bool> timedOut(false);
    std::atomic<bool> memoryExceeded(false);
    std::atomic<int>  peakMemoryKb(0);
    std::atomic<long long> groupCpuMillis(0);
    long long startMillis = getMonotonicMillis();

    // Child processes are accounted for, not only the top-level process: every GROUP_SCAN_INTERVAL
    // polls the watchdog sums the CPU time and the memory (PSS) of the whole process group (the
    // seccomp filter keeps every descendant in it), plus the CPU time of the descendants that have
    // already exited and been reaped by the supervisor. Memory is always summed, so the compile
    // memory limit also covers cc1plus, rustc's linker, go's compile/link children, kotlinc's JVM,
    // ... For a compilation, CPU time keeps being measured on the top-level process only (plus the
    // wall-clock backstop and each process's own RLIMIT_CPU): the toolchains legitimately burn CPU
    // in parallel children (go build, the JVM-based kotlinc) and must not turn into compile errors.
    bool countGroupCpu = !plan.compileStep;

    std::thread watchdog([&]() {
        if ( pid <= 0 ) {
            return; // the program was never started
        }
        // PSS (/proc/<pid>/smaps_rollup) is only readable with ptrace-read access to the process,
        // which root lacks for another user's process without CAP_SYS_PTRACE (Docker drops it).
        // The kernel checks the file-system UID/GID for it, so this thread - and only this thread,
        // which the raw syscalls affect, unlike the glibc wrappers - takes on the sandbox user's
        // for file access. It does nothing but read /proc and send signals (which use the real UID).
        if ( plan.dropPrivileges ) {
            syscall(SYS_setfsgid, plan.dropGid);
            syscall(SYS_setfsuid, plan.dropUid);
        }
        long long wallBackstopMs =
            (timeLimit > 0) ? (long long) timeLimit * WALL_TIME_FACTOR + WALL_TIME_SLACK_MS : 0;
        // The group's memory is the sum of its processes' PSS. Reading it is costly (see
        // PSS_SCAN_COST_FACTOR), so it is only read when it can matter: when the group has more
        // than one process (a single one's VmHWM, sampled on every poll, is at least its PSS) and
        // the cheap sum of their RSS, an upper bound of the PSS sum, exceeds the peak so far. When
        // that happens too soon after the last read, the read waits for a later scan; the memory
        // is never estimated.
        long long nextPssScanMs = 0;
        std::vector<std::pair<pid_t, long long>> members;
        auto scanProcessGroup = [&](long long* cpuMs, long long* memoryKb) {
            if ( !readProcessGroupUsage(pid, cpuMs, &members) ) {
                return false;
            }
            long long rssKb = 0;
            for ( const auto& member : members ) {
                rssKb += member.second;
            }
            long long startMs = getMonotonicMillis();
            if ( members.size() < 2 || rssKb <= peakMemoryKb.load() || startMs < nextPssScanMs ) {
                *memoryKb = (members.size() < 2) ? rssKb : -1;
                return true;
            }
            long long pssKb = 0;
            for ( const auto& member : members ) {
                long long processPssKb = readProportionalSetSizeKb(member.first);
                pssKb += (processPssKb >= 0) ? processPssKb : member.second;
            }
            long long elapsedMs = getMonotonicMillis() - startMs;
            nextPssScanMs = startMs + std::max(PSS_SCAN_MIN_INTERVAL_MS,
                                               elapsedMs * PSS_SCAN_COST_FACTOR);
            *memoryKb = pssKb;
            return true;
        };
        for ( int poll = 0; !finished.load(); ++poll ) {
            long long groupCpuMs = -1;
            long long groupMemKb = -1;
            if ( poll % GROUP_SCAN_INTERVAL == 0 ) {
                // Read the reaped total first: a process reaped during the scan is then missed
                // until the next scan rather than counted twice.
                long long reapedMs = __atomic_load_n(reapedCpuMicros, __ATOMIC_SEQ_CST) / 1000;
                if ( scanProcessGroup(&groupCpuMs, &groupMemKb) ) {
                    groupCpuMs += reapedMs;
                    if ( countGroupCpu && groupCpuMs > groupCpuMillis.load() ) {
                        groupCpuMillis.store(groupCpuMs);
                    }
                }
            }
            if ( timeLimit > 0 ) {
                long long cpuMs = readCpuTimeMillis(pid);
                if ( countGroupCpu && groupCpuMs > cpuMs ) {
                    cpuMs = groupCpuMs;
                }
                if ( cpuMs >= 0 && cpuMs > timeLimit ) {
                    timedOut.store(true);
                    kill(-pid, SIGKILL);
                }
                long long wallMs = getMonotonicMillis() - startMillis;
                if ( wallBackstopMs > 0 && wallMs > wallBackstopMs ) {
                    timedOut.store(true);
                    kill(-pid, SIGKILL);
                }
            }
            // Sample the top-level process's peak RSS from /proc (VmHWM), which also catches a spike
            // between two polls, and the whole group's memory (PSS). The watchdog only runs after
            // the child has exec'd (the parent blocks on the sync pipe until then), so this never
            // sees the JVM image the freshly forked child briefly shared before exec.
            int peakKb = readPeakMemoryKb(pid);
            if ( groupMemKb > peakKb ) {
                peakKb = (int) groupMemKb;
            }
            if ( peakKb > peakMemoryKb.load() ) {
                peakMemoryKb.store(peakKb);
            }
            if ( memoryLimit > 0 && peakKb > memoryLimit ) {
                memoryExceeded.store(true);
                kill(-pid, SIGKILL);
            }
            usleep(POLL_INTERVAL_US);
        }
        if ( plan.dropPrivileges ) {
            syscall(SYS_setfsuid, 0);
            syscall(SYS_setfsgid, 0);
        }
    });

    // Wait until the supervisor has reaped the program (or has died, which never happens normally).
    bool      programExited    = false;
    int       status           = 0;
    long long programCpuMicros = 0;
    while ( pid > 0 && readSupervisorMessage(reportPipe[0], &message, -1) ) {
        if ( message.type == SUPERVISOR_EXITED ) {
            programExited    = true;
            status           = message.status;
            programCpuMicros = message.value;
            break;
        }
    }
    finished.store(true);
    watchdog.join();

    // Kill everything the program left behind (background children, a fork bomb) before the caller
    // looks at its output. setsid/setpgid are denied by the seccomp filter, so every descendant is
    // still in the process group; when running as a dedicated sandbox user, also sweep every
    // process of that user, in case one got out of the group anyway (e.g. no seccomp filter).
    if ( pid > 0 ) {
        killProcessGroup(pid);
    }
    if ( plan.dropPrivileges ) {
        killSandboxUserProcesses(plan.dropUid, pid > 0 ? pid : 0);
    }

    // Every descendant is reparented to the supervisor (a child subreaper) when its parent exits,
    // so once they are all gone the supervisor's children's CPU time is that of every process the
    // run created, including orphans that would otherwise have been reaped by init unaccounted.
    long long totalCpuMicros = __atomic_load_n(reapedCpuMicros, __ATOMIC_SEQ_CST);
    bool      supervisorDone = false;
    while ( readSupervisorMessage(reportPipe[0], &message, SUPERVISOR_EXIT_TIMEOUT_MS) ) {
        if ( message.type == SUPERVISOR_DONE ) {
            supervisorDone = true;
            totalCpuMicros = message.value;
            break;
        }
    }
    close(reportPipe[0]);
    if ( !supervisorDone ) {
        // Something the kill sweeps cannot reach outlived the run: stop waiting for it.
        kill(supervisor, SIGKILL);
    }
    while ( waitpid(supervisor, nullptr, 0) < 0 && errno == EINTR ) {
        // Retry on interrupted system call.
    }
    long long reapedTotal = __atomic_load_n(reapedCpuMicros, __ATOMIC_SEQ_CST);
    if ( reapedTotal > totalCpuMicros ) {
        totalCpuMicros = reapedTotal;
    }
    munmap(sharedPage, sizeof(long long));

    // Kernel-accounted CPU time (user + system): the whole process tree for a run of the
    // submission, the top-level process (including the children it waited for) for a compilation.
    long long cpuMillis = (countGroupCpu ? totalCpuMicros : programCpuMicros) / 1000;
    if ( countGroupCpu && groupCpuMillis.load() > cpuMillis ) {
        cpuMillis = groupCpuMillis.load();
    }
    usedTime   = (int) cpuMillis;
    // Peak memory sampled from /proc after exec. rusage's ru_maxrss is deliberately NOT used: for a
    // child forked from the multi-threaded JVM it can include the parent's resident pages, briefly
    // shared through the fork/exec window, hugely over-reporting memory on some architectures (e.g.
    // a trivial program measured as the whole JVM heap).
    usedMemory = peakMemoryKb.load();

    if ( syncBytes > 0 || pid <= 0 ) {
        // The child never reached the program: exec failed.
        exitCode = 127;
    } else if ( !programExited ) {
        exitCode = 1;
    } else if ( WIFEXITED(status) ) {
        exitCode = WEXITSTATUS(status);
    } else if ( WIFSIGNALED(status) ) {
        exitCode = 128 + WTERMSIG(status);
    } else {
        exitCode = 1;
    }

    // CPU time the watchdog could not see while the run lasted (it polls, and descendants are only
    // counted once reaped) still counts: a run whose total exceeds the limit is a time-out.
    if ( countGroupCpu && timeLimit > 0 && usedTime > timeLimit ) {
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

    result.put("usedTime", usedTime);
    result.put("usedMemory", usedMemory);
    result.put("exitCode", exitCode);
    return result.toJObject(jniEnv);
}

/**
 * The body of the supervisor, the judger's direct child, which forks and then accounts for the
 * sandboxed program. It makes itself a child subreaper (PR_SET_CHILD_SUBREAPER), so every
 * descendant of the program whose parent exits is reparented to it rather than to init (or to the
 * judger, when it is PID 1 in Docker), and it reaps everything until no descendant is left. The
 * CPU time of each reaped process (which includes the children it reaped itself) is added to the
 * shared running total, so the CPU time of a process that exits after being orphaned is not lost.
 * It reports the program's PID, its exit and, once the run's last process is gone, the total.
 *
 * The supervisor keeps root's (the judger's) credentials, so the program, which runs as the
 * sandbox user, can neither signal nor trace it, and it is not in the program's process group, so
 * the kill sweeps leave it alone. Like the program before exec, it is a fork of the multi-threaded
 * JVM and runs only async-signal-safe operations; it never executes anything else.
 *
 * @param  plan            - the pre-resolved execution plan of the program
 * @param  syncWriteFd     - the write end of the close-on-exec sync pipe (handed to the program)
 * @param  reportWriteFd   - the write end of the report pipe
 * @param  reapedCpuMicros - the shared running total of the CPU time of the reaped processes
 */
static void runSupervisor(const ChildPlan& plan, int syncWriteFd, int reportWriteFd,
                          long long* reapedCpuMicros) {
    // Drop the JVM's signal handlers and mask: neither means anything in this process, and the
    // program would otherwise inherit the mask (and any ignored signal) across exec.
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

    // Nothing the JVM has open (database and message-broker sockets, ...) may be held open for as
    // long as the run lasts, nor be inherited by the program.
    closeDescriptorsExcept(syncWriteFd, reportWriteFd);

    if ( prctl(PR_SET_CHILD_SUBREAPER, 1, 0, 0, 0) != 0 ) {
        reportChildFailure(syncWriteFd, errno);
    }

    pid_t program = fork();
    if ( program < 0 ) {
        reportChildFailure(syncWriteFd, errno);
    }
    if ( program == 0 ) {
        close(reportWriteFd);
        runProgram(plan, syncWriteFd); // never returns
    }
    setpgid(program, program); // close the setpgid race with the program
    close(syncWriteFd);
    writeSupervisorMessage(reportWriteFd, SUPERVISOR_STARTED, 0, program);

    long long totalCpuMicros = 0;
    for ( ;; ) {
        int           status = 0;
        struct rusage usage;
        memset(&usage, 0, sizeof(usage));
        pid_t reaped = wait4(-1, &status, __WALL, &usage);
        if ( reaped < 0 ) {
            if ( errno == EINTR ) {
                continue;
            }
            break; // ECHILD: no descendant is left
        }
        long long cpuMicros =
            (long long) usage.ru_utime.tv_sec * 1000000 + usage.ru_utime.tv_usec +
            (long long) usage.ru_stime.tv_sec * 1000000 + usage.ru_stime.tv_usec;
        totalCpuMicros += cpuMicros;
        __atomic_store_n(reapedCpuMicros, totalCpuMicros, __ATOMIC_SEQ_CST);
        if ( reaped == program ) {
            writeSupervisorMessage(reportWriteFd, SUPERVISOR_EXITED, status, cpuMicros);
        }
    }
    writeSupervisorMessage(reportWriteFd, SUPERVISOR_DONE, 0, totalCpuMicros);
    _exit(0);
}

/**
 * Writes a report of the supervisor. Async-signal-safe.
 * @param  fd     - the write end of the report pipe
 * @param  type   - the SupervisorMessageType
 * @param  status - the wait status (SUPERVISOR_EXITED only)
 * @param  value  - the message's value (see SupervisorMessageType)
 */
static void writeSupervisorMessage(int fd, int type, int status, long long value) {
    SupervisorMessage message;
    memset(&message, 0, sizeof(message));
    message.type   = type;
    message.status = status;
    message.value  = value;
    // Smaller than PIPE_BUF, so written atomically, in one piece.
    while ( write(fd, &message, sizeof(message)) < 0 && errno == EINTR ) {
        // Retry on interrupted write.
    }
}

/**
 * Reads a report of the supervisor.
 * @param  fd        - the read end of the report pipe
 * @param  message   - receives the message
 * @param  timeoutMs - how long to wait for it (negative: forever)
 * @return whether a message was read (false on end-of-file, error or time-out)
 */
static bool readSupervisorMessage(int fd, SupervisorMessage* message, int timeoutMs) {
    long long deadline = getMonotonicMillis() + timeoutMs;
    for ( ;; ) {
        struct pollfd pfd;
        pfd.fd      = fd;
        pfd.events  = POLLIN;
        pfd.revents = 0;
        int wait = -1;
        if ( timeoutMs >= 0 ) {
            long long remaining = deadline - getMonotonicMillis();
            wait = remaining > 0 ? (int) remaining : 0;
        }
        int ready = poll(&pfd, 1, wait);
        if ( ready < 0 && errno == EINTR ) {
            continue;
        }
        if ( ready <= 0 ) {
            return false;
        }
        ssize_t length = read(fd, message, sizeof(*message));
        if ( length < 0 && errno == EINTR ) {
            continue;
        }
        return length == (ssize_t) sizeof(*message);
    }
}

/**
 * Closes every file descriptor above standard error except the given two. Async-signal-safe.
 * @param  keepFd1 - a descriptor to keep open
 * @param  keepFd2 - another descriptor to keep open
 */
static void closeDescriptorsExcept(int keepFd1, int keepFd2) {
    unsigned int low  = (unsigned int) (keepFd1 < keepFd2 ? keepFd1 : keepFd2);
    unsigned int high = (unsigned int) (keepFd1 < keepFd2 ? keepFd2 : keepFd1);
    // The ranges [3, low), (low, high) and (high, ~0].
    unsigned int ranges[3][2] = {
        {3U, low - 1}, {low + 1, high - 1}, {high + 1, ~0U},
    };
    for ( auto& range : ranges ) {
        if ( range[0] < 3U || range[0] > range[1] ) {
            continue;
        }
        if ( syscall(SYS_close_range, range[0], range[1], 0U) == 0 ) {
            continue;
        }
        // Fallback for kernels without close_range: walk the descriptor table.
        struct rlimit noFile;
        unsigned int  maxFd = 65536;
        if ( getrlimit(RLIMIT_NOFILE, &noFile) == 0 && noFile.rlim_cur != RLIM_INFINITY &&
             noFile.rlim_cur < ((rlim_t) 1 << 20) ) {
            maxFd = (unsigned int) noFile.rlim_cur;
        }
        for ( unsigned int fd = range[0]; fd <= range[1] && fd < maxFd; ++fd ) {
            close((int) fd);
        }
    }
}

/**
 * The body of the sandboxed program's process before exec. Runs only async-signal-safe operations
 * (no malloc, NSS or C++ runtime), because it is a fork of the multi-threaded JVM, whose locks
 * fork leaves frozen. Everything else was resolved into the ChildPlan before fork. On any failure
 * it reports the errno through the sync pipe and exits; on success execve replaces the image and
 * closes the close-on-exec pipe, which the judger observes as EOF.
 *
 * @param  plan        - the pre-resolved execution plan
 * @param  syncWriteFd - the write end of the close-on-exec sync pipe
 */
static void runProgram(const ChildPlan& plan, int syncWriteFd) {
    // Nothing the JVM has open (database and message-broker sockets, jars, logs, ...) may leak into
    // the sandbox, where it would bypass the network namespace and file permissions.
    markInheritedDescriptorsCloseOnExec();

    // Standard input is opened before the privilege drop: the checkpoint directory is private to the
    // judger, and the path is chosen by the judger, not by the submission.
    if ( !plan.inputFilePath.empty() ) {
        int inputFd = open(plan.inputFilePath.c_str(), O_RDONLY | O_CLOEXEC);
        if ( inputFd < 0 ) {
            reportChildFailure(syncWriteFd, errno);
        }
        dup2(inputFd, STDIN_FILENO);
        close(inputFd);
    }

    // Run in a dedicated process group so the watchdog can signal the whole tree.
    setpgid(0, 0);

    // Tier 1: kernel-enforced resource limits.
    struct rlimit limit;
    if ( plan.timeLimit > 0 ) {
        rlim_t seconds = (rlim_t) ((plan.timeLimit + 999) / 1000) + 1;
        limit.rlim_cur = seconds;
        limit.rlim_max = seconds + 1;
        setrlimit(RLIMIT_CPU, &limit);
    }
    limit.rlim_cur = OUTPUT_FILE_SIZE_LIMIT;
    limit.rlim_max = OUTPUT_FILE_SIZE_LIMIT;
    setrlimit(RLIMIT_FSIZE, &limit);

    // Tier 2 (best-effort): cut off the network.
    unshare(CLONE_NEWNET);

    // Drop privileges to the unprivileged sandbox user (only when running as root). The process
    // count cap is applied only here, because RLIMIT_NPROC is per real-UID: enforcing it without a
    // dedicated user would count, and throttle, every process owned by the shared account. It
    // counts threads too, so a compilation gets a higher cap than a run of the submission: the Go
    // toolchain runs one compiler per CPU, each with a thread per CPU.
    if ( plan.dropPrivileges ) {
        limit.rlim_cur = plan.processLimit;
        limit.rlim_max = plan.processLimit;
        setrlimit(RLIMIT_NPROC, &limit);

        setgroups(0, nullptr);
        if ( setgid(plan.dropGid) != 0 || setuid(plan.dropUid) != 0 ) {
            reportChildFailure(syncWriteFd, errno);
        }
    }

    // The output file is opened only now, with the sandbox user's credentials: the work directory is
    // writable by the submission, which may have planted a symlink (or a hard link, FIFO, ...) at
    // this path during an earlier checkpoint, and root must never follow it.
    if ( !plan.outputFilePath.empty() ) {
        int outputFd = openOutputFile(plan.outputFilePath.c_str());
        if ( outputFd < 0 ) {
            reportChildFailure(syncWriteFd, errno);
        }
        dup2(outputFd, STDOUT_FILENO);
        if ( plan.compileStep ) {
            dup2(outputFd, STDERR_FILENO);
        }
        close(outputFd);
    }
    if ( !plan.compileStep ) {
        int nullFd = open("/dev/null", O_WRONLY | O_CLOEXEC);
        if ( nullFd < 0 ) {
            reportChildFailure(syncWriteFd, errno);
        }
        dup2(nullFd, STDERR_FILENO);
        close(nullFd);
    }

    // Tier 1: a per-process kernel memory backstop, inherited by every child. RLIMIT_DATA (private
    // writable mappings, i.e. roughly committed memory) is used rather than RLIMIT_AS, because the
    // JVM, V8 (Node.js) and the Go runtime reserve far more address space than they ever commit and
    // do not start under a tight RLIMIT_AS. It is generous on purpose (see getDataLimit), so the
    // watchdog normally reports the overrun as MLE first; the backstop only bounds how far a single
    // process can balloon between two polls. Set last: the forked JVM image itself is far larger.
    if ( plan.dataLimit > 0 ) {
        limit.rlim_cur = plan.dataLimit;
        limit.rlim_max = plan.dataLimit;
        setrlimit(RLIMIT_DATA, &limit);
    }

    // Tier 2 (best-effort): install the pre-built seccomp syscall filter via the raw syscall, so
    // the child does not have to call into libseccomp (which would allocate).
    if ( !plan.seccompProgram.empty() ) {
        struct sock_fprog prog;
        prog.len    = (unsigned short) (plan.seccompProgram.size() / sizeof(struct sock_filter));
        prog.filter = (struct sock_filter*) plan.seccompProgram.data();
        prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0);
        syscall(SYS_seccomp, SECCOMP_SET_MODE_FILTER, 0, &prog);
    }

    execve(plan.executable.c_str(), plan.argv.data(), plan.envp.data());

    // execve only returns on failure: report the errno and exit.
    reportChildFailure(syncWriteFd, errno);
}

/**
 * Reports a failure of the forked child to the parent through the sync pipe and exits. The parent
 * then knows the program never ran (exit code 127). Async-signal-safe.
 *
 * @param  syncWriteFd - the write end of the close-on-exec sync pipe
 * @param  error       - the errno to report
 */
static void reportChildFailure(int syncWriteFd, int error) {
    ssize_t ignored = write(syncWriteFd, &error, sizeof(error));
    (void) ignored;
    _exit(127);
}

/**
 * Marks every file descriptor above standard error close-on-exec, so the sandboxed program only
 * inherits stdin, stdout and stderr. Closing them right away is not an option: the sync pipe must
 * stay open until exec. Async-signal-safe.
 */
static void markInheritedDescriptorsCloseOnExec() {
    if ( syscall(SYS_close_range, 3U, ~0U, CLOSE_RANGE_CLOEXEC) == 0 ) {
        return;
    }
    // Fallback for kernels without close_range: walk the whole descriptor table.
    struct rlimit noFile;
    rlim_t        maxFd = 65536;
    if ( getrlimit(RLIMIT_NOFILE, &noFile) == 0 && noFile.rlim_cur != RLIM_INFINITY ) {
        maxFd = noFile.rlim_cur;
    }
    if ( maxFd > ((rlim_t) 1 << 20) ) {
        maxFd = (rlim_t) 1 << 20;
    }
    for ( rlim_t fd = 3; fd < maxFd; ++fd ) {
        fcntl((int) fd, F_SETFD, FD_CLOEXEC);
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
 * work directory (the directory holding the output file), which the sandbox user owns.
 * @param  dropPrivileges - whether the child drops to the sandbox user
 * @param  outputFilePath - the output file path (its directory is the work directory)
 * @return the list of "NAME=value" strings
 */
static std::vector<std::string> buildEnvironment(bool dropPrivileges, const std::string& outputFilePath) {
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

    const char* home = getenv("HOME");
    if ( dropPrivileges || home == nullptr ) {
        size_t      lastSlash     = outputFilePath.rfind('/');
        std::string workDirectory = (lastSlash != std::string::npos && lastSlash > 0)
                                        ? outputFilePath.substr(0, lastSlash)
                                        : std::string("/tmp");
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
 * setns are denied outright; clone is denied only when it asks for a new namespace, so threads and
 * fork keep working; clone3 passes its flags in memory, which seccomp cannot inspect, so it fails
 * with ENOSYS, and the C library (glibc 2.34+ creates threads with clone3) falls back to clone.
 * @return the exported BPF bytes, or an empty string if the filter could not be built
 */
static std::string buildSeccompProgram() {
    scmp_filter_ctx ctx = seccomp_init(SCMP_ACT_ALLOW);
    if ( ctx == nullptr ) {
        return std::string();
    }
    const int deniedSyscalls[] = {
        SCMP_SYS(ptrace),            SCMP_SYS(process_vm_readv), SCMP_SYS(process_vm_writev),
        SCMP_SYS(mount),             SCMP_SYS(umount2),          SCMP_SYS(pivot_root),
        SCMP_SYS(chroot),            SCMP_SYS(reboot),           SCMP_SYS(kexec_load),
        SCMP_SYS(init_module),       SCMP_SYS(finit_module),     SCMP_SYS(delete_module),
        SCMP_SYS(swapon),            SCMP_SYS(swapoff),          SCMP_SYS(settimeofday),
        SCMP_SYS(clock_settime),     SCMP_SYS(adjtimex),         SCMP_SYS(setns),
        SCMP_SYS(unshare),
        // Leaving the process group would escape the watchdog's kill(-pgid) and the group's
        // resource accounting, letting a process outlive the run and tamper with later ones.
        SCMP_SYS(setsid),            SCMP_SYS(setpgid),
    };
    for ( int syscallNr : deniedSyscalls ) {
        if ( syscallNr >= 0 ) {
            seccomp_rule_add(ctx, SCMP_ACT_ERRNO(EPERM), syscallNr, 0);
        }
    }

    // clone(flags, ...): the flags are the first argument on every architecture this builds for
    // (amd64/arm64; s390 swaps the first two). One rule per flag: any of them set means EPERM.
    const unsigned long long namespaceFlags[] = {
        CLONE_NEWUSER, CLONE_NEWNS, CLONE_NEWPID, CLONE_NEWNET, CLONE_NEWUTS, CLONE_NEWIPC,
        CLONE_NEWCGROUP,
    };
    for ( unsigned long long flag : namespaceFlags ) {
        seccomp_rule_add(ctx, SCMP_ACT_ERRNO(EPERM), SCMP_SYS(clone), 1,
                         SCMP_A0(SCMP_CMP_MASKED_EQ, flag, flag));
    }
    // Resolved by name, so this still builds against a libseccomp that predates clone3.
    int clone3Nr = seccomp_syscall_resolve_name("clone3");
    if ( clone3Nr != __NR_SCMP_ERROR ) {
        seccomp_rule_add(ctx, SCMP_ACT_ERRNO(ENOSYS), clone3Nr, 0);
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
 * Reaps the members of a process group that are children of this process. The program's
 * descendants are reparented to the supervisor, which reaps them; only if the supervisor died would
 * they be reparented to init or, when the judger is PID 1 (e.g. in its Docker image), to us, and
 * linger as zombies counted against RLIMIT_NPROC. Only the given group is waited for, so the JVM's
 * other children are never touched.
 * @param  pgid - the process group ID
 */
static void reapProcessGroup(pid_t pgid) {
    if ( pgid <= 0 ) {
        return; // waitpid(0) or waitpid(-1) would reap the JVM's own children
    }
    while ( waitpid(-pgid, nullptr, WNOHANG | __WALL) > 0 ) {
        // Keep reaping.
    }
}

/**
 * Kills every process in the group and waits (bounded) until the group is empty.
 * @param  pgid - the process group ID
 */
static void killProcessGroup(pid_t pgid) {
    for ( int attempt = 0; attempt < CLEANUP_ATTEMPTS; ++attempt ) {
        reapProcessGroup(pgid);
        if ( kill(-pgid, SIGKILL) != 0 ) {
            return; // ESRCH: nothing is left in the group
        }
        usleep(POLL_INTERVAL_US);
    }
}

/**
 * Kills every process owned by the sandbox user. A helper process drops to that user and calls
 * kill(-1, SIGKILL), which signals every process it is allowed to signal (all of the sandbox user's
 * processes in this PID namespace) except itself, until none is left. This is only safe when the
 * sandbox user is dedicated to the judger, i.e. when privileges are dropped; it is never called
 * otherwise, since it would kill the judger's own user's processes.
 * @param  uid  - the sandbox user ID (never root)
 * @param  pgid - the run's process group, reaped meanwhile in case its members are our children
 */
static void killSandboxUserProcesses(uid_t uid, pid_t pgid) {
    if ( uid == 0 ) {
        return;
    }
    pid_t helper = fork();
    if ( helper < 0 ) {
        return;
    }
    if ( helper == 0 ) {
        // Async-signal-safe only: this is a fork of the multi-threaded JVM. A failed or partial
        // setuid must never reach kill(-1), which as root would signal every process on the host.
        if ( setuid(uid) != 0 || getuid() != uid || geteuid() != uid ) {
            _exit(2);
        }
        struct timespec pause = {0, POLL_INTERVAL_US * 1000L};
        for ( int attempt = 0; attempt < CLEANUP_ATTEMPTS; ++attempt ) {
            if ( kill(-1, SIGKILL) != 0 ) {
                _exit(errno == ESRCH ? 0 : 3); // ESRCH: no process of the user is left
            }
            nanosleep(&pause, nullptr);
        }
        _exit(1);
    }
    for ( ;; ) {
        reapProcessGroup(pgid);
        pid_t waited = waitpid(helper, nullptr, WNOHANG);
        if ( waited == helper || (waited < 0 && errno != EINTR) ) {
            break;
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
 * Sums the CPU time of every live process in a process group and lists them with their resident
 * set size (RSS), by scanning /proc. A process's CPU time includes its children that it has already
 * waited for, so a process tree is counted once. The RSS counts a page shared between processes
 * (e.g. after fork) once per process, so their sum is an upper bound of the group's memory; the
 * watchdog reads their proportional set size (readProportionalSetSizeKb) when it needs the exact
 * figure.
 * @param  pgid      - the process group ID
 * @param  cpuMillis - receives the total CPU time (user + system) in milliseconds
 * @param  members   - receives the group's processes and their RSS in KB
 * @return whether /proc could be scanned
 */
static bool readProcessGroupUsage(pid_t pgid, long long* cpuMillis,
                                  std::vector<std::pair<pid_t, long long>>* members) {
    members->clear();
    DIR* procDir = opendir("/proc");
    if ( procDir == nullptr ) {
        return false;
    }
    long hertz    = sysconf(_SC_CLK_TCK);
    long pageSize = sysconf(_SC_PAGESIZE);
    if ( hertz <= 0 ) {
        hertz = 100;
    }
    if ( pageSize <= 0 ) {
        pageSize = 4096;
    }
    long long ticks = 0;
    struct dirent* entry;
    while ( (entry = readdir(procDir)) != nullptr ) {
        if ( entry->d_name[0] < '0' || entry->d_name[0] > '9' ) {
            continue;
        }
        char path[64];
        snprintf(path, sizeof(path), "/proc/%s/stat", entry->d_name);
        int fd = open(path, O_RDONLY | O_CLOEXEC);
        if ( fd < 0 ) {
            continue;
        }
        char    buffer[1024];
        ssize_t length = read(fd, buffer, sizeof(buffer) - 1);
        close(fd);
        if ( length <= 0 ) {
            continue;
        }
        buffer[length] = '\0';

        // The comm field may contain spaces/parentheses, so parse after the last ')'. The fields
        // read are 3 (state), 5 (pgrp), 14-17 (utime, stime, cutime, cstime) and 24 (rss).
        char* rightParen = strrchr(buffer, ')');
        if ( rightParen == nullptr ) {
            continue;
        }
        char      state;
        int       processGroup;
        long long utime, stime, cutime, cstime, rss;
        int fields = sscanf(rightParen + 1,
            " %c %*s %d %*s %*s %*s %*s %*s %*s %*s %*s %lld %lld %lld %lld %*s %*s %*s %*s %*s %*s %lld",
            &state, &processGroup, &utime, &stime, &cutime, &cstime, &rss);
        if ( fields != 7 || processGroup != pgid || state == 'Z' || state == 'X' ) {
            continue;
        }
        ticks += utime + stime + cutime + cstime;
        members->push_back(std::make_pair((pid_t) atoi(entry->d_name), rss * (pageSize / 1024)));
    }
    closedir(procDir);
    *cpuMillis = ticks * 1000 / hertz;
    return true;
}

/**
 * Reads the proportional set size (PSS) of a process from /proc/<pid>/smaps_rollup (Linux 4.14+):
 * a page shared by n processes counts 1/n towards each, so the PSS of a group's processes adds up
 * pages shared after fork, or the shared libraries of the JVM and other runtimes, only once.
 * Reading it requires ptrace-read access to the process (see the watchdog) and walks its page
 * tables, so it costs far more than its stat.
 * @param  pid - the process ID
 * @return the PSS in KB, or -1 if it cannot be read (the caller then counts the RSS instead)
 */
static long long readProportionalSetSizeKb(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/smaps_rollup", (int) pid);
    int fd = open(path, O_RDONLY | O_CLOEXEC);
    if ( fd < 0 ) {
        return -1;
    }
    // The file is a header line and a few dozen "Name:   value kB" lines; Pss is the third field.
    char    buffer[4096];
    ssize_t length = read(fd, buffer, sizeof(buffer) - 1);
    close(fd);
    if ( length <= 0 ) {
        return -1;
    }
    buffer[length] = '\0';
    const char* pss = strstr(buffer, "\nPss:");
    if ( pss == nullptr ) {
        return -1;
    }
    long long kb = -1;
    return sscanf(pss + 5, " %lld", &kb) == 1 ? kb : -1;
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
 * Reads the consumed CPU time (user + system) of a process from /proc.
 * @param  pid - the process ID
 * @return the consumed CPU time in milliseconds, or -1 if it cannot be read
 */
static int readCpuTimeMillis(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/stat", (int) pid);

    FILE* fp = fopen(path, "r");
    if ( fp == nullptr ) {
        return -1;
    }
    std::string content;
    char        buffer[4096];
    size_t      bytesRead;
    while ( (bytesRead = fread(buffer, 1, sizeof(buffer), fp)) > 0 ) {
        content.append(buffer, bytesRead);
    }
    fclose(fp);

    // The comm field (field 2) may contain spaces/parentheses, so parse after the last ')'.
    // The remaining tokens start at field 3 (state); utime is field 14, stime is field 15.
    size_t rightParen = content.rfind(')');
    if ( rightParen == std::string::npos ) {
        return -1;
    }
    std::istringstream iss(content.substr(rightParen + 1));
    std::vector<std::string> tokens{
        std::istream_iterator<std::string>{iss},
        std::istream_iterator<std::string>{}
    };
    if ( tokens.size() <= 12 ) {
        return -1;
    }
    long long ticks = atoll(tokens[11].c_str()) + atoll(tokens[12].c_str());
    long      hertz = sysconf(_SC_CLK_TCK);
    if ( hertz <= 0 ) {
        hertz = 100;
    }
    return (int) (ticks * 1000 / hertz);
}

/**
 * Reads the peak resident set size (high-water mark) of a process from /proc. VmHWM is reset by
 * exec, so - unlike rusage's ru_maxrss - it reflects only the program's own memory, not the JVM
 * image briefly shared with the child across the fork/exec window.
 * @param  pid - the process ID
 * @return the peak resident set size in KB, or -1 if it cannot be read
 */
static int readPeakMemoryKb(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/status", (int) pid);

    FILE* fp = fopen(path, "r");
    if ( fp == nullptr ) {
        return -1;
    }
    char line[256];
    int  peakMemoryKb = -1;
    while ( fgets(line, sizeof(line), fp) != nullptr ) {
        if ( strncmp(line, "VmHWM:", 6) == 0 ) {
            peakMemoryKb = atoi(line + 6);
            break;
        }
    }
    fclose(fp);
    return peakMemoryKb;
}
