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

#include <fcntl.h>
#include <grp.h>
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

/**
 * Tunables for the sandbox.
 */
static const mode_t    OUTPUT_FILE_MODE       = 0644;
static const int       POLL_INTERVAL_US       = 1000;        // watchdog poll (1 ms)
static const int       WALL_TIME_FACTOR       = 3;           // wall-clock backstop multiplier
static const long long WALL_TIME_SLACK_MS     = 1000;        // wall-clock backstop slack
static const rlim_t    OUTPUT_FILE_SIZE_LIMIT = (rlim_t)256 * 1024 * 1024; // RLIMIT_FSIZE (256 MB)
static const rlim_t    PROCESS_NUMBER_LIMIT   = 64;          // RLIMIT_NPROC (anti fork-bomb)

/**
 * Everything the (async-signal-safe) child needs is resolved in the parent before fork, so that
 * the child never has to call malloc, NSS or the C++ runtime while the rest of the JVM's threads
 * are frozen by fork.
 */
struct ChildPlan {
  std::string              executable;     // absolute path or PATH-resolved program to exec
  std::vector<char*>       argv;           // NULL-terminated argument vector
  std::string              inputFilePath;  // stdin redirect (empty = none)
  std::string              outputFilePath; // stdout/stderr redirect (empty = none)
  int                      timeLimit;      // ms (0 = no limit)
  bool                     dropPrivileges; // whether to setuid/setgid
  uid_t                    dropUid;
  gid_t                    dropGid;
  std::string              seccompProgram; // exported classic BPF bytes (empty = no filter)
};

/**
 * Function Prototypes.
 */
static std::vector<std::string> splitCommandLine(const std::string&);
static std::string resolveExecutable(const std::string&);
static std::string buildSeccompProgram();
static long long getMonotonicMillis();
static int  readCpuTimeMillis(pid_t);
static int  readResidentMemoryKb(pid_t);
static void runChild(const ChildPlan&, int syncWriteFd) __attribute__((noreturn));

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
 * @param  jOutputFilePath - the output file path after running the program (may be NULL)
 * @param  timeLimit       - the program execution time limit (ms, 0 means no limit)
 * @param  memoryLimit     - the program execution memory limit (KB, 0 means no limit)
 * @return a Map<String, Object> object containing the runtime result
 */
JNIEXPORT jobject JNICALL Java_org_verwandlung_voj_judger_core_Runner_getRuntimeResult(
    JNIEnv* jniEnv, jobject selfReference, jstring jCommandLine, jstring jUsername,
    jstring jPassword, jstring jInputFilePath, jstring jOutputFilePath, jint timeLimit,
    jint memoryLimit) {
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
    plan.timeLimit      = timeLimit;
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

    plan.seccompProgram = buildSeccompProgram();

    // A close-on-exec pipe: it stays open until execvp succeeds (then it auto-closes, the parent's
    // read sees EOF) or the child reports an errno on failure. The parent blocks on it so the
    // watchdog only starts once the real program is running, avoiding measuring the JVM image that
    // the freshly forked child still shares before exec.
    int syncPipe[2];
    if ( pipe2(syncPipe, O_CLOEXEC) != 0 ) {
        throwStringException(jniEnv, std::string("Judger: pipe2() failed: ") + strerror(errno));
        return NULL;
    }

    pid_t pid = fork();
    if ( pid < 0 ) {
        close(syncPipe[0]);
        close(syncPipe[1]);
        throwStringException(jniEnv, std::string("Judger: fork() failed: ") + strerror(errno));
        return NULL;
    }
    if ( pid == 0 ) {
        close(syncPipe[0]);
        runChild(plan, syncPipe[1]); // never returns
    }

    // ----- Parent process -----
    setpgid(pid, pid); // close the setpgid race with the child
    close(syncPipe[1]);

    int     childErrno = 0;
    ssize_t syncBytes  = 0;
    while ( (syncBytes = read(syncPipe[0], &childErrno, sizeof(childErrno))) < 0 && errno == EINTR ) {
        // Retry on interrupted read.
    }
    close(syncPipe[0]);

    std::atomic<bool> finished(false);
    std::atomic<bool> timedOut(false);
    std::atomic<bool> memoryExceeded(false);
    long long startMillis = getMonotonicMillis();

    std::thread watchdog([&]() {
        long long wallBackstopMs =
            (timeLimit > 0) ? (long long) timeLimit * WALL_TIME_FACTOR + WALL_TIME_SLACK_MS : 0;
        while ( !finished.load() ) {
            if ( timeLimit > 0 ) {
                int cpuMs = readCpuTimeMillis(pid);
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
            if ( memoryLimit > 0 ) {
                int rssKb = readResidentMemoryKb(pid);
                if ( rssKb > memoryLimit ) {
                    memoryExceeded.store(true);
                    kill(-pid, SIGKILL);
                }
            }
            usleep(POLL_INTERVAL_US);
        }
    });

    int           status = 0;
    struct rusage usage;
    memset(&usage, 0, sizeof(usage));
    while ( wait4(pid, &status, 0, &usage) < 0 && errno == EINTR ) {
        // Retry on interrupted system call.
    }
    finished.store(true);
    watchdog.join();

    // Reap any leftover children (e.g. from a fork bomb) still in the group.
    kill(-pid, SIGKILL);

    // Accurate, kernel-accounted CPU time and peak resident memory.
    long long cpuMillis =
        (long long) usage.ru_utime.tv_sec * 1000 + usage.ru_utime.tv_usec / 1000 +
        (long long) usage.ru_stime.tv_sec * 1000 + usage.ru_stime.tv_usec / 1000;
    usedTime   = (int) cpuMillis;
    usedMemory = (int) usage.ru_maxrss; // ru_maxrss is in KB on Linux

    if ( syncBytes > 0 ) {
        // The child never reached the program: exec failed.
        exitCode = 127;
    } else if ( WIFEXITED(status) ) {
        exitCode = WEXITSTATUS(status);
    } else if ( WIFSIGNALED(status) ) {
        exitCode = 128 + WTERMSIG(status);
    } else {
        exitCode = 1;
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
 * The body of the forked child. Runs only async-signal-safe operations (no malloc, NSS or C++
 * runtime), because the surrounding JVM is multi-threaded and fork leaves its locks frozen.
 * Everything else was resolved into the ChildPlan before fork. On any failure it reports the
 * errno through the sync pipe and exits; on success execvp replaces the image and closes the
 * close-on-exec pipe, which the parent observes as EOF.
 *
 * @param  plan        - the pre-resolved execution plan
 * @param  syncWriteFd - the write end of the close-on-exec sync pipe
 */
static void runChild(const ChildPlan& plan, int syncWriteFd) {
    // I/O redirection.
    if ( !plan.inputFilePath.empty() ) {
        int inputFd = open(plan.inputFilePath.c_str(), O_RDONLY);
        if ( inputFd < 0 ) {
            _exit(127);
        }
        dup2(inputFd, STDIN_FILENO);
        close(inputFd);
    }
    if ( !plan.outputFilePath.empty() ) {
        int outputFd = open(plan.outputFilePath.c_str(), O_CREAT | O_WRONLY | O_TRUNC, OUTPUT_FILE_MODE);
        if ( outputFd < 0 ) {
            _exit(127);
        }
        dup2(outputFd, STDOUT_FILENO);
        dup2(outputFd, STDERR_FILENO);
        close(outputFd);
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
    // dedicated user would count, and throttle, every process owned by the shared account.
    if ( plan.dropPrivileges ) {
        limit.rlim_cur = PROCESS_NUMBER_LIMIT;
        limit.rlim_max = PROCESS_NUMBER_LIMIT;
        setrlimit(RLIMIT_NPROC, &limit);

        setgroups(0, nullptr);
        if ( setgid(plan.dropGid) != 0 ) {
            _exit(127);
        }
        if ( setuid(plan.dropUid) != 0 ) {
            _exit(127);
        }
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

    execv(plan.executable.c_str(), plan.argv.data());

    // execv only returns on failure: report the errno and exit.
    int execErrno = errno;
    ssize_t ignored = write(syncWriteFd, &execErrno, sizeof(execErrno));
    (void) ignored;
    _exit(127);
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
 * otherwise PATH is searched, so the child can call execv (which does not search PATH and so does
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
        path = "/usr/local/bin:/usr/bin:/bin";
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
 * Builds the seccomp syscall filter and exports it as a classic BPF program. Uses a default-allow
 * policy and denies a curated set of syscalls that a competitive-programming submission never
 * legitimately needs but that are useful for sandbox escape, host tampering or persistence. Rules
 * are added by syscall name through libseccomp, so the filter is portable across architectures
 * (amd64/arm64). The program is exported here, in the parent, so the child can install it without
 * calling into libseccomp.
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
    };
    for ( int syscallNr : deniedSyscalls ) {
        if ( syscallNr >= 0 ) {
            seccomp_rule_add(ctx, SCMP_ACT_ERRNO(EPERM), syscallNr, 0);
        }
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
 * Reads the current resident set size (physical memory) of a process from /proc.
 * @param  pid - the process ID
 * @return the resident set size in KB, or -1 if it cannot be read
 */
static int readResidentMemoryKb(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/status", (int) pid);

    FILE* fp = fopen(path, "r");
    if ( fp == nullptr ) {
        return -1;
    }
    char line[256];
    int  residentMemoryKb = -1;
    while ( fgets(line, sizeof(line), fp) != nullptr ) {
        if ( strncmp(line, "VmRSS:", 6) == 0 ) {
            residentMemoryKb = atoi(line + 6);
            break;
        }
    }
    fclose(fp);
    return residentMemoryKb;
}
