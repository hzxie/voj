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
#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0600
#endif

#include <cstdint>
#include <cwchar>
#include <iostream>
#include <map>
#include <sstream>
#include <string>
#include <vector>

#include <windows.h>
#include <aclapi.h>
#include <sddl.h>
#include <userenv.h>
#include <psapi.h>

#include "../org_verwandlung_voj_jni_hashmap.h"
#include "../org_verwandlung_voj_jni_library.h"
#include "../org_verwandlung_voj_judger_core_Runner.h"

/**
 * The default maximum number of processes a compiler may have running at once, used when the
 * caller does not pass one. This is enforced by the Job Object and primarily blocks fork bombs; it
 * must stay high enough for the compiler's helper-process chain (cc1, as, ld, ...).
 */
#define ACTIVE_PROCESS_LIMIT 64

/**
 * The maximum number of processes a run of the submission may have in its Job: a submission may not
 * create processes (as on Linux), but its console host counts too. CreateProcessWithLogonW always
 * gives the new process a console of its own (CREATE_NEW_CONSOLE is implied), and since Windows 8
 * the console host (conhost.exe) is started by the console program itself while it initialises -
 * after it has been assigned to the Job - so it becomes a member of the Job. With a limit of 1 the
 * console host could not start and every console program would fail to initialise
 * (STATUS_DLL_INIT_FAILED). With 2, the program plus its console host fill the Job, and any
 * CreateProcess of the submission fails. (Should the console host not be in the Job, the program
 * could have one child process at a time, still confined to the Job.)
 * See https://learn.microsoft.com/windows/win32/api/winbase/nf-winbase-createprocesswithlogonw
 */
#define RUN_ACTIVE_PROCESS_LIMIT 2

/**
 * Extra wall-clock time (ms) granted on top of the CPU time limit, so a process that blocks/sleeps
 * without consuming CPU is still terminated.
 */
#define WALL_TIME_BUFFER_MS 1000

/**
 * How often (ms) the parent checks the child's CPU time and peak memory while it runs. The memory
 * limit is enforced by polling the program's peak working set (a soft limit), so that an offending
 * peak rises just above the limit before the process is killed and the run is classified as MLE -
 * matching the Linux runner.
 */
#define POLL_INTERVAL_MS 2

/**
 * The Job's hard memory (commit) limit, the counterpart of the Linux runner's RLIMIT_DATA backstop:
 * twice the memory limit plus a headroom of max(256 MB, physical RAM / 32). Commit is deliberately
 * not what the memory limit is measured against (that is the peak working set, see
 * getPeakMemoryUsage), and the cap is generous for the same reason: the JVM commits its initial
 * heap, physical RAM / 64 by default, as it starts, so a Java "hello world" on a 64 GB host commits
 * over 1 GB while it touches a few dozen MB. The cap bounds how far a program can grow between two
 * polls, and it is the only bound on the memory of a program that keeps its own working set below
 * the limit (see getPeakMemoryUsage): commit is the one measure the program cannot lower. It does
 * not kill the program: an allocation that would exceed it fails, and the Job posts
 * JOB_OBJECT_MSG_(JOB|PROCESS)_MEMORY_LIMIT to its completion port, which the runner treats as
 * Memory Limit Exceeded (otherwise the failed allocation would surface as a crash, indistinguishable
 * from a runtime error, or be survived by a program that checks for it), like the Linux runner's
 * seccomp rule for a single mapping beyond RLIMIT_DATA.
 * See https://learn.microsoft.com/windows/win32/api/winnt/ns-winnt-jobobject_extended_limit_information
 * and https://learn.microsoft.com/windows/win32/api/winnt/ns-winnt-jobobject_associate_completion_port
 */
static const unsigned long long MEMORY_HARD_LIMIT_MIN_HEADROOM_KB = 256ULL * 1024;
static const unsigned long long MEMORY_HARD_LIMIT_RAM_DIVISOR     = 32;

/**
 * The values of the result's "limitExceeded" key: the limit the program was killed for, or was found
 * to have exceeded once it exited (Runner.java maps it to the verdict). A program that exceeded a
 * limit is never reported with exit code 0.
 */
static const jint LIMIT_NONE   = 0;
static const jint LIMIT_TIME   = 1;
static const jint LIMIT_MEMORY = 2;
static const jint LIMIT_OUTPUT = 3;

/**
 * The largest output file (bytes) a program may write: 256 MB, the Linux runner's RLIMIT_FSIZE.
 * Windows has no per-process file size limit, so the size of the output file is polled with the
 * other limits and the run is killed once it is exceeded.
 */
static const LONGLONG OUTPUT_FILE_SIZE_LIMIT = 256LL * 1024 * 1024;

/**
 * The only environment variables the sandboxed process inherits from the judger, mirroring the
 * Linux runner. The judger's own environment holds secrets (VOJ_DB_PASSWORD, VOJ_JUDGER_API_TOKEN,
 * ...) that a submission could otherwise read (GetEnvironmentVariable, or a compile error that
 * echoes it into the compile log), so the sandbox gets the system environment (see
 * buildEnvironmentBlock) plus only the locale and toolchain settings that compilers and runtimes
 * genuinely need. PATH is the
 * judger's because that is where the toolchains are found (e.g. a MinGW bin directory added to
 * PATH only for the judger, as on AppVeyor).
 */
static const wchar_t* const INHERITED_ENVIRONMENT_VARIABLES[] = {
    L"PATH", L"LANG", L"LC_ALL", L"TZ",
    L"JAVA_HOME", L"KOTLIN_HOME",
    L"GOROOT", L"GOENV", L"GOTOOLCHAIN", L"GOFLAGS",
    L"RUSTUP_HOME", L"RUSTUP_TOOLCHAIN", L"CARGO_HOME",
};
/**
 * Variables without which Windows programs misbehave (Winsock and the CRT need SystemRoot, cmd
 * needs ComSpec and PATHEXT). They are system variables, which CreateEnvironmentBlock always puts
 * in the system environment; the judger's values are only a fallback for a block that somehow
 * lacks one. None of them is a secret.
 */
static const wchar_t* const ESSENTIAL_ENVIRONMENT_VARIABLES[] = {
    L"SystemRoot", L"SystemDrive", L"windir", L"ComSpec", L"PATHEXT",
};
/**
 * The variables a JVM reads its options from (the java launcher's JDK_JAVA_OPTIONS, and the JVM's
 * own JAVA_TOOL_OPTIONS and _JAVA_OPTIONS), which are never taken from the system environment: as
 * on Linux, where the sandbox gets none of them, a run gets JDK_JAVA_OPTIONS from the judger only,
 * and a compilation none.
 */
static const wchar_t* const JVM_OPTIONS_VARIABLES[] = {
    L"JDK_JAVA_OPTIONS", L"JAVA_TOOL_OPTIONS", L"_JAVA_OPTIONS",
};
/**
 * The Go toolchain's writable state, which must never be shared between submissions: a build
 * cache that one submission can write is one it can poison for every later Go build. As on Linux
 * (where the sandbox user is always a different user too), both live in the submission's own work
 * directory instead of the sandbox user's profile. Must match Preprocessor.GO_BUILD_CACHE_DIRECTORY.
 */
static const wchar_t* const GO_BUILD_CACHE_DIRECTORY = L"/.cache/go-build"; // GOCACHE (work dir relative)
static const wchar_t* const GO_PATH_DIRECTORY        = L"/go";              // GOPATH (work dir relative)

/**
 * The integrity level of every compilation and run: Low (S-1-16-4096), the counterpart of the
 * Linux runner's Landlock rules. A process may not write to an object labelled with a higher
 * integrity level than its own (the "no write up" policy every object has), whatever its DACL
 * grants, and an object without a label is Medium. So a Low process of the sandbox account cannot
 * write to the account's registry (HKCU, such as HKCU\Environment) and profile (such as the
 * Python user site), nor to C:\Users\Public or the other directories every account may write
 * to, but only to what is labelled Low: its work directory (see WORK_DIRECTORY_LABEL), and what
 * Windows keeps for Low processes, the account's own %USERPROFILE%\AppData\LocalLow (and
 * HKCU\Software\AppDataLow, once something created it). Reading is not restricted (files have
 * no "no read up" policy). Nor can it open the processes of the account that are not Low, or
 * send messages to their windows (UIPI).
 * See https://learn.microsoft.com/windows/win32/secauthz/mandatory-integrity-control
 */
static const wchar_t* const LOW_INTEGRITY_SID = L"S-1-16-4096";
/**
 * The label the work directory of a compilation or run gets so that a Low process may write to it
 * (only the DACL decides then): Low, with the "no write up" policy, inherited by what is created in
 * it (OI, CI), as the files the judger creates there and the program's own. A compilation's work
 * directory is the program directory, which the judger takes once the program is compiled (see
 * Preprocessor.java): its ACL then lets the sandbox account read it only. A run's is a fresh
 * directory of its own, so no run can change what a later one uses.
 */
static const wchar_t* const WORK_DIRECTORY_LABEL = L"S:(ML;OICI;NW;;;LW)";
/**
 * The variables that tell a program where the account's profile is. The system environment has
 * the Default profile's (and USERNAME is SYSTEM's), which are not the account's; when the work
 * directory is known they point into it instead, as HOME on Linux: it is the only place a program
 * can write to.
 */
static const wchar_t* const PROFILE_ENVIRONMENT_VARIABLES[] = {
    L"USERPROFILE", L"HOME", L"APPDATA", L"LOCALAPPDATA",
};
/**
 * What the sandbox account needs of the judger's window station and desktop for its programs to
 * start: user32 connects to them as a console program starts (a JVM's does, whoami's does), and
 * without this access it cannot initialise (STATUS_DLL_INIT_FAILED, which Windows reports in a
 * hard-error box the program then waits on). CreateProcessWithLogonW granted it to the logon of
 * each run; CreateProcessWithTokenW does not, so the runner grants it to the account (see
 * grantDesktopAccess). Neither lets a program read the screen, switch desktops, hook the input of
 * other programs, or create desktops; the Job's UI restrictions apply too (see createJobObject).
 */
static const DWORD WINDOW_STATION_ACCESS = READ_CONTROL | WINSTA_ENUMDESKTOPS | WINSTA_READATTRIBUTES |
                                           WINSTA_ACCESSGLOBALATOMS | WINSTA_EXITWINDOWS;
static const DWORD DESKTOP_ACCESS        = DESKTOP_READOBJECTS | DESKTOP_CREATEWINDOW |
                                           DESKTOP_CREATEMENU | DESKTOP_WRITEOBJECTS;

/**
 * Orders environment variable names the way Windows expects an environment block to be sorted:
 * case-insensitively, in Unicode (ordinal) order, without regard to locale. Names that differ only
 * in case (Path, PATH) are the same variable.
 * See https://learn.microsoft.com/windows/win32/procthread/changing-environment-variables
 */
struct EnvironmentNameLess {
    bool operator()(const std::wstring& a, const std::wstring& b) const {
        return CompareStringOrdinal(a.c_str(), (int) a.size(), b.c_str(), (int) b.size(), TRUE) == CSTR_LESS_THAN;
    }
};
typedef std::map<std::wstring, std::wstring, EnvironmentNameLess> EnvironmentMap;

/**
 * Function Prototypes.
 */
bool setupIoRedirection(const std::wstring&, const std::wstring&, HANDLE&, HANDLE&);
bool getJudgerOnlySecurityDescriptor(PSECURITY_DESCRIPTOR&);
void setupStartupInfo(STARTUPINFOW&, HANDLE&, HANDLE&, HANDLE&);
HANDLE createJobObject(DWORD, jint, bool, HANDLE);
bool drainJobMessages(HANDLE, bool&);
bool createProcess(const std::wstring&, const std::wstring&, const std::wstring&, const std::wstring&, const std::wstring&, HANDLE&, HANDLE&, STARTUPINFOW&, PROCESS_INFORMATION&, std::string&);
HANDLE createLowIntegrityToken(HANDLE);
bool labelLowIntegrity(const std::wstring&);
bool hasLowIntegrityLabel(PACL, PSID);
bool grantDesktopAccess(HANDLE);
bool grantAccess(HANDLE, PSID, DWORD);
bool buildEnvironmentBlock(const std::wstring&, const std::wstring&, const std::wstring&, std::vector<wchar_t>&);
bool getJudgerEnvironmentVariable(const wchar_t*, std::wstring&);

bool getPeakMemoryUsage(HANDLE& hProcess, jint& memory);
bool getCpuTimeUsage(HANDLE& hJob, HANDLE& hProcess, bool includeChildren, jint& cpuTime);
unsigned long long fileTimeToUll(const FILETIME&);
std::string getErrorMessage(const std::string&);
std::wstring getWideStringValue(JNIEnv*, jstring);

/**
 * JNI call entry point.
 * Gets the program's runtime result.
 * @param  jniEnv          - a reference to the JNI runtime environment
 * @param  selfReference   - a reference to the calling Java object
 * @param  jCommandLine    - the command line to execute
 * @param  jUsername       - the Windows username
 * @param  jPassword       - the Windows password
 * @param  jInputFilePath  - the input file path when running the program (may be NULL)
 * @param  jOutputFilePath - the file receiving stdout (may be NULL); for a compilation it also
 *                           receives stderr, otherwise stderr is discarded. Created by the judger
 *                           (see setupIoRedirection): it must be in a directory the program may
 *                           not write to
 * @param  jWorkDirectory  - the directory the program starts in, and the only one it may write to
 *                           (may be NULL: the sandbox account's profile directory, and none)
 * @param  timeLimit       - the program execution time limit (ms, 0 means no limit)
 * @param  memoryLimit     - the program execution memory limit (KB, 0 means no limit)
 * @param  isCompilation   - whether this is a compilation (as opposed to a run of the submission)
 * @param  processLimit    - the maximum number of active processes of a compilation (0 means the
 *                           default); a run may not create processes (RUN_ACTIVE_PROCESS_LIMIT)
 * @param  jJavaOptions    - the options of a JVM the program starts, given to it as JDK_JAVA_OPTIONS
 *                           (may be NULL or empty: not set); see JavaOptions.java
 * @return a Map<String, Object> object containing the runtime result
 */
JNIEXPORT jobject JNICALL Java_org_verwandlung_voj_judger_core_Runner_getRuntimeResult(
    JNIEnv* jniEnv, jobject selfReference, jstring jCommandLine, jstring jUsername,
    jstring jPassword, jstring jInputFilePath, jstring jOutputFilePath, jstring jWorkDirectory,
    jint timeLimit, jint memoryLimit, jboolean isCompilation, jint processLimit,
    jstring jJavaOptions) {
    std::wstring        commandLine         = getWideStringValue(jniEnv, jCommandLine);
    std::wstring        username            = getWideStringValue(jniEnv, jUsername);
    std::wstring        password            = getWideStringValue(jniEnv, jPassword);
    std::wstring        inputFilePath       = getWideStringValue(jniEnv, jInputFilePath);
    std::wstring        outputFilePath      = getWideStringValue(jniEnv, jOutputFilePath);
    std::wstring        workDirectory       = getWideStringValue(jniEnv, jWorkDirectory);
    std::wstring        javaOptions         = getWideStringValue(jniEnv, jJavaOptions);

    HANDLE              hInput              = NULL;
    HANDLE              hOutput             = NULL;
    HANDLE              hError              = NULL;
    HANDLE              hToken              = NULL;
    HANDLE              hProfile            = NULL;
    HANDLE              hJob                = NULL;
    HANDLE              hCompletionPort     = NULL;
    PROCESS_INFORMATION processInfo         = {0};
    STARTUPINFOW        startupInfo         = {0};

    JHashMap            result;
    jint                timeUsage           = 0;
    jint                memoryUsage         = 0;
    DWORD               exitCode            = 127;

    // Releases everything acquired so far. Every early exit below goes through it: a pending Java
    // exception does not stop this function, so each failure must clean up and return explicitly
    // instead of carrying on with invalid handles.
    auto cleanup = [&]() {
        if ( processInfo.hThread != NULL ) {
            CloseHandle(processInfo.hThread);
            processInfo.hThread = NULL;
        }
        if ( processInfo.hProcess != NULL ) {
            CloseHandle(processInfo.hProcess);
            processInfo.hProcess = NULL;
        }
        // Closing the job kills any surviving processes in it (JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE).
        if ( hJob != NULL ) {
            CloseHandle(hJob);
            hJob = NULL;
        }
        if ( hCompletionPort != NULL ) {
            CloseHandle(hCompletionPort);
            hCompletionPort = NULL;
        }
        if ( hInput != NULL ) {
            CloseHandle(hInput);
            hInput = NULL;
        }
        if ( hError != NULL && hError != hOutput ) {
            CloseHandle(hError);
        }
        hError = NULL;
        if ( hOutput != NULL ) {
            CloseHandle(hOutput);
            hOutput = NULL;
        }
        if ( hProfile != NULL ) {
            UnloadUserProfile(hToken, hProfile);
            hProfile = NULL;
        }
        if ( hToken != NULL ) {
            CloseHandle(hToken);
            hToken = NULL;
        }
    };
    // Reports a failed Windows API call as a Java exception and releases everything. The error
    // message is read before cleaning up, which would overwrite GetLastError().
    auto fail = [&](const std::string& apiName) -> jobject {
        std::string message = getErrorMessage(apiName);
        cleanup();
        throwStringException(jniEnv, message);
        return NULL;
    };

    if ( !setupIoRedirection(inputFilePath, outputFilePath, hInput, hOutput) ) {
        return fail("SetupIoRedirection");
    }
    // A compilation's diagnostics (stderr) are the compile log, but a submission's stderr must never
    // reach the judged output: discard it.
    if ( isCompilation ) {
        hError = hOutput;
    } else {
        SECURITY_ATTRIBUTES nulSa = { sizeof(SECURITY_ATTRIBUTES), NULL, TRUE };
        hError = CreateFileW(L"NUL", GENERIC_WRITE, FILE_SHARE_READ | FILE_SHARE_WRITE, &nulSa,
                    OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
        if ( hError == INVALID_HANDLE_VALUE ) {
            hError = NULL;
        }
    }
    setupStartupInfo(startupInfo, hInput, hOutput, hError);

    // The Job's notifications (a refused allocation, see MEMORY_HARD_LIMIT_MIN_HEADROOM_KB) arrive on
    // this completion port, which the poll loop drains.
    hCompletionPort = CreateIoCompletionPort(INVALID_HANDLE_VALUE, NULL, 0, 1);
    if ( hCompletionPort == NULL ) {
        return fail("CreateIoCompletionPort");
    }
    // The Job Object enforces the active-process limit (anti fork bomb), a hard memory cap above the
    // memory limit and the UI restrictions, and kills the whole process tree atomically when it is
    // closed. The memory limit itself is enforced below by polling the program's peak working set (a
    // soft limit). Without a Job none of this holds, so the run is refused rather than started
    // unconfined. A run of the submission may not create processes; a compiler may (cc1, as, ld,
    // ...), so the hard cap applies to a run's whole Job and to each process of a compilation.
    DWORD activeProcessLimit = isCompilation
                             ? ( processLimit > 0 ? (DWORD) processLimit : ACTIVE_PROCESS_LIMIT )
                             : RUN_ACTIVE_PROCESS_LIMIT;
    hJob = createJobObject(activeProcessLimit, memoryLimit, !isCompilation, hCompletionPort);
    if ( hJob == NULL ) {
        return fail("CreateJobObject");
    }

    // The program runs at Low integrity (see LOW_INTEGRITY_SID): its work directory is the one
    // place it may write to.
    if ( !workDirectory.empty() && !labelLowIntegrity(workDirectory) ) {
        return fail("LabelLowIntegrity");
    }
    // The process is created suspended, assigned to the Job Object before it runs a single
    // instruction, and only then resumed - so the limits are in force from the very beginning.
    std::string failedApi;
    if ( !createProcess(commandLine, username, password, workDirectory, javaOptions,
            hToken, hProfile, startupInfo, processInfo, failedApi) ) {
        return fail(failedApi);
    }
    // If the assignment fails the process would run outside the Job: without the process limit,
    // without the kill-on-close guarantee, and with the Job reporting no memory at all (a memory
    // bomb would be judged Accepted). Kill it while it is still suspended and refuse the run.
    if ( !AssignProcessToJobObject(hJob, processInfo.hProcess) ) {
        std::string message = getErrorMessage("AssignProcessToJobObject");
        TerminateProcess(processInfo.hProcess, 1);
        WaitForSingleObject(processInfo.hProcess, INFINITE);
        cleanup();
        throwStringException(jniEnv, message);
        return NULL;
    }
    if ( ResumeThread(processInfo.hThread) == (DWORD) -1 ) {
        std::string message = getErrorMessage("ResumeThread");
        TerminateJobObject(hJob, 1);
        WaitForSingleObject(processInfo.hProcess, INFINITE);
        cleanup();
        throwStringException(jniEnv, message);
        return NULL;
    }

    // Kills every process of the run and waits for the program to be gone. If the Job cannot be
    // terminated, the program itself is; failing that, the run is aborted (closing the Job in
    // cleanup() still kills what is left, thanks to JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE).
    auto killRun = [&]() -> bool {
        if ( !TerminateJobObject(hJob, 1) && !TerminateProcess(processInfo.hProcess, 1) ) {
            return false;
        }
        return WaitForSingleObject(processInfo.hProcess, INFINITE) == WAIT_OBJECT_0;
    };

    // Poll while the child runs, and kill the whole Job as soon as a limit is exceeded:
    //  - CPU time, measured exactly as it is reported (see getCpuTimeUsage). The wall-clock guard
    //    alone would let a program use up to timeLimit + WALL_TIME_BUFFER_MS of CPU and, if it
    //    exits 0 in that window, be judged on its output;
    //  - memory (the peak working set), as a soft limit: the peak may rise just above the limit
    //    before the kill, so that the run is classified as MLE (matching the Linux runner). An
    //    allocation refused by the Job's hard cap is reported on the completion port and is a Memory
    //    Limit Exceeded too;
    //  - the size of the output file (see OUTPUT_FILE_SIZE_LIMIT);
    //  - wall-clock time, a guard for a process that blocks or sleeps while using little CPU.
    DWORD     wallTimeLimit       = ( timeLimit > 0 ) ? (DWORD)(timeLimit + WALL_TIME_BUFFER_MS) : INFINITE;
    jint      limitExceeded       = LIMIT_NONE;
    bool      killedByWallTimeout = false;
    bool      memoryLimitHit      = false;
    LARGE_INTEGER outputSize      = {};
    ULONGLONG startTick           = GetTickCount64();
    for ( ;; ) {
        DWORD waitResult = WaitForSingleObject(processInfo.hProcess, POLL_INTERVAL_MS);
        if ( waitResult == WAIT_OBJECT_0 ) {
            break; // the process exited on its own
        }
        if ( waitResult == WAIT_FAILED ) {
            // Never spin on a handle that cannot be waited on: kill the run and report the error.
            std::string message = getErrorMessage("WaitForSingleObject");
            TerminateJobObject(hJob, 1);
            cleanup();
            throwStringException(jniEnv, message);
            return NULL;
        }
        if ( timeLimit > 0 ) {
            if ( !getCpuTimeUsage(hJob, processInfo.hProcess, !isCompilation, timeUsage) ) {
                return fail("GetCpuTimeUsage");
            }
            if ( timeUsage > timeLimit ) {
                limitExceeded = LIMIT_TIME;
            }
        }
        // Drained on every pass, so the messages of a compiler's many processes do not pile up.
        if ( !drainJobMessages(hCompletionPort, memoryLimitHit) ) {
            return fail("GetQueuedCompletionStatus");
        }
        if ( limitExceeded == LIMIT_NONE && memoryLimit > 0 ) {
            if ( !getPeakMemoryUsage(processInfo.hProcess, memoryUsage) ) {
                return fail("GetPeakMemoryUsage");
            }
            if ( memoryLimitHit || memoryUsage > memoryLimit ) {
                limitExceeded = LIMIT_MEMORY;
            }
        }
        if ( limitExceeded == LIMIT_NONE && hOutput != NULL ) {
            if ( !GetFileSizeEx(hOutput, &outputSize) ) {
                return fail("GetFileSizeEx");
            }
            if ( outputSize.QuadPart > OUTPUT_FILE_SIZE_LIMIT ) {
                limitExceeded = LIMIT_OUTPUT;
            }
        }
        if ( limitExceeded == LIMIT_NONE && wallTimeLimit != INFINITE &&
                GetTickCount64() - startTick > (ULONGLONG) wallTimeLimit ) {
            limitExceeded       = LIMIT_TIME;
            killedByWallTimeout = true;
        }
        if ( limitExceeded != LIMIT_NONE ) {
            if ( !killRun() ) {
                return fail("TerminateJobObject");
            }
            break;
        }
    }

    if ( !GetExitCodeProcess(processInfo.hProcess, &exitCode) ) {
        return fail("GetExitCodeProcess");
    }
    // A run is charged for the CPU of every process it started; a compilation is not, because build
    // tools legitimately run several processes in parallel (matching the Linux runner).
    if ( !getCpuTimeUsage(hJob, processInfo.hProcess, !isCompilation, timeUsage) ) {
        return fail("GetCpuTimeUsage");
    }
    // The process's peak accounting persists after it exits (its handle is still open). A refused
    // allocation is posted to the port before the allocating call returns, so a program that exits
    // after one has its message queued by now.
    if ( !getPeakMemoryUsage(processInfo.hProcess, memoryUsage) ) {
        return fail("GetPeakMemoryUsage");
    }
    if ( !drainJobMessages(hCompletionPort, memoryLimitHit) ) {
        return fail("GetQueuedCompletionStatus");
    }
    if ( hOutput != NULL && !GetFileSizeEx(hOutput, &outputSize) ) {
        return fail("GetFileSizeEx");
    }

    // The program may have exceeded a limit after the last poll and exited before the next one
    // (CPU time and memory accounting are only sampled): judge the final figures too.
    if ( limitExceeded == LIMIT_NONE ) {
        if ( timeLimit > 0 && timeUsage > timeLimit ) {
            limitExceeded = LIMIT_TIME;
        } else if ( memoryLimit > 0 && (memoryLimitHit || memoryUsage > memoryLimit) ) {
            limitExceeded = LIMIT_MEMORY;
        } else if ( hOutput != NULL && outputSize.QuadPart > OUTPUT_FILE_SIZE_LIMIT ) {
            limitExceeded = LIMIT_OUTPUT;
        }
    }
    // A process killed by the wall-clock guard used little CPU: report at least the limit, so that
    // the time shown matches the verdict.
    if ( killedByWallTimeout && timeUsage < timeLimit ) {
        timeUsage = timeLimit;
    }
    // A process killed for exceeding the memory limit must report at least the limit.
    if ( limitExceeded == LIMIT_MEMORY && memoryUsage < memoryLimit ) {
        memoryUsage = memoryLimit;
    }
    // A program that exceeded a limit must never be reported as a clean exit (matching Linux).
    if ( limitExceeded != LIMIT_NONE && exitCode == 0 ) {
        exitCode = 1;
    }

    cleanup();

    result.put("usedTime", timeUsage);
    result.put("usedMemory", memoryUsage);
    result.put("exitCode", (jint)exitCode);
    result.put("limitExceeded", limitExceeded);

    return result.toJObject(jniEnv);
}

/**
 * Redirects the child process's I/O.
 * @param inputFilePath  - the input file path
 * @param outputFilePath - the output file path
 * @param hInput         - the input file handle (output parameter)
 * @param hOutput        - the output file handle (output parameter)
 * @return whether the redirection was set up successfully
 */
bool setupIoRedirection(const std::wstring& inputFilePath, const std::wstring& outputFilePath,
        HANDLE& hInput, HANDLE& hOutput) {
    SECURITY_ATTRIBUTES sa;
    sa.nLength                  = sizeof(sa);
    sa.lpSecurityDescriptor     = NULL;
    sa.bInheritHandle           = TRUE;

    if ( !inputFilePath.empty() ) {
        hInput = CreateFileW(inputFilePath.c_str(), GENERIC_READ, FILE_SHARE_READ, &sa,
                    OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
        if ( hInput == INVALID_HANDLE_VALUE )  {
            hInput = NULL;
            return false;
        }
    }
    if ( !outputFilePath.empty() ) {
        // The output file is in the directory of the judging, which no compilation or run may write
        // to (see Preprocessor.java), so nothing can have been planted at its path. As defence in
        // depth, whatever is at the path is removed first (DeleteFileW removes a link, never its
        // target) and the file is created with CREATE_NEW, which fails rather than open anything
        // that is still, or again, at the path; FILE_FLAG_OPEN_REPARSE_POINT never follows a reparse
        // point. A failed deletion is not an error of its own: CREATE_NEW then fails, and the run
        // is refused. The file gets an ACL of its own that allows the judger alone (rather than the
        // one it would inherit, which lets the sandbox account read it): a later run cannot read
        // it. FILE_READ_ATTRIBUTES lets the runner poll the file's size through this handle
        // (GetFileSizeEx needs it, and GENERIC_WRITE does not include it). The child inherits the
        // handle as its stdout, which only lets it read the attributes of its own output file.
        PSECURITY_DESCRIPTOR pSecurityDescriptor = NULL;
        if ( !getJudgerOnlySecurityDescriptor(pSecurityDescriptor) ) {
            return false;
        }
        SECURITY_ATTRIBUTES outputSa = { sizeof(SECURITY_ATTRIBUTES), pSecurityDescriptor, TRUE };
        DeleteFileW(outputFilePath.c_str());
        hOutput = CreateFileW(outputFilePath.c_str(), GENERIC_WRITE | FILE_READ_ATTRIBUTES, FILE_SHARE_READ,
                    &outputSa, CREATE_NEW, FILE_ATTRIBUTE_NORMAL | FILE_FLAG_OPEN_REPARSE_POINT, NULL);
        DWORD error = GetLastError();
        LocalFree(pSecurityDescriptor);
        if ( hOutput == INVALID_HANDLE_VALUE )  {
            hOutput = NULL;
            SetLastError(error);
            return false;
        }
    }
    return true;
}

/**
 * Builds a security descriptor whose (protected) DACL gives the user of the judger's process full
 * control, and nobody else any access.
 * @param  pSecurityDescriptor - receives the security descriptor, to be freed with LocalFree
 * @return whether it was built (GetLastError() tells why not)
 */
bool getJudgerOnlySecurityDescriptor(PSECURITY_DESCRIPTOR& pSecurityDescriptor) {
    HANDLE hToken = NULL;
    if ( !OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY, &hToken) ) {
        return false;
    }
    BYTE  tokenUser[sizeof(TOKEN_USER) + SECURITY_MAX_SID_SIZE];
    DWORD size    = 0;
    BOOL  queried = GetTokenInformation(hToken, TokenUser, tokenUser, sizeof(tokenUser), &size);
    DWORD error   = GetLastError();
    CloseHandle(hToken);
    LPWSTR sid = NULL;
    if ( !queried || !ConvertSidToStringSidW(((PTOKEN_USER) tokenUser)->User.Sid, &sid) ) {
        SetLastError(queried ? GetLastError() : error);
        return false;
    }
    std::wstring sddl = std::wstring(L"D:P(A;;FA;;;") + sid + L")";
    LocalFree(sid);
    return ConvertStringSecurityDescriptorToSecurityDescriptorW(sddl.c_str(), SDDL_REVISION_1,
               &pSecurityDescriptor, NULL) != FALSE;
}

/**
 * Reconfigures startupInfo based on the I/O redirection information.
 * @param startupInfo - the STARTUPINFOW structure
 * @param hInput      - the file input handle
 * @param hOutput     - the file output handle
 * @param hError      - the standard error handle
 */
void setupStartupInfo(STARTUPINFOW& startupInfo, HANDLE& hInput, HANDLE& hOutput, HANDLE& hError) {
    startupInfo.cb              = sizeof(STARTUPINFOW);
    startupInfo.dwFlags        |= STARTF_USESTDHANDLES;
    startupInfo.hStdInput       = hInput;
    startupInfo.hStdError       = hError;
    startupInfo.hStdOutput      = hOutput;
}

/**
 * Creates a Job Object that enforces the active-process limit, a hard memory cap and the UI
 * restrictions, reports its events to a completion port, and kills the whole tree when closed.
 * @param  activeProcessLimit - the maximum number of processes that may be active in the Job
 * @param  memoryLimit        - the memory limit (KB, 0 means none); the hard cap is set above it
 *                              (see MEMORY_HARD_LIMIT_MIN_HEADROOM_KB)
 * @param  jobWideMemory      - whether the cap applies to all processes together (a run) or to
 *                              each process (a compilation)
 * @param  hCompletionPort    - the completion port that receives the Job's messages
 * @return the Job Object handle, or NULL on failure (GetLastError() tells why)
 */
HANDLE createJobObject(DWORD activeProcessLimit, jint memoryLimit, bool jobWideMemory,
        HANDLE hCompletionPort) {
    HANDLE hJob = CreateJobObjectW(NULL, NULL);
    if ( hJob == NULL ) {
        return NULL;
    }
    // Closes the Job (nothing runs in it yet) while keeping the error of the call that failed.
    auto failJob = [&]() -> HANDLE {
        DWORD errorCode = GetLastError();
        CloseHandle(hJob);
        SetLastError(errorCode);
        return NULL;
    };

    // Every message of the Job, among them JOB_OBJECT_MSG_(JOB|PROCESS)_MEMORY_LIMIT, is posted to
    // the port with the Job's handle as the completion key. Associated before any process joins.
    JOBOBJECT_ASSOCIATE_COMPLETION_PORT completionPort = {0};
    completionPort.CompletionKey  = hJob;
    completionPort.CompletionPort = hCompletionPort;
    if ( !SetInformationJobObject(hJob, JobObjectAssociateCompletionPortInformation,
            &completionPort, sizeof(completionPort)) ) {
        return failJob();
    }

    JOBOBJECT_EXTENDED_LIMIT_INFORMATION jeli = {0};
    jeli.BasicLimitInformation.LimitFlags =
        JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE | JOB_OBJECT_LIMIT_DIE_ON_UNHANDLED_EXCEPTION |
        JOB_OBJECT_LIMIT_ACTIVE_PROCESS;
    jeli.BasicLimitInformation.ActiveProcessLimit = activeProcessLimit;
    if ( memoryLimit > 0 ) {
        unsigned long long headroomKb = MEMORY_HARD_LIMIT_MIN_HEADROOM_KB;
        MEMORYSTATUSEX     memoryStatus;
        memoryStatus.dwLength = sizeof(memoryStatus);
        if ( GlobalMemoryStatusEx(&memoryStatus) &&
                memoryStatus.ullTotalPhys / 1024 / MEMORY_HARD_LIMIT_RAM_DIVISOR > headroomKb ) {
            headroomKb = memoryStatus.ullTotalPhys / 1024 / MEMORY_HARD_LIMIT_RAM_DIVISOR;
        }
        unsigned long long hardLimitBytes = (2ULL * (unsigned long long) memoryLimit + headroomKb) * 1024ULL;
        SIZE_T             hardLimit      = ( hardLimitBytes > (unsigned long long) SIZE_MAX )
                                          ? SIZE_MAX : (SIZE_T) hardLimitBytes;
        if ( jobWideMemory ) {
            jeli.BasicLimitInformation.LimitFlags |= JOB_OBJECT_LIMIT_JOB_MEMORY;
            jeli.JobMemoryLimit = hardLimit;
        } else {
            jeli.BasicLimitInformation.LimitFlags |= JOB_OBJECT_LIMIT_PROCESS_MEMORY;
            jeli.ProcessMemoryLimit = hardLimit;
        }
    }
    if ( !SetInformationJobObject(hJob, JobObjectExtendedLimitInformation, &jeli, sizeof(jeli)) ) {
        // Without these limits the Job would neither cap the process count nor kill the tree.
        return failJob();
    }

    // Cut the sandboxed program off from the interactive desktop (clipboard, global atoms, etc.).
    JOBOBJECT_BASIC_UI_RESTRICTIONS uiRestrictions = {0};
    uiRestrictions.UIRestrictionsClass =
        JOB_OBJECT_UILIMIT_DESKTOP | JOB_OBJECT_UILIMIT_DISPLAYSETTINGS |
        JOB_OBJECT_UILIMIT_EXITWINDOWS | JOB_OBJECT_UILIMIT_GLOBALATOMS |
        JOB_OBJECT_UILIMIT_READCLIPBOARD | JOB_OBJECT_UILIMIT_WRITECLIPBOARD |
        JOB_OBJECT_UILIMIT_SYSTEMPARAMETERS;
    if ( !SetInformationJobObject(hJob, JobObjectBasicUIRestrictions, &uiRestrictions, sizeof(uiRestrictions)) ) {
        return failJob();
    }

    return hJob;
}

/**
 * Takes every message the Job has posted so far off its completion port. With a zero timeout,
 * GetQueuedCompletionStatus returns FALSE with no packet (lpOverlapped NULL) and WAIT_TIMEOUT once
 * the queue is empty. For a Job message, lpNumberOfBytesTransferred carries the message identifier.
 * See https://learn.microsoft.com/windows/win32/api/winnt/ns-winnt-jobobject_associate_completion_port
 * @param  hCompletionPort - the Job's completion port
 * @param  memoryLimitHit  - set when an allocation was refused by the Job's hard memory cap
 * @return whether the port could be read (GetLastError() tells why not)
 */
bool drainJobMessages(HANDLE hCompletionPort, bool& memoryLimitHit) {
    for ( ;; ) {
        DWORD        message    = 0;
        ULONG_PTR    key        = 0;
        LPOVERLAPPED overlapped = NULL;
        if ( !GetQueuedCompletionStatus(hCompletionPort, &message, &key, &overlapped, 0) ) {
            return overlapped == NULL && GetLastError() == WAIT_TIMEOUT;
        }
        if ( message == JOB_OBJECT_MSG_JOB_MEMORY_LIMIT || message == JOB_OBJECT_MSG_PROCESS_MEMORY_LIMIT ) {
            memoryLimitHit = true;
        }
    }
}

/**
 * Creates a process running as the given (low-privilege) user, at Low integrity.
 *
 * The account is logged on (LogonUserW, as a batch job), its profile loaded, and the process
 * created with a copy of its token lowered to Low integrity (see LOW_INTEGRITY_SID) by
 * CreateProcessWithTokenW, which needs the judger to hold SeImpersonatePrivilege (an elevated
 * administrator does; CreateProcessAsUserW would need SeAssignPrimaryTokenPrivilege, which only
 * services have). CreateProcessWithLogonW, which needs no privilege, takes no token: the integrity
 * level of the process it creates cannot be lowered, as the judger may not change that process's
 * token. The process is created suspended so the caller can assign it to a Job Object before it
 * runs.
 *
 * @param  commandLine   - the command line to execute
 * @param  username      - the Windows username
 * @param  password      - the Windows password
 * @param  workDirectory - the program's work directory, where the process starts (empty if
 *                         none: the sandbox account's profile directory)
 * @param  javaOptions   - the value of JDK_JAVA_OPTIONS (empty: not set), see buildEnvironmentBlock
 * @param  hToken        - receives a token that represents the specified user
 * @param  hProfile      - receives the handle of the user's loaded profile (its registry), which
 *                         must be unloaded once the process is gone
 * @param  startupInfo   - a STARTUPINFO structure
 * @param  processInfo   - receives identification information for the new process
 * @param  failedApi     - receives the name of the call that failed, if one did
 * @return whether the process was created successfully (GetLastError() tells why not)
 */
bool createProcess(const std::wstring& commandLine, const std::wstring& username,
        const std::wstring& password, const std::wstring& workDirectory,
        const std::wstring& javaOptions, HANDLE& hToken, HANDLE& hProfile, STARTUPINFOW& startupInfo,
        PROCESS_INFORMATION& processInfo, std::string& failedApi) {
    WCHAR   szUserProfile[MAX_PATH] = L"";
    DWORD   dwSize                  = sizeof(szUserProfile) / sizeof(WCHAR);
    std::wstring         profileUsername = username;
    std::vector<wchar_t> environmentBlock;

    if ( !LogonUserW(username.c_str(), L".", password.c_str(), LOGON32_LOGON_BATCH,
            LOGON32_PROVIDER_DEFAULT, &hToken) ) {
        failedApi = "LogonUser";
        return false;
    }
    // The account's registry (HKCU) and profile, which programs expect (the JVM finds user.home
    // there). Loaded by the judger with the logon token, not by CreateProcessWithTokenW
    // (LOGON_WITH_PROFILE) with the Low one, which cannot create the profile of an account that
    // has none yet (ERROR_INVALID_STATE at its first run).
    PROFILEINFOW profileInfo = {0};
    profileInfo.dwSize     = sizeof(profileInfo);
    profileInfo.dwFlags    = PI_NOUI;
    profileInfo.lpUserName = &profileUsername[0];
    if ( !LoadUserProfileW(hToken, &profileInfo) ) {
        failedApi = "LoadUserProfile";
        return false;
    }
    hProfile = profileInfo.hProfile;
    if ( !buildEnvironmentBlock(username, workDirectory, javaOptions, environmentBlock) ) {
        failedApi = "CreateEnvironmentBlock";
        return false;
    }
    // The program starts in its own work directory, as on Linux, never in a directory that outlives
    // the submission or that every submission shares. The sandbox account can always enter it.
    std::wstring currentDirectory = workDirectory;
    for ( wchar_t& c : currentDirectory ) {
        if ( c == L'/' ) {
            c = L'\\';
        }
    }
    if ( currentDirectory.empty() ) {
        if ( !GetUserProfileDirectoryW(hToken, szUserProfile, &dwSize) ) {
            failedApi = "GetUserProfileDirectory";
            return false;
        }
        currentDirectory = szUserProfile;
    }
    if ( !grantDesktopAccess(hToken) ) {
        failedApi = "GrantDesktopAccess";
        return false;
    }
    HANDLE hLowToken = createLowIntegrityToken(hToken);
    if ( hLowToken == NULL ) {
        failedApi = "CreateLowIntegrityToken";
        return false;
    }
    // lpDesktop is NULL: the program gets the judger's window station and desktop.
    // CreateProcessWithTokenW may write to the command line, which is therefore copied.
    std::vector<wchar_t> mutableCommandLine(commandLine.begin(), commandLine.end());
    mutableCommandLine.push_back(L'\0');
    BOOL  created = CreateProcessWithTokenW(hLowToken, 0, NULL, mutableCommandLine.data(),
                        CREATE_UNICODE_ENVIRONMENT | CREATE_SUSPENDED | CREATE_NO_WINDOW,
                        environmentBlock.data(), currentDirectory.c_str(), &startupInfo, &processInfo);
    DWORD error   = GetLastError();
    CloseHandle(hLowToken);
    SetLastError(error);
    failedApi = "CreateProcessWithToken";
    return created;
}

/**
 * Creates a primary token for a process from the logon token of the sandbox account, at Low
 * integrity (see LOW_INTEGRITY_SID). A token's integrity level can be lowered by whoever may adjust
 * it, never raised.
 * See https://learn.microsoft.com/windows/win32/secauthz/mandatory-integrity-control
 * @param  hToken - the logon token of the sandbox account
 * @return the new token, or NULL on failure (GetLastError() tells why)
 */
HANDLE createLowIntegrityToken(HANDLE hToken) {
    // CreateProcessWithTokenW puts the token in the judger's session, which takes
    // TOKEN_ADJUST_SESSIONID besides the rights it documents (it fails with ERROR_ACCESS_DENIED
    // otherwise); TOKEN_ADJUST_DEFAULT is for the integrity level.
    HANDLE hLowToken = NULL;
    if ( !DuplicateTokenEx(hToken, TOKEN_QUERY | TOKEN_DUPLICATE | TOKEN_ASSIGN_PRIMARY |
            TOKEN_ADJUST_DEFAULT | TOKEN_ADJUST_SESSIONID,
            NULL, SecurityImpersonation, TokenPrimary, &hLowToken) ) {
        return NULL;
    }
    PSID lowSid = NULL;
    if ( !ConvertStringSidToSidW(LOW_INTEGRITY_SID, &lowSid) ) {
        DWORD error = GetLastError();
        CloseHandle(hLowToken);
        SetLastError(error);
        return NULL;
    }
    TOKEN_MANDATORY_LABEL label = {};
    label.Label.Attributes = SE_GROUP_INTEGRITY;
    label.Label.Sid        = lowSid;
    BOOL  lowered = SetTokenInformation(hLowToken, TokenIntegrityLevel, &label,
                        sizeof(label) + GetLengthSid(lowSid));
    DWORD error   = GetLastError();
    LocalFree(lowSid);
    if ( !lowered ) {
        CloseHandle(hLowToken);
        SetLastError(error);
        return NULL;
    }
    return hLowToken;
}

/**
 * Labels the work directory Low (see WORK_DIRECTORY_LABEL), so that a Low process may write to it,
 * unless it is already: the first compilation or run in a work directory labels it, before any
 * program of the submission has run in it, and the label is passed on to what the judger created
 * in it so far (the source). The program cannot change the label: only the owner of the directory
 * (the judger) may, and the sandbox account is not given that right (see
 * Preprocessor#createJudgingDirectory).
 * @param  directory - the work directory (the judger passes '/'-separated paths)
 * @return whether the directory is labelled Low (GetLastError() tells why not)
 */
bool labelLowIntegrity(const std::wstring& directory) {
    std::wstring path = directory;
    for ( wchar_t& c : path ) {
        if ( c == L'/' ) {
            c = L'\\';
        }
    }
    PSID lowSid = NULL;
    if ( !ConvertStringSidToSidW(LOW_INTEGRITY_SID, &lowSid) ) {
        return false;
    }
    PACL                 pSacl       = NULL;
    PSECURITY_DESCRIPTOR pCurrent    = NULL;
    DWORD                error       = GetNamedSecurityInfoW(path.c_str(), SE_FILE_OBJECT,
                                           LABEL_SECURITY_INFORMATION, NULL, NULL, NULL, &pSacl, &pCurrent);
    bool                 labelled    = error == ERROR_SUCCESS && hasLowIntegrityLabel(pSacl, lowSid);
    if ( pCurrent != NULL ) {
        LocalFree(pCurrent);
    }
    LocalFree(lowSid);
    if ( error != ERROR_SUCCESS ) {
        SetLastError(error);
        return false;
    }
    if ( labelled ) {
        return true;
    }
    PSECURITY_DESCRIPTOR pLabel = NULL;
    if ( !ConvertStringSecurityDescriptorToSecurityDescriptorW(WORK_DIRECTORY_LABEL, SDDL_REVISION_1,
            &pLabel, NULL) ) {
        return false;
    }
    BOOL present   = FALSE;
    BOOL defaulted = FALSE;
    pSacl = NULL;
    if ( !GetSecurityDescriptorSacl(pLabel, &present, &pSacl, &defaulted) ) {
        error = GetLastError();
    } else {
        error = SetNamedSecurityInfoW(&path[0], SE_FILE_OBJECT, LABEL_SECURITY_INFORMATION,
                    NULL, NULL, NULL, pSacl);
    }
    LocalFree(pLabel);
    SetLastError(error);
    return error == ERROR_SUCCESS;
}

/**
 * Checks whether the label of a directory is Low and passed on to what is created in it.
 * @param  pSacl  - the directory's SACL (its label; may be NULL: Medium)
 * @param  lowSid - the SID of the Low integrity level
 * @return whether the directory is labelled as WORK_DIRECTORY_LABEL does
 */
bool hasLowIntegrityLabel(PACL pSacl, PSID lowSid) {
    if ( pSacl == NULL ) {
        return false;
    }
    for ( DWORD i = 0; i < pSacl->AceCount; ++ i ) {
        PACE_HEADER ace = NULL;
        if ( !GetAce(pSacl, i, (LPVOID*) &ace) || ace->AceType != SYSTEM_MANDATORY_LABEL_ACE_TYPE ) {
            continue;
        }
        PSYSTEM_MANDATORY_LABEL_ACE label = (PSYSTEM_MANDATORY_LABEL_ACE) ace;
        BYTE inheritance = OBJECT_INHERIT_ACE | CONTAINER_INHERIT_ACE;
        if ( EqualSid((PSID) &label->SidStart, lowSid) && (ace->AceFlags & inheritance) == inheritance &&
                (ace->AceFlags & INHERIT_ONLY_ACE) == 0 ) {
            return true;
        }
    }
    return false;
}

/**
 * Grants the sandbox account the access to the judger's window station and desktop its programs
 * need to start (see WINDOW_STATION_ACCESS), unless it has it already. The grant is the account's,
 * so it is made once and stays (until the judger's session ends, which takes its window station
 * with it): the ACLs do not grow with every run, and judgers sharing the session grant the same.
 * @param  hToken - the logon token of the sandbox account
 * @return whether the account has that access (GetLastError() tells why not)
 */
bool grantDesktopAccess(HANDLE hToken) {
    BYTE  tokenUser[sizeof(TOKEN_USER) + SECURITY_MAX_SID_SIZE];
    DWORD size = 0;
    if ( !GetTokenInformation(hToken, TokenUser, tokenUser, sizeof(tokenUser), &size) ) {
        return false;
    }
    PSID    sid      = ((PTOKEN_USER) tokenUser)->User.Sid;
    HWINSTA hWinsta  = GetProcessWindowStation();
    HDESK   hDesktop = GetThreadDesktop(GetCurrentThreadId());
    if ( hWinsta == NULL || hDesktop == NULL ) {
        return false;
    }
    return grantAccess(hWinsta, sid, WINDOW_STATION_ACCESS) && grantAccess(hDesktop, sid, DESKTOP_ACCESS);
}

/**
 * Grants an account access to a window station or desktop, unless an entry of its DACL already
 * allows the account all of it.
 * @param  hObject - the window station or desktop
 * @param  sid     - the account
 * @param  access  - the access to grant
 * @return whether the account has that access (GetLastError() tells why not)
 */
bool grantAccess(HANDLE hObject, PSID sid, DWORD access) {
    PACL                 pDacl = NULL;
    PSECURITY_DESCRIPTOR pSd   = NULL;
    DWORD error = GetSecurityInfo(hObject, SE_WINDOW_OBJECT, DACL_SECURITY_INFORMATION, NULL, NULL,
                      &pDacl, NULL, &pSd);
    if ( error != ERROR_SUCCESS ) {
        SetLastError(error);
        return false;
    }
    // A NULL DACL allows everyone everything.
    bool granted = pDacl == NULL;
    for ( DWORD i = 0; !granted && i < pDacl->AceCount; ++ i ) {
        PACE_HEADER ace = NULL;
        if ( GetAce(pDacl, i, (LPVOID*) &ace) && ace->AceType == ACCESS_ALLOWED_ACE_TYPE &&
                (ace->AceFlags & INHERIT_ONLY_ACE) == 0 ) {
            PACCESS_ALLOWED_ACE allowed = (PACCESS_ALLOWED_ACE) ace;
            granted = EqualSid((PSID) &allowed->SidStart, sid) && (allowed->Mask & access) == access;
        }
    }
    if ( !granted ) {
        EXPLICIT_ACCESSW entry = {};
        entry.grfAccessPermissions = access;
        entry.grfAccessMode        = GRANT_ACCESS;
        entry.grfInheritance       = NO_INHERITANCE;
        entry.Trustee.TrusteeForm  = TRUSTEE_IS_SID;
        entry.Trustee.TrusteeType  = TRUSTEE_IS_USER;
        entry.Trustee.ptstrName    = (LPWSTR) sid;
        PACL pNewDacl = NULL;
        error = SetEntriesInAclW(1, &entry, pDacl, &pNewDacl);
        if ( error == ERROR_SUCCESS ) {
            error = SetSecurityInfo(hObject, SE_WINDOW_OBJECT, DACL_SECURITY_INFORMATION, NULL, NULL,
                        pNewDacl, NULL);
            LocalFree(pNewDacl);
        }
    }
    LocalFree(pSd);
    SetLastError(error);
    return error == ERROR_SUCCESS;
}

/**
 * Builds the environment of the sandboxed process: the system environment, plus the judger's
 * whitelisted toolchain variables (INHERITED_ENVIRONMENT_VARIABLES), TEMP and TMP, the profile
 * variables (PROFILE_ENVIRONMENT_VARIABLES), a private Go build cache and GOPATH in the work
 * directory, and the options of the JVM a run starts (JVM_OPTIONS_VARIABLES). Nothing else of the
 * judger's environment is passed on, and nothing of the sandbox account's own.
 *
 * The system environment is what CreateEnvironmentBlock gives without a token: the system
 * variables (those of HKLM\System\CurrentControlSet\Control\Session Manager\Environment, such as
 * Path, ComSpec and PATHEXT, and SystemRoot, ProgramFiles, ...), never the variables of an
 * account (HKCU\Environment), which a program of the account could set for the ones after it, nor
 * the judger's environment (bInherit = FALSE), whose secrets it would copy.
 * See https://learn.microsoft.com/windows/win32/api/userenv/nf-userenv-createenvironmentblock
 *
 * @param  username         - the sandbox account (USERNAME)
 * @param  workDirectory    - the submission's work directory (empty if unknown: TEMP and TMP are
 *                            then the system's, the profile variables unset and the Go variables
 *                            left to their defaults)
 * @param  javaOptions      - the value of JDK_JAVA_OPTIONS, which the java launcher (and nothing
 *                            else) reads: the heap and other settings that fit the JVM to the run's
 *                            limits, derived from them by JavaOptions.java (empty: not set; any of
 *                            the system environment are dropped either way)
 * @param  environmentBlock - receives the Unicode environment block (sorted "NAME=value" strings,
 *                            each NUL-terminated, followed by an extra NUL)
 * @return whether the block was built (GetLastError() tells why not)
 */
bool buildEnvironmentBlock(const std::wstring& username, const std::wstring& workDirectory,
        const std::wstring& javaOptions, std::vector<wchar_t>& environmentBlock) {
    LPVOID lpSystemEnvironment = NULL;
    if ( !CreateEnvironmentBlock(&lpSystemEnvironment, NULL, FALSE) ) {
        return false;
    }
    EnvironmentMap environment;
    for ( const wchar_t* entry = static_cast<const wchar_t*>(lpSystemEnvironment);
            *entry != L'\0'; entry += wcslen(entry) + 1 ) {
        // A name never contains '=' but may start with one (the per-drive current directories,
        // "=C:=C:\..."): look for the separator after the first character, and drop those entries.
        const wchar_t* separator = wcschr(entry + 1, L'=');
        if ( entry[0] == L'=' || separator == NULL ) {
            continue;
        }
        environment[std::wstring(entry, separator - entry)] = std::wstring(separator + 1);
    }
    DestroyEnvironmentBlock(lpSystemEnvironment);
    // A compilation never gets JVM options, as on Linux, and a run only the judger's (see
    // JavaOptions.java), which a system variable could otherwise override (_JAVA_OPTIONS comes
    // after every other option, -Xmx included).
    for ( const wchar_t* name : JVM_OPTIONS_VARIABLES ) {
        environment.erase(name);
    }
    for ( const wchar_t* name : PROFILE_ENVIRONMENT_VARIABLES ) {
        environment.erase(name);
    }
    environment[L"USERNAME"] = username;

    std::wstring value;
    for ( const wchar_t* name : INHERITED_ENVIRONMENT_VARIABLES ) {
        if ( getJudgerEnvironmentVariable(name, value) ) {
            environment[name] = value;
        }
    }
    for ( const wchar_t* name : ESSENTIAL_ENVIRONMENT_VARIABLES ) {
        if ( environment.find(name) == environment.end() && getJudgerEnvironmentVariable(name, value) ) {
            environment[name] = value;
        }
    }
    if ( !workDirectory.empty() ) {
        // Temporary files in the work directory, as TMPDIR on Linux: the system's TEMP is shared,
        // and the program may not write to it anyway.
        environment[L"TEMP"]    = workDirectory;
        environment[L"TMP"]     = workDirectory;
        for ( const wchar_t* name : PROFILE_ENVIRONMENT_VARIABLES ) {
            environment[name] = workDirectory;
        }
        environment[L"GOCACHE"] = workDirectory + GO_BUILD_CACHE_DIRECTORY;
        environment[L"GOPATH"]  = workDirectory + GO_PATH_DIRECTORY;
    }
    if ( !javaOptions.empty() ) {
        environment[L"JDK_JAVA_OPTIONS"] = javaOptions;
    }

    environmentBlock.clear();
    for ( const auto& variable : environment ) {
        environmentBlock.insert(environmentBlock.end(), variable.first.begin(), variable.first.end());
        environmentBlock.push_back(L'=');
        environmentBlock.insert(environmentBlock.end(), variable.second.begin(), variable.second.end());
        environmentBlock.push_back(L'\0');
    }
    // The block ends with an empty string; an empty block is two NULs.
    if ( environmentBlock.empty() ) {
        environmentBlock.push_back(L'\0');
    }
    environmentBlock.push_back(L'\0');
    return true;
}

/**
 * Gets an environment variable of the judger.
 * @param  name  - the variable's name
 * @param  value - receives its value
 * @return whether the variable is set
 */
bool getJudgerEnvironmentVariable(const wchar_t* name, std::wstring& value) {
    // With a too small buffer, the returned size includes the terminating NUL; 0 means unset.
    DWORD size = GetEnvironmentVariableW(name, NULL, 0);
    if ( size == 0 ) {
        return false;
    }
    std::vector<wchar_t> buffer(size);
    DWORD length = GetEnvironmentVariableW(name, buffer.data(), size);
    if ( length >= size || (length == 0 && GetLastError() == ERROR_ENVVAR_NOT_FOUND) ) {
        return false;
    }
    value.assign(buffer.data(), length);
    return true;
}


/**
 * Gets the peak memory usage: the peak working set (resident memory) of the program's process,
 * the counterpart of the VmHWM the Linux runner measures, for a run and for a compilation (its
 * top-level process, as on Linux). Memory that is committed but never touched does not count, so
 * the JVM's initial heap (see MEMORY_HARD_LIMIT_MIN_HEADROOM_KB) is not mistaken for the program's
 * own use. A run cannot have other processes but its console host (see RUN_ACTIVE_PROCESS_LIMIT),
 * which is not the program's; a compiler's other processes are held to the Job's hard cap.
 *
 * The working set is not a measure the program cannot lower. Its pages leave it, while staying
 * committed and readable (they are faulted back from the standby or modified list, or the paging
 * file), when the program calls SetProcessWorkingSetSize(Ex) with (SIZE_T)-1 for both sizes,
 * EmptyWorkingSet, or VirtualUnlock on pages it has not locked; it needs no privilege for that, and
 * a Job cannot take it away (JOB_OBJECT_LIMIT_WORKINGSET still lets a process empty its working
 * set). A program that trims itself as it goes can thus touch more memory than its limit without
 * its peak working set ever reaching it. No documented per-process measure tells such pages apart
 * from the JVM's committed but untouched heap: the commit charge (PagefileUsage, PrivateUsage)
 * counts both, QueryWorkingSetEx says nothing of a page that is not resident, and PageFaultCount
 * also counts every page freed and touched again. Measuring commit would judge every Java program
 * by the size of the heap the JVM reserves for it, not by what it uses, and differently from Linux;
 * so the limit is measured on the working set, and the residual is bounded by the Job's commit cap
 * (see MEMORY_HARD_LIMIT_MIN_HEADROOM_KB): a self-trimming run can hold up to 2 x the limit +
 * max(256 MB, physical RAM / 32) in all before an allocation fails and it is judged Memory Limit
 * Exceeded. It pays for that with a page fault for every trimmed page it touches again.
 * See https://learn.microsoft.com/windows/win32/api/psapi/ns-psapi-process_memory_counters,
 * https://learn.microsoft.com/windows/win32/api/memoryapi/nf-memoryapi-setprocessworkingsetsizeex,
 * https://learn.microsoft.com/windows/win32/api/memoryapi/nf-memoryapi-virtualunlock and
 * https://learn.microsoft.com/windows/win32/api/winnt/ns-winnt-jobobject_basic_limit_information
 * @param  hProcess - the main process handle
 * @param  memory   - receives the peak memory usage (KB)
 * @return whether the memory could be measured (GetLastError() tells why not)
 */
bool getPeakMemoryUsage(HANDLE& hProcess, jint& memory) {
    PROCESS_MEMORY_COUNTERS counters = {0};
    counters.cb = sizeof(counters);
    if ( !GetProcessMemoryInfo(hProcess, &counters, sizeof(counters)) ) {
        return false;
    }
    unsigned long long peakKb = (unsigned long long) counters.PeakWorkingSetSize >> 10;
    memory = ( peakKb > (unsigned long long)INT32_MAX ) ? INT32_MAX : (jint)peakKb;
    return true;
}

/**
 * Gets the CPU time (user + kernel) used by the run: the main process's own time or, when children
 * are included, the Job's total over every process that ever ran in it (so CPU burnt in child
 * processes counts too), whichever is larger. Both figures are live while the processes run - the
 * Job's totals cover its active processes as well as the terminated ones - so the same function
 * enforces the limit during the run and measures it afterwards.
 * See https://learn.microsoft.com/windows/win32/api/winnt/ns-winnt-jobobject_basic_accounting_information
 * @param  hJob             - the Job Object handle
 * @param  hProcess         - the main process handle
 * @param  includeChildren  - whether to charge the CPU time of the other processes in the Job
 * @param  cpuTime          - receives the CPU time used (ms)
 * @return whether the CPU time could be measured (GetLastError() tells why not)
 */
bool getCpuTimeUsage(HANDLE& hJob, HANDLE& hProcess, bool includeChildren, jint& cpuTime) {
    unsigned long long cpu100ns = 0;
    FILETIME creationTime, exitTime, kernelTime, userTime;
    if ( !GetProcessTimes(hProcess, &creationTime, &exitTime, &kernelTime, &userTime) ) {
        return false;
    }
    // FILETIME is measured in 100-nanosecond intervals.
    cpu100ns = fileTimeToUll(kernelTime) + fileTimeToUll(userTime);
    if ( includeChildren ) {
        JOBOBJECT_BASIC_ACCOUNTING_INFORMATION accounting = {0};
        if ( !QueryInformationJobObject(hJob, JobObjectBasicAccountingInformation,
                &accounting, sizeof(accounting), NULL) ) {
            return false;
        }
        unsigned long long jobCpu100ns = (unsigned long long) accounting.TotalUserTime.QuadPart +
            (unsigned long long) accounting.TotalKernelTime.QuadPart;
        if ( jobCpu100ns > cpu100ns ) {
            cpu100ns = jobCpu100ns;
        }
    }
    unsigned long long cpuMs = cpu100ns / 10000ULL;
    cpuTime = ( cpuMs > (unsigned long long)INT32_MAX ) ? INT32_MAX : (jint)cpuMs;
    return true;
}

/**
 * Converts a FILETIME to a 64-bit integer.
 * @param  fileTime - the FILETIME structure
 * @return the corresponding 64-bit value
 */
unsigned long long fileTimeToUll(const FILETIME& fileTime) {
    ULARGE_INTEGER uli;
    uli.LowPart  = fileTime.dwLowDateTime;
    uli.HighPart = fileTime.dwHighDateTime;
    return uli.QuadPart;
}

/**
 * Gets the Windows API error message.
 * @param  apiName - the Windows API name
 * @return the Windows API error message
 */
std::string getErrorMessage(const std::string& apiName) {
    DWORD  errorCode      = GetLastError();
    LPWSTR lpMessage      = NULL;
    // The wide variant, converted to UTF-8 below: the message becomes a Java string, which must not
    // be in the ANSI code page. IGNORE_INSERTS because a system message may contain %1-style
    // inserts for which there are no arguments. With ALLOCATE_BUFFER, lpBuffer receives a pointer
    // to a LocalAlloc'ed buffer, and nothing at all if the call fails.
    DWORD  messageLength  = FormatMessageW(FORMAT_MESSAGE_ALLOCATE_BUFFER |
        FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
        NULL, errorCode,
        MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
        (LPWSTR)&lpMessage, 0, NULL);

    std::string errorMessage;
    if ( messageLength > 0 && lpMessage != NULL ) {
        int length = WideCharToMultiByte(CP_UTF8, 0, lpMessage, (int) messageLength, NULL, 0, NULL, NULL);
        if ( length > 0 ) {
            errorMessage.resize(length);
            WideCharToMultiByte(CP_UTF8, 0, lpMessage, (int) messageLength, &errorMessage[0], length, NULL, NULL);
        }
    }
    if ( lpMessage != NULL ) {
        LocalFree(lpMessage);
    }

    std::stringstream stringStream;
    stringStream << "API:     " << apiName << std::endl
                 << "Code:    " << errorCode << std::endl
                 << "Message: " << errorMessage << std::endl;
    return stringStream.str();
}

/**
 * Gets the value of a Java String as a wide string. A Java string is UTF-16, as a wide string is on
 * Windows, so its characters are copied as they are: JNI's modified UTF-8 (GetStringUTFChars)
 * encodes a character outside the Basic Multilingual Plane (such as an emoji in a password) as two
 * surrogates of three bytes each, which a UTF-8 decoder rejects.
 * @param  jniEnv - a reference to the JNI runtime environment
 * @param  jStr   - the Java string (may be null)
 * @return its value, or an empty string for null
 */
std::wstring getWideStringValue(JNIEnv* jniEnv, jstring jStr) {
    static_assert(sizeof(wchar_t) == sizeof(jchar), "a wide character is a UTF-16 code unit");
    if ( jStr == NULL ) {
        return std::wstring();
    }
    jsize        length = jniEnv->GetStringLength(jStr);
    std::wstring value((size_t) length, L'\0');
    if ( length > 0 ) {
        jniEnv->GetStringRegion(jStr, 0, length, reinterpret_cast<jchar*>(&value[0]));
    }
    return value;
}
