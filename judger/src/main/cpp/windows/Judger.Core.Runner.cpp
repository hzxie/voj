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
 * limit is enforced by polling the Job's peak accounting (a soft limit), so that an offending peak
 * rises just above the limit before the process is killed and the run is classified as MLE -
 * matching the Linux runner.
 */
#define POLL_INTERVAL_MS 2

/**
 * The Job's hard memory limit is the memory limit plus this headroom: max(limit / 4, 16 MB). The
 * hard limit bounds what a program can allocate between two polls. It does not kill the program:
 * an allocation that would exceed it fails, and the Job posts JOB_OBJECT_MSG_(JOB|PROCESS)_MEMORY_LIMIT
 * to its completion port, which the runner treats as Memory Limit Exceeded (otherwise the failed
 * allocation would surface as a crash, indistinguishable from a runtime error, or be survived by a
 * program that checks for it). The headroom keeps the ordinary case - memory growing past the
 * limit - on the precise polling path.
 * See https://learn.microsoft.com/windows/win32/api/winnt/ns-winnt-jobobject_extended_limit_information
 * and https://learn.microsoft.com/windows/win32/api/winnt/ns-winnt-jobobject_associate_completion_port
 */
static const unsigned long long MEMORY_HARD_LIMIT_MIN_HEADROOM_KB = 16ULL * 1024;
static const unsigned long long MEMORY_HARD_LIMIT_HEADROOM_DIVISOR = 4;

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
 * echoes it into the compile log), so the sandbox gets the sandbox user's own environment plus
 * only the locale and toolchain settings that compilers and runtimes genuinely need. PATH is the
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
 * in the sandbox user's environment; the judger's values are only a fallback for a block that
 * somehow lacks one. None of them is a secret.
 */
static const wchar_t* const ESSENTIAL_ENVIRONMENT_VARIABLES[] = {
    L"SystemRoot", L"SystemDrive", L"windir", L"ComSpec", L"PATHEXT",
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
void setupStartupInfo(STARTUPINFOW&, HANDLE&, HANDLE&, HANDLE&);
HANDLE createJobObject(DWORD, jint, bool, HANDLE);
bool drainJobMessages(HANDLE, bool&);
bool createProcess(const std::wstring&, const std::wstring&, const std::wstring&, const std::wstring&, HANDLE&, STARTUPINFOW&, PROCESS_INFORMATION&);
bool buildEnvironmentBlock(HANDLE, const std::wstring&, std::vector<wchar_t>&);
bool getJudgerEnvironmentVariable(const wchar_t*, std::wstring&);
std::wstring getWorkDirectory(const std::wstring&);
bool getPeakMemoryUsage(HANDLE& hJob, HANDLE& hProcess, bool jobWide, jint& memory);
bool getCpuTimeUsage(HANDLE& hJob, HANDLE& hProcess, bool includeChildren, jint& cpuTime);
unsigned long long fileTimeToUll(const FILETIME&);
std::string getErrorMessage(const std::string&);
std::wstring getWideString(const std::string&);
LPWSTR getWideStringPointer(const std::wstring&);
LPCWSTR getConstWideStringPointer(const std::wstring&);

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
 *                           receives stderr, otherwise stderr is discarded
 * @param  timeLimit       - the program execution time limit (ms, 0 means no limit)
 * @param  memoryLimit     - the program execution memory limit (KB, 0 means no limit)
 * @param  isCompilation   - whether this is a compilation (as opposed to a run of the submission)
 * @param  processLimit    - the maximum number of active processes of a compilation (0 means the
 *                           default); a run may not create processes (RUN_ACTIVE_PROCESS_LIMIT)
 * @return a Map<String, Object> object containing the runtime result
 */
JNIEXPORT jobject JNICALL Java_org_verwandlung_voj_judger_core_Runner_getRuntimeResult(
    JNIEnv* jniEnv, jobject selfReference, jstring jCommandLine, jstring jUsername,
    jstring jPassword, jstring jInputFilePath, jstring jOutputFilePath, jint timeLimit,
    jint memoryLimit, jboolean isCompilation, jint processLimit) {
    std::wstring        commandLine         = getWideString(getStringValue(jniEnv, jCommandLine));
    std::wstring        username            = getWideString(getStringValue(jniEnv, jUsername));
    std::wstring        password            = getWideString(getStringValue(jniEnv, jPassword));
    std::wstring        inputFilePath       = getWideString(getStringValue(jniEnv, jInputFilePath));
    std::wstring        outputFilePath      = getWideString(getStringValue(jniEnv, jOutputFilePath));

    HANDLE              hInput              = NULL;
    HANDLE              hOutput             = NULL;
    HANDLE              hError              = NULL;
    HANDLE              hToken              = NULL;
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
        if ( hToken != NULL ) {
            CloseHandle(hToken);
            hToken = NULL;
        }
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
    // closed. The memory limit itself is enforced below by polling the Job's peak accounting (a soft
    // limit). Without a Job none of this holds, so the run is refused rather than started unconfined.
    // A run of the submission may not create processes; a compiler may (cc1, as, ld, ...). For the
    // same reason a run's memory is that of the whole Job, a compilation's that of its largest process.
    DWORD activeProcessLimit = isCompilation
                             ? ( processLimit > 0 ? (DWORD) processLimit : ACTIVE_PROCESS_LIMIT )
                             : RUN_ACTIVE_PROCESS_LIMIT;
    hJob = createJobObject(activeProcessLimit, memoryLimit, !isCompilation, hCompletionPort);
    if ( hJob == NULL ) {
        return fail("CreateJobObject");
    }

    // The process is created suspended, assigned to the Job Object before it runs a single
    // instruction, and only then resumed - so the limits are in force from the very beginning.
    if ( !createProcess(commandLine, username, password, getWorkDirectory(outputFilePath), hToken,
            startupInfo, processInfo) ) {
        return fail("CreateProcess");
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
    //  - memory, as a soft limit: the peak may rise just above the limit before the kill, so that
    //    the run is classified as MLE (matching the Linux runner). An allocation refused by the
    //    Job's hard cap is reported on the completion port and is a Memory Limit Exceeded too;
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
            if ( !getPeakMemoryUsage(hJob, processInfo.hProcess, !isCompilation, memoryUsage) ) {
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
    // Both the Job's and the process's peak accounting persist after the process exits. A refused
    // allocation is posted to the port before the allocating call returns, so a program that exits
    // after one has its message queued by now.
    if ( !getPeakMemoryUsage(hJob, processInfo.hProcess, !isCompilation, memoryUsage) ) {
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
        // The work directory is writable by the sandbox account, which may have left a hard link (or,
        // given the right to create one, a symbolic link) at this path during an earlier checkpoint.
        // Opening it with CREATE_ALWAYS would truncate the link's target with the judger's rights.
        // So whatever is at the path is removed first (DeleteFileW removes a link, never its target)
        // and the file is created with CREATE_NEW, which fails rather than open anything that is
        // still, or again, at the path; FILE_FLAG_OPEN_REPARSE_POINT never follows a reparse point.
        // A failed deletion is not an error of its own: CREATE_NEW then fails, and the run is
        // refused. FILE_READ_ATTRIBUTES lets the runner poll the file's size through this handle
        // (GetFileSizeEx needs it, and GENERIC_WRITE does not include it). The child inherits the
        // handle as its stdout, which only lets it read the attributes of its own output file.
        DeleteFileW(outputFilePath.c_str());
        hOutput = CreateFileW(outputFilePath.c_str(), GENERIC_WRITE | FILE_READ_ATTRIBUTES, FILE_SHARE_READ, &sa,
                    CREATE_NEW, FILE_ATTRIBUTE_NORMAL | FILE_FLAG_OPEN_REPARSE_POINT, NULL);
        if ( hOutput == INVALID_HANDLE_VALUE )  {
            hOutput = NULL;
            return false;
        }
    }
    return true;
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
        unsigned long long limitKb    = (unsigned long long) memoryLimit;
        unsigned long long headroomKb = limitKb / MEMORY_HARD_LIMIT_HEADROOM_DIVISOR;
        if ( headroomKb < MEMORY_HARD_LIMIT_MIN_HEADROOM_KB ) {
            headroomKb = MEMORY_HARD_LIMIT_MIN_HEADROOM_KB;
        }
        unsigned long long hardLimitBytes = (limitKb + headroomKb) * 1024ULL;
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
 * Creates a process running as the given (low-privilege) user.
 *
 * CreateProcessWithLogonW is used (rather than CreateProcessAsUser) because it does not require the
 * judger process to hold SE_ASSIGNPRIMARYTOKEN/SE_INCREASE_QUOTA. The process is created suspended so
 * the caller can assign it to a Job Object before it runs.
 *
 * @param  commandLine   - the command line to execute
 * @param  username      - the Windows username
 * @param  password      - the Windows password
 * @param  workDirectory - the submission's work directory, where the process starts (empty if
 *                         unknown: the sandbox account's profile directory)
 * @param  hToken        - receives a token that represents the specified user
 * @param  startupInfo   - a STARTUPINFO structure
 * @param  processInfo   - receives identification information for the new process
 * @return whether the process was created successfully (GetLastError() tells why not)
 */
bool createProcess(const std::wstring& commandLine, const std::wstring& username,
        const std::wstring& password, const std::wstring& workDirectory, HANDLE& hToken,
        STARTUPINFOW& startupInfo, PROCESS_INFORMATION& processInfo) {
    WCHAR   szUserProfile[256]  = L"";
    DWORD   dwSize              = sizeof(szUserProfile) / sizeof(WCHAR);
    LPCWSTR lpUsername          = getConstWideStringPointer(username);
    LPCWSTR lpDomain            = L".";
    LPCWSTR lpPassword          = getConstWideStringPointer(password);
    LPWSTR  lpCommandLine       = getWideStringPointer(commandLine);
    std::vector<wchar_t> environmentBlock;

    if ( !LogonUserW(lpUsername, lpDomain, lpPassword, LOGON32_LOGON_BATCH,
            LOGON32_PROVIDER_DEFAULT, &hToken) ) {
        return false;
    }
    if ( !buildEnvironmentBlock(hToken, workDirectory, environmentBlock) ) {
        return false;
    }
    // The program starts in the submission's own work directory, as on Linux, never in a directory
    // that outlives the submission or that every submission shares. The sandbox account can always
    // enter it: it reads the source and writes the program there when compiling, and runs the
    // program from there.
    std::wstring currentDirectory = workDirectory;
    for ( wchar_t& c : currentDirectory ) {
        if ( c == L'/' ) {
            c = L'\\';
        }
    }
    if ( currentDirectory.empty() ) {
        if ( !GetUserProfileDirectoryW(hToken, szUserProfile, &dwSize) ) {
            return false;
        }
        currentDirectory = szUserProfile;
    }
    if ( !CreateProcessWithLogonW(lpUsername, lpDomain, lpPassword,
            LOGON_WITH_PROFILE, NULL, lpCommandLine,
            CREATE_UNICODE_ENVIRONMENT | CREATE_SUSPENDED | CREATE_NO_WINDOW,
            environmentBlock.data(), currentDirectory.c_str(), &startupInfo, &processInfo) ) {
        return false;
    }
    return true;
}

/**
 * Builds the environment of the sandboxed process: the sandbox user's own environment, plus the
 * judger's whitelisted toolchain variables (INHERITED_ENVIRONMENT_VARIABLES) and a private Go build
 * cache and GOPATH in the work directory. Nothing else of the judger's environment is passed on.
 *
 * CreateEnvironmentBlock is called with bInherit = FALSE, so the block holds only the system
 * variables and the user's own variables and profile paths (USERPROFILE, APPDATA, LOCALAPPDATA,
 * TEMP, ...), never the judger's environment. With TRUE it would also copy the judger's whole
 * environment, secrets included.
 * See https://learn.microsoft.com/windows/win32/api/userenv/nf-userenv-createenvironmentblock
 *
 * @param  hToken           - the sandbox user's token
 * @param  workDirectory    - the submission's work directory (empty if unknown: the Go variables
 *                            are then left to the user's defaults)
 * @param  environmentBlock - receives the Unicode environment block (sorted "NAME=value" strings,
 *                            each NUL-terminated, followed by an extra NUL)
 * @return whether the block was built (GetLastError() tells why not)
 */
bool buildEnvironmentBlock(HANDLE hToken, const std::wstring& workDirectory,
        std::vector<wchar_t>& environmentBlock) {
    LPVOID lpUserEnvironment = NULL;
    if ( !CreateEnvironmentBlock(&lpUserEnvironment, hToken, FALSE) ) {
        return false;
    }
    EnvironmentMap environment;
    for ( const wchar_t* entry = static_cast<const wchar_t*>(lpUserEnvironment);
            *entry != L'\0'; entry += wcslen(entry) + 1 ) {
        // A name never contains '=' but may start with one (the per-drive current directories,
        // "=C:=C:\..."): look for the separator after the first character, and drop those entries.
        const wchar_t* separator = wcschr(entry + 1, L'=');
        if ( entry[0] == L'=' || separator == NULL ) {
            continue;
        }
        environment[std::wstring(entry, separator - entry)] = std::wstring(separator + 1);
    }
    DestroyEnvironmentBlock(lpUserEnvironment);

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
        environment[L"GOCACHE"] = workDirectory + GO_BUILD_CACHE_DIRECTORY;
        environment[L"GOPATH"]  = workDirectory + GO_PATH_DIRECTORY;
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
 * Gets the submission's work directory: the directory holding the output file, as on Linux.
 * @param  outputFilePath - the output file path (the judger passes '/'-separated paths)
 * @return the work directory, or an empty string if the path has no directory part
 */
std::wstring getWorkDirectory(const std::wstring& outputFilePath) {
    size_t lastSeparator = outputFilePath.find_last_of(L"/\\");
    if ( lastSeparator == std::wstring::npos || lastSeparator == 0 ) {
        return std::wstring();
    }
    return outputFilePath.substr(0, lastSeparator);
}

/**
 * Gets the peak memory (commit) usage:
 *  - of a run, the Job's PeakJobMemoryUsed: the peak of the memory committed by all its processes
 *    together, so memory spread over several processes counts in full (as on Linux, where the
 *    processes' PSS are summed). It includes the console host's own small commit (see
 *    RUN_ACTIVE_PROCESS_LIMIT);
 *  - of a compilation, its largest process: the Job's PeakProcessMemoryUsed. The compiler's
 *    processes run in parallel by design, and the limit is a cap against a "compiler bomb".
 * The main process's own peak commit is taken into account too; it can only be lower.
 * See https://learn.microsoft.com/windows/win32/api/winnt/ns-winnt-jobobject_extended_limit_information
 * @param  hJob     - the Job Object handle
 * @param  hProcess - the main process handle
 * @param  jobWide  - whether to measure all processes together (a run) or the largest one
 * @param  memory   - receives the peak memory usage (KB)
 * @return whether the memory could be measured (GetLastError() tells why not)
 */
bool getPeakMemoryUsage(HANDLE& hJob, HANDLE& hProcess, bool jobWide, jint& memory) {
    JOBOBJECT_EXTENDED_LIMIT_INFORMATION jeli = {0};
    if ( !QueryInformationJobObject(hJob, JobObjectExtendedLimitInformation, &jeli, sizeof(jeli), NULL) ) {
        return false;
    }
    unsigned long long peakBytes = jobWide ? (unsigned long long) jeli.PeakJobMemoryUsed
                                           : (unsigned long long) jeli.PeakProcessMemoryUsed;
    PROCESS_MEMORY_COUNTERS counters = {0};
    counters.cb = sizeof(counters);
    if ( !GetProcessMemoryInfo(hProcess, &counters, sizeof(counters)) ) {
        return false;
    }
    if ( (unsigned long long) counters.PeakPagefileUsage > peakBytes ) {
        peakBytes = (unsigned long long) counters.PeakPagefileUsage;
    }
    unsigned long long peakKb = peakBytes >> 10;
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
 * Converts a (UTF-8) std::string to a std::wstring.
 * @param  str - a UTF-8 std::string
 * @return the corresponding std::wstring
 */
std::wstring getWideString(const std::string& str) {
    if ( str.empty() ) {
        return std::wstring();
    }
    int length = MultiByteToWideChar(CP_UTF8, 0, str.c_str(), (int)str.size(), NULL, 0);
    std::wstring wstr(length, 0);
    MultiByteToWideChar(CP_UTF8, 0, str.c_str(), (int)str.size(), &wstr[0], length);
    return wstr;
}

/**
 * Gets the LPWSTR pointer corresponding to a std::wstring.
 * @param  str - a std::wstring string
 * @return the corresponding LPWSTR pointer
 */
LPWSTR getWideStringPointer(const std::wstring& str) {
    return const_cast<LPWSTR>(str.c_str());
}

/**
 * Gets the LPCWSTR pointer corresponding to a std::wstring.
 * @param  str - a std::wstring string
 * @return the corresponding LPCWSTR pointer
 */
LPCWSTR getConstWideStringPointer(const std::wstring& str) {
    return str.c_str();
}
