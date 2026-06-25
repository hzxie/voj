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
#include <iostream>
#include <iterator>
#include <sstream>
#include <string>
#include <thread>
#include <vector>

#include <fcntl.h>
#include <grp.h>
#include <pwd.h>
#include <sched.h>
#include <seccomp.h>
#include <signal.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>

/**
 * Tunables for the sandbox.
 */
static const mode_t OUTPUT_FILE_MODE          = 0644;
static const int    POLL_INTERVAL_US          = 1000;        // watchdog poll (1 ms)
static const int    WALL_TIME_FACTOR          = 3;           // wall-clock backstop multiplier
static const long long WALL_TIME_SLACK_MS     = 1000;        // wall-clock backstop slack
static const rlim_t OUTPUT_FILE_SIZE_LIMIT    = (rlim_t)256 * 1024 * 1024; // RLIMIT_FSIZE (256 MB)
static const rlim_t PROCESS_NUMBER_LIMIT      = 64;          // RLIMIT_NPROC (anti fork-bomb)

/**
 * Function Prototypes.
 */
static std::vector<std::string> splitCommandLine(const std::string&);
static long long getMonotonicMillis();
static int  readCpuTimeMillis(pid_t);
static int  readResidentMemoryKb(pid_t);
static void applyResourceLimits(int timeLimit);
static void dropPrivileges(const std::string& username);
static void applySeccompFilter();

/**
 * JNI call entry point.
 * Runs the given command in a sandboxed child process and measures its CPU time and peak
 * memory usage. Time and memory limits are enforced both by the kernel (setrlimit) and by a
 * fine-grained watchdog; isolation is layered and best-effort (network namespace, privilege
 * drop and a seccomp syscall filter are applied when the environment permits).
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
    std::string commandLine    = getStringValue(jniEnv, jCommandLine);
    std::string username       = getStringValue(jniEnv, jUsername);
    std::string inputFilePath  = getStringValue(jniEnv, jInputFilePath);
    std::string outputFilePath = getStringValue(jniEnv, jOutputFilePath);

    std::cout << "[INFO] Command Line: " << commandLine << std::endl;

    JHashMap result;
    int usedTime   = 0;
    int usedMemory = 0;
    int exitCode   = 127;

    std::vector<std::string> args = splitCommandLine(commandLine);
    if ( args.empty() ) {
        result.put("usedTime", 0);
        result.put("usedMemory", 0);
        result.put("exitCode", 127);
        return result.toJObject(jniEnv);
    }

    pid_t pid = fork();
    if ( pid < 0 ) {
        throwStringException(jniEnv, std::string("Judger: fork() failed: ") + strerror(errno));
        return NULL;
    }

    if ( pid == 0 ) {
        // ----- Child process -----
        // I/O redirection.
        if ( !inputFilePath.empty() ) {
            int inputFd = open(inputFilePath.c_str(), O_RDONLY);
            if ( inputFd < 0 ) {
                _exit(127);
            }
            dup2(inputFd, STDIN_FILENO);
            close(inputFd);
        }
        if ( !outputFilePath.empty() ) {
            int outputFd = open(outputFilePath.c_str(), O_CREAT | O_WRONLY | O_TRUNC, OUTPUT_FILE_MODE);
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
        applyResourceLimits(timeLimit);

        // Tier 2 (best-effort): cut off the network, drop privileges, restrict syscalls.
        unshare(CLONE_NEWNET);
        dropPrivileges(username);
        applySeccompFilter();

        // Execute the target program.
        std::vector<char*> argv;
        argv.reserve(args.size() + 1);
        for ( std::string& arg : args ) {
            argv.push_back(const_cast<char*>(arg.c_str()));
        }
        argv.push_back(nullptr);
        execvp(argv[0], argv.data());

        // execvp only returns on failure.
        _exit(127);
    }

    // ----- Parent process -----
    setpgid(pid, pid); // close the setpgid race with the child

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

    if ( WIFEXITED(status) ) {
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

    std::cout << "[DEBUG] usedTime: "   << usedTime   << " ms" << std::endl;
    std::cout << "[DEBUG] usedMemory: " << usedMemory << " KB" << std::endl;
    std::cout << "[DEBUG] exitCode: "   << exitCode   << std::endl;

    result.put("usedTime", usedTime);
    result.put("usedMemory", usedMemory);
    result.put("exitCode", exitCode);
    return result.toJObject(jniEnv);
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

/**
 * Applies kernel-enforced resource limits to the calling (child) process.
 * Note: memory is intentionally NOT capped through RLIMIT_AS, because managed runtimes
 * (the JVM, Go, ...) reserve a huge virtual address space while using little physical
 * memory; MLE is enforced on the resident set size by the watchdog instead.
 * @param  timeLimit - the CPU time limit (ms, 0 means no limit)
 */
static void applyResourceLimits(int timeLimit) {
    struct rlimit limit;

    // CPU time: a coarse (whole-second) kernel backstop; the watchdog enforces ms precision.
    if ( timeLimit > 0 ) {
        rlim_t seconds = (rlim_t) ((timeLimit + 999) / 1000) + 1;
        limit.rlim_cur = seconds;
        limit.rlim_max = seconds + 1;
        setrlimit(RLIMIT_CPU, &limit);
    }

    // Output file size: guard against output bombs.
    limit.rlim_cur = OUTPUT_FILE_SIZE_LIMIT;
    limit.rlim_max = OUTPUT_FILE_SIZE_LIMIT;
    setrlimit(RLIMIT_FSIZE, &limit);

    // Process count: guard against fork bombs (effective once dropped to the sandbox user).
    limit.rlim_cur = PROCESS_NUMBER_LIMIT;
    limit.rlim_max = PROCESS_NUMBER_LIMIT;
    setrlimit(RLIMIT_NPROC, &limit);
}

/**
 * Drops privileges to the given unprivileged user.
 *
 * When the judger is not running as root there is nothing to drop, and the submission simply
 * runs as the (already unprivileged) current user — this is the normal development and CI path.
 *
 * When the judger IS running as root, dropping is mandatory and fails closed: any problem
 * (no user configured, user not found, the configured user is root itself, or a failed
 * setgid/setuid) aborts the child so that untrusted code is NEVER executed with root privileges.
 *
 * @param  username - the unprivileged user to switch to
 */
static void dropPrivileges(const std::string& username) {
    if ( geteuid() != 0 ) {
        return;
    }
    if ( username.empty() ) {
        _exit(127);
    }
    struct passwd* pw = getpwnam(username.c_str());
    if ( pw == nullptr || pw->pw_uid == 0 ) {
        _exit(127);
    }
    if ( setgid(pw->pw_gid) != 0 ) {
        _exit(127);
    }
    initgroups(username.c_str(), pw->pw_gid);
    if ( setuid(pw->pw_uid) != 0 ) {
        _exit(127);
    }
}

/**
 * Installs a seccomp syscall filter (best-effort). Uses a default-allow policy and denies a
 * curated set of syscalls that a competitive-programming submission never legitimately needs
 * but that are useful for sandbox escape, host tampering or persistence. Rules are added by
 * syscall name through libseccomp, so the filter is portable across architectures (amd64/arm64).
 * Denied calls return EPERM rather than killing the process, to avoid spurious runtime errors.
 */
static void applySeccompFilter() {
    scmp_filter_ctx ctx = seccomp_init(SCMP_ACT_ALLOW);
    if ( ctx == nullptr ) {
        return;
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
    seccomp_load(ctx);
    seccomp_release(ctx);
}
