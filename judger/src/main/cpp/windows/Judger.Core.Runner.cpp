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
 * create processes (as on Linux), but its console host counts too. A console program created by
 * CreateProcessWithTokenW gets a console of its own (CREATE_NO_WINDOW: one without a window), and
 * since Windows 8 the console host (conhost.exe) is started by the console program itself while it
 * initialises - after it has been assigned to the Job - so it becomes a member of the Job. With a
 * limit of 1 the console host could not start and every console program would fail to initialise
 * (STATUS_DLL_INIT_FAILED). With 2, the program plus its console host fill the Job, and any
 * CreateProcess of the submission fails. (Should the console host not be in the Job, the program
 * could have one child process at a time, still confined to the Job.)
 * See https://learn.microsoft.com/windows/console/creating-a-pseudoconsole-session and
 * https://learn.microsoft.com/windows/win32/procthread/process-creation-flags
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
 * How long (ms) the runner waits, once the program is gone, for the rest of its Job (a run's
 * console host, what a compiler left running) to be killed and gone, before it gives up on the run.
 */
static const DWORD JOB_EMPTY_TIMEOUT_MS = 5000;

/**
 * The longest command line CreateProcessWithTokenW takes (in characters, the terminating NUL not
 * included). A longer one is refused before the call, with a message that says why.
 * See https://learn.microsoft.com/windows/win32/api/winbase/nf-winbase-createprocesswithtokenw
 */
static const size_t MAX_COMMAND_LINE_LENGTH = 1024;

/**
 * The error mode every compilation and run starts with, as SetErrorMode(SEM_FAILCRITICALERRORS |
 * SEM_NOGPFAULTERRORBOX | SEM_NOOPENFILEERRORBOX) would set it: no error box the program would wait
 * on, nobody being there to close it on its desktop (see WINDOW_STATION_ACCESS). A program that
 * imports a DLL that cannot be found (or that fails to initialise) is then ended at once
 * (STATUS_DLL_NOT_FOUND, ...), a runtime error, rather than kept waiting until it is killed at the
 * wall-clock limit, a time limit exceeded. The Job's JOB_OBJECT_LIMIT_DIE_ON_UNHANDLED_EXCEPTION
 * covers SEM_NOGPFAULTERRORBOX only. The error mode is a process's own (it is what processes it
 * creates inherit), so it is set on the program's process while it is still suspended, through
 * NtSetInformationProcess(ProcessDefaultHardErrorMode), which is what SetErrorMode does for the
 * calling process; there, the bit of SEM_FAILCRITICALERRORS means the opposite (critical errors
 * are reported), so it is left clear.
 * See https://learn.microsoft.com/windows/win32/api/errhandlingapi/nf-errhandlingapi-seterrormode
 */
static const ULONG PROCESS_DEFAULT_HARD_ERROR_MODE = 12;
static const ULONG HARD_ERROR_MODE                 = SEM_NOGPFAULTERRORBOX | SEM_NOOPENFILEERRORBOX;
typedef LONG (NTAPI *NtSetInformationProcessFunction)(HANDLE, ULONG, PVOID, ULONG);
typedef ULONG (NTAPI *RtlNtStatusToDosErrorFunction)(LONG);

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
 * to, but only to what is labelled Low: its work directory (see WORK_DIRECTORY_LABEL). What
 * Windows keeps for Low processes, the account's own %USERPROFILE%\AppData\LocalLow and
 * HKCU\Software\AppDataLow, would outlive the program, and let it leave data for a later one: the
 * judger labels both Medium (see raiseLowLabels). Reading is not restricted (files have no "no read
 * up" policy). Nor can it open the processes of the account that are not Low, or send messages to
 * their windows (UIPI).
 * See https://learn.microsoft.com/windows/win32/secauthz/mandatory-integrity-control
 */
static const wchar_t* const LOW_INTEGRITY_SID = L"S-1-16-4096";
/**
 * The labels of the account's LocalLow folder and AppDataLow key (see raiseLowLabels): Medium, with
 * the "no write up" policy, inherited by what is in them (a registry key has no objects to pass
 * it on to, only subkeys: CI).
 */
static const wchar_t* const MEDIUM_INTEGRITY_SID  = L"S-1-16-8192";
static const wchar_t* const MEDIUM_FOLDER_LABEL   = L"S:(ML;OICI;NW;;;ME)";
static const wchar_t* const MEDIUM_KEY_LABEL      = L"S:(ML;CI;NW;;;ME)";
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
 * The desktop of every compilation and run: a private one, which the judger creates for its
 * programs alone on its own window station (see createPrivateDesktop), never the operator's
 * (WinSta0\Default), whose windows a program could otherwise see and send input to. user32
 * connects to its window station and desktop as a console program starts (a JVM's does, whoami's
 * does), and without access to them it cannot initialise (STATUS_DLL_INIT_FAILED).
 * CreateProcessWithTokenW grants the account no access (CreateProcessWithLogonW grants the account
 * access to the window station and desktop a process inherits), so:
 *  - the private desktop's DACL gives the logon session of the sandbox account (its logon SID,
 *    which no other logon has) DESKTOP_ACCESS, the judger and SYSTEM full control, and nobody else
 *    any access; it is labelled Low, as WinSta0\Default is, so that a Low process may use it;
 *  - the judger's window station gives that logon session WINDOW_STATION_ACCESS, which is what
 *    user32 needs: READ_CONTROL, WINSTA_ACCESSGLOBALATOMS and WINSTA_EXITWINDOWS (without any of
 *    them, user32 fails to initialise), not to enumerate its desktops, read the clipboard or the
 *    screen. Logging off or shutting down is denied all the same: by the Job
 *    (JOB_OBJECT_UILIMIT_EXITWINDOWS) and, for shutting down, for want of SeShutdownPrivilege.
 *    The entry is removed when the session is closed (an entry left by a judger that exited names
 *    a logon session that is gone, and so grants nothing).
 * A window station of its own does not work: on any window station but the interactive one
 * (WinSta0), such as one the judger creates, or a service's (a judger in session 0), every program
 * that loads user32 fails to initialise, whatever access its DACL grants.
 * See https://learn.microsoft.com/windows/win32/winstation/window-station-security-and-access-rights,
 * https://learn.microsoft.com/windows/win32/winstation/desktop-security-and-access-rights and
 * https://learn.microsoft.com/windows/win32/api/winbase/nf-winbase-createprocesswithtokenw
 */
static const DWORD WINDOW_STATION_ACCESS = READ_CONTROL | WINSTA_ACCESSGLOBALATOMS | WINSTA_EXITWINDOWS;
static const DWORD DESKTOP_ACCESS        = READ_CONTROL | DESKTOP_READOBJECTS | DESKTOP_CREATEWINDOW |
                                           DESKTOP_CREATEMENU | DESKTOP_WRITEOBJECTS;
/** The label of the private desktop: Low, with the "no write up" policy. */
static const wchar_t* const PRIVATE_DESKTOP_LABEL = L"S:(ML;;NW;;;LW)";

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
 * The sandbox account's logon session, which every compilation and run of the judger uses: the
 * account is logged on (LogonUserW, as a batch job) and its profile loaded once, when the first
 * compilation or run needs them, and kept until the judger exits (or another account is
 * configured), rather than for every compilation and run. Guarded by sandboxSessionLock.
 */
struct SandboxSession {
    std::wstring username;
    std::wstring password;
    HANDLE       hToken;    // the account's logon token
    HANDLE       hProfile;  // its loaded profile (registry hive), unloaded with the session
    HANDLE       hRunToken; // the token every program runs with (see createLowIntegrityToken)
    HDESK        hDesktop;  // the private desktop of its programs (see createPrivateDesktop)
    std::wstring desktop;   // "<window station>\<desktop>", the programs' lpDesktop
    PSID         logonSid;  // the SID of the logon session, granted access to the window station
};
static SandboxSession sandboxSession     = { L"", L"", NULL, NULL, NULL, NULL, L"", NULL };
static SRWLOCK        sandboxSessionLock = SRWLOCK_INIT;

/**
 * Function Prototypes.
 */
bool setupIoRedirection(const std::wstring&, const std::wstring&, HANDLE&, HANDLE&);
bool getJudgerOnlySecurityDescriptor(PSECURITY_DESCRIPTOR&);
void setupStartupInfo(STARTUPINFOW&, HANDLE&, HANDLE&, HANDLE&);
HANDLE createJobObject(DWORD, jint, bool, HANDLE);
bool drainJobMessages(HANDLE, bool&);
bool emptyJob(HANDLE, HANDLE, bool&);
bool disableErrorDialogs(HANDLE);
bool createProcess(const std::wstring&, const std::wstring&, const std::wstring&, const std::wstring&, const std::wstring&, STARTUPINFOW&, PROCESS_INFORMATION&, std::string&);
bool openSandboxSession(const std::wstring&, const std::wstring&, std::string&);
void closeSandboxSession();
HANDLE createLowIntegrityToken(HANDLE);
bool labelLowIntegrity(const std::wstring&);
bool hasLabel(PACL, PSID, BYTE);
bool raiseLowLabels(HANDLE);
bool ensureMediumLabel(const std::wstring&, SE_OBJECT_TYPE, const wchar_t*, BYTE);
bool createPrivateDesktop(HANDLE, HDESK&, std::wstring&, PSID&);
bool setWindowStationAccess(PSID, bool);
bool getTokenUserSid(HANDLE, std::wstring&);
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
    HANDLE              hJob                = NULL;
    HANDLE              hCompletionPort     = NULL;
    PROCESS_INFORMATION processInfo         = {0};
    STARTUPINFOW        startupInfo         = {0};
    bool                jobInUse            = false; // processes may be (or have been) in the Job
    bool                memoryLimitHit      = false;

    JHashMap            result;
    jint                timeUsage           = 0;
    jint                memoryUsage         = 0;
    DWORD               exitCode            = 127;

    // Releases everything acquired so far. Every early exit below goes through it: a pending Java
    // exception does not stop this function, so each failure must clean up and return explicitly
    // instead of carrying on with invalid handles. Whatever is left of the Job's processes is killed,
    // and gone, first.
    auto cleanup = [&]() {
        if ( jobInUse ) {
            emptyJob(hJob, hCompletionPort, memoryLimitHit);
            jobInUse = false;
        }
        if ( processInfo.hThread != NULL ) {
            CloseHandle(processInfo.hThread);
            processInfo.hThread = NULL;
        }
        if ( processInfo.hProcess != NULL ) {
            CloseHandle(processInfo.hProcess);
            processInfo.hProcess = NULL;
        }
        // Closing the job kills any surviving processes in it (JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE),
        // should emptying it have failed.
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
            startupInfo, processInfo, failedApi) ) {
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
    jobInUse = true;
    if ( !disableErrorDialogs(processInfo.hProcess) ) {
        return fail("NtSetInformationProcess(ProcessDefaultHardErrorMode)");
    }
    if ( ResumeThread(processInfo.hThread) == (DWORD) -1 ) {
        return fail("ResumeThread");
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
    LARGE_INTEGER outputSize      = {};
    ULONGLONG startTick           = GetTickCount64();
    for ( ;; ) {
        DWORD waitResult = WaitForSingleObject(processInfo.hProcess, POLL_INTERVAL_MS);
        if ( waitResult == WAIT_OBJECT_0 ) {
            break; // the process exited on its own
        }
        if ( waitResult == WAIT_FAILED ) {
            // Never spin on a handle that cannot be waited on: kill the run and report the error.
            return fail("WaitForSingleObject");
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

    // Every process of the Job (a run's console host, whatever a compiler left running) is killed,
    // and gone, before the judger goes on: it removes the work directory of a run next, and a
    // process that still had a file of it open would keep it from doing so.
    if ( !emptyJob(hJob, hCompletionPort, memoryLimitHit) ) {
        std::string message = getErrorMessage("EmptyJob");
        jobInUse = false;
        cleanup();
        throwStringException(jniEnv, message);
        return NULL;
    }
    jobInUse = false;
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
    std::wstring sid;
    bool         known = getTokenUserSid(hToken, sid);
    DWORD        error = GetLastError();
    CloseHandle(hToken);
    if ( !known ) {
        SetLastError(error);
        return false;
    }
    std::wstring sddl = L"D:P(A;;FA;;;" + sid + L")";
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

    // Cut the sandboxed program off from the user interface of the system (clipboard, global atoms,
    // other desktops, display and system settings, logging off and shutting down) and from the
    // windows and other USER objects of every process outside its Job (UILIMIT_HANDLES), on top of
    // the private desktop it runs on (see WINDOW_STATION_ACCESS).
    JOBOBJECT_BASIC_UI_RESTRICTIONS uiRestrictions = {0};
    uiRestrictions.UIRestrictionsClass =
        JOB_OBJECT_UILIMIT_DESKTOP | JOB_OBJECT_UILIMIT_DISPLAYSETTINGS |
        JOB_OBJECT_UILIMIT_EXITWINDOWS | JOB_OBJECT_UILIMIT_GLOBALATOMS |
        JOB_OBJECT_UILIMIT_HANDLES | JOB_OBJECT_UILIMIT_READCLIPBOARD |
        JOB_OBJECT_UILIMIT_WRITECLIPBOARD | JOB_OBJECT_UILIMIT_SYSTEMPARAMETERS;
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
 * Kills whatever is left in a Job and waits until it holds no process any more: until the Job
 * reports no active process, or JOB_OBJECT_MSG_ACTIVE_PROCESS_ZERO arrives on its completion port
 * (which is not guaranteed to be delivered, hence the count is looked at too).
 * See https://learn.microsoft.com/windows/win32/api/winnt/ns-winnt-jobobject_associate_completion_port
 * @param  hJob            - the Job
 * @param  hCompletionPort - the Job's completion port
 * @param  memoryLimitHit  - set when an allocation was refused by the Job's hard memory cap
 * @return whether the Job is empty (GetLastError() tells why not: WAIT_TIMEOUT after
 *         JOB_EMPTY_TIMEOUT_MS)
 */
bool emptyJob(HANDLE hJob, HANDLE hCompletionPort, bool& memoryLimitHit) {
    // Fails harmlessly for a Job whose processes are all gone already.
    TerminateJobObject(hJob, 1);
    ULONGLONG deadline = GetTickCount64() + JOB_EMPTY_TIMEOUT_MS;
    for ( ;; ) {
        JOBOBJECT_BASIC_ACCOUNTING_INFORMATION accounting = {0};
        if ( !QueryInformationJobObject(hJob, JobObjectBasicAccountingInformation,
                &accounting, sizeof(accounting), NULL) ) {
            return false;
        }
        if ( accounting.ActiveProcesses == 0 ) {
            return true;
        }
        ULONGLONG now = GetTickCount64();
        if ( now >= deadline ) {
            SetLastError(WAIT_TIMEOUT);
            return false;
        }
        DWORD        message    = 0;
        ULONG_PTR    key        = 0;
        LPOVERLAPPED overlapped = NULL;
        DWORD        wait       = deadline - now < POLL_INTERVAL_MS ? (DWORD) (deadline - now)
                                                                     : POLL_INTERVAL_MS;
        if ( GetQueuedCompletionStatus(hCompletionPort, &message, &key, &overlapped, wait) ) {
            if ( message == JOB_OBJECT_MSG_ACTIVE_PROCESS_ZERO ) {
                return true;
            }
            if ( message == JOB_OBJECT_MSG_JOB_MEMORY_LIMIT || message == JOB_OBJECT_MSG_PROCESS_MEMORY_LIMIT ) {
                memoryLimitHit = true;
            }
        }
    }
}

/**
 * Sets the error mode of a (suspended) process, see HARD_ERROR_MODE.
 * @param  hProcess - the process
 * @return whether it was set (GetLastError() tells why not)
 */
bool disableErrorDialogs(HANDLE hProcess) {
    HMODULE ntdll = GetModuleHandleW(L"ntdll.dll");
    NtSetInformationProcessFunction setInformationProcess = ntdll == NULL ? NULL :
        (NtSetInformationProcessFunction) (void*) GetProcAddress(ntdll, "NtSetInformationProcess");
    RtlNtStatusToDosErrorFunction ntStatusToDosError = ntdll == NULL ? NULL :
        (RtlNtStatusToDosErrorFunction) (void*) GetProcAddress(ntdll, "RtlNtStatusToDosError");
    if ( setInformationProcess == NULL || ntStatusToDosError == NULL ) {
        return false;
    }
    ULONG mode   = HARD_ERROR_MODE;
    LONG  status = setInformationProcess(hProcess, PROCESS_DEFAULT_HARD_ERROR_MODE, &mode, sizeof(mode));
    if ( status < 0 ) {
        SetLastError(ntStatusToDosError(status));
        return false;
    }
    return true;
}

/**
 * Creates a process running as the given (low-privilege) user, without privileges and at Low
 * integrity.
 *
 * The process is created with the run token of the account's logon session (see
 * openSandboxSession and createLowIntegrityToken) by CreateProcessWithTokenW, which needs the judger
 * to hold SeImpersonatePrivilege (an elevated administrator does; CreateProcessAsUserW would need
 * SeAssignPrimaryTokenPrivilege, which only services have). CreateProcessWithLogonW, which needs no
 * privilege, takes no token: the integrity level of the process it creates cannot be lowered, as
 * the judger may not change that process's token. The process is created suspended so the caller
 * can assign it to a Job Object before it runs.
 *
 * @param  commandLine   - the command line to execute
 * @param  username      - the Windows username
 * @param  password      - the Windows password
 * @param  workDirectory - the program's work directory, where the process starts (empty if
 *                         none: the sandbox account's profile directory)
 * @param  javaOptions   - the value of JDK_JAVA_OPTIONS (empty: not set), see buildEnvironmentBlock
 * @param  startupInfo   - a STARTUPINFO structure
 * @param  processInfo   - receives identification information for the new process
 * @param  failedApi     - receives the name of the call that failed, if one did
 * @return whether the process was created successfully (GetLastError() tells why not)
 */
bool createProcess(const std::wstring& commandLine, const std::wstring& username,
        const std::wstring& password, const std::wstring& workDirectory,
        const std::wstring& javaOptions, STARTUPINFOW& startupInfo,
        PROCESS_INFORMATION& processInfo, std::string& failedApi) {
    if ( commandLine.size() > MAX_COMMAND_LINE_LENGTH ) {
        failedApi = "CreateProcessWithToken (the command line is longer than its limit of " +
                    std::to_string(MAX_COMMAND_LINE_LENGTH) + " characters)";
        SetLastError(ERROR_INVALID_PARAMETER);
        return false;
    }
    std::vector<wchar_t> environmentBlock;
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
    // CreateProcessWithTokenW may write to the command line, which is therefore copied.
    std::vector<wchar_t> mutableCommandLine(commandLine.begin(), commandLine.end());
    mutableCommandLine.push_back(L'\0');

    AcquireSRWLockExclusive(&sandboxSessionLock);
    bool created = openSandboxSession(username, password, failedApi);
    if ( created && currentDirectory.empty() ) {
        WCHAR userProfile[MAX_PATH] = L"";
        DWORD size                  = MAX_PATH;
        created = GetUserProfileDirectoryW(sandboxSession.hToken, userProfile, &size) != FALSE;
        currentDirectory = userProfile;
        failedApi = "GetUserProfileDirectory";
    }
    if ( created ) {
        startupInfo.lpDesktop = &sandboxSession.desktop[0];
        created = CreateProcessWithTokenW(sandboxSession.hRunToken, 0, NULL,
                      mutableCommandLine.data(),
                      CREATE_UNICODE_ENVIRONMENT | CREATE_SUSPENDED | CREATE_NO_WINDOW,
                      environmentBlock.data(), currentDirectory.c_str(), &startupInfo,
                      &processInfo) != FALSE;
        failedApi = "CreateProcessWithToken";
    }
    DWORD error = GetLastError();
    startupInfo.lpDesktop = NULL;
    ReleaseSRWLockExclusive(&sandboxSessionLock);
    SetLastError(error);
    return created;
}

/**
 * Opens the sandbox account's logon session (see SandboxSession), unless it is open already for the
 * same account: logs the account on (as a batch job) and loads its profile, its registry (HKCU),
 * which programs expect (the JVM finds user.home there), and creates the token every program runs
 * with. The profile is loaded by the judger with the logon token, not by CreateProcessWithTokenW
 * (LOGON_WITH_PROFILE) with the run token, which cannot create the profile of an account that has
 * none yet (ERROR_INVALID_STATE at its first run). Called with sandboxSessionLock held.
 * @param  username  - the Windows username
 * @param  password  - the Windows password
 * @param  failedApi - receives the name of the call that failed, if one did
 * @return whether the session is open (GetLastError() tells why not)
 */
bool openSandboxSession(const std::wstring& username, const std::wstring& password,
        std::string& failedApi) {
    SandboxSession& session = sandboxSession;
    if ( session.hRunToken != NULL && session.username == username && session.password == password ) {
        return true;
    }
    closeSandboxSession();
    std::wstring profileUsername = username;
    PROFILEINFOW profileInfo     = {0};
    profileInfo.dwSize     = sizeof(profileInfo);
    profileInfo.dwFlags    = PI_NOUI;
    profileInfo.lpUserName = &profileUsername[0];
    if ( !LogonUserW(username.c_str(), L".", password.c_str(), LOGON32_LOGON_BATCH,
            LOGON32_PROVIDER_DEFAULT, &session.hToken) ) {
        session.hToken = NULL;
        failedApi      = "LogonUser";
    } else if ( !LoadUserProfileW(session.hToken, &profileInfo) ) {
        failedApi = "LoadUserProfile";
    } else {
        session.hProfile = profileInfo.hProfile;
        if ( !raiseLowLabels(session.hToken) ) {
            failedApi = "RaiseLowLabels";
        } else {
            session.hRunToken = createLowIntegrityToken(session.hToken);
            failedApi         = "CreateLowIntegrityToken";
        }
    }
    if ( session.hRunToken != NULL &&
            !createPrivateDesktop(session.hToken, session.hDesktop, session.desktop, session.logonSid) ) {
        CloseHandle(session.hRunToken);
        session.hRunToken = NULL;
        failedApi         = "CreatePrivateDesktop";
    }
    if ( session.hRunToken == NULL ) {
        DWORD error = GetLastError();
        closeSandboxSession();
        SetLastError(error);
        return false;
    }
    session.username = username;
    session.password = password;
    return true;
}

/**
 * Closes the sandbox account's logon session, if it is open: unloads its profile and closes its
 * tokens. Called with sandboxSessionLock held, when no program of the session runs any more.
 */
void closeSandboxSession() {
    SandboxSession& session = sandboxSession;
    if ( session.hDesktop != NULL ) {
        CloseDesktop(session.hDesktop);
        session.hDesktop = NULL;
    }
    if ( session.logonSid != NULL ) {
        setWindowStationAccess(session.logonSid, false);
        LocalFree(session.logonSid);
        session.logonSid = NULL;
    }
    session.desktop.clear();
    if ( session.hRunToken != NULL ) {
        CloseHandle(session.hRunToken);
        session.hRunToken = NULL;
    }
    if ( session.hProfile != NULL ) {
        UnloadUserProfile(session.hToken, session.hProfile);
        session.hProfile = NULL;
    }
    if ( session.hToken != NULL ) {
        CloseHandle(session.hToken);
        session.hToken = NULL;
    }
    if ( !session.password.empty() ) {
        SecureZeroMemory(&session.password[0], session.password.size() * sizeof(wchar_t));
    }
    session.username.clear();
    session.password.clear();
}

/**
 * Creates a primary token for a process from the logon token of the sandbox account, without
 * privileges and at Low integrity (see LOW_INTEGRITY_SID). The logon token holds the privileges of
 * the account's groups, those of Users among them: SeShutdownPrivilege (on client editions),
 * SeTimeZonePrivilege, SeUndockPrivilege, SeIncreaseWorkingSetPrivilege and
 * SeChangeNotifyPrivilege. None but the last (Bypass traverse checking, which every program needs
 * to reach a file through directories it may not list) is any use to a compiler or a submission,
 * so the token is restricted first (CreateRestrictedToken with DISABLE_MAX_PRIVILEGE removes every
 * other privilege), then lowered. A token's integrity level can be lowered by whoever may adjust
 * it, never raised.
 * See https://learn.microsoft.com/windows/win32/secauthz/mandatory-integrity-control and
 * https://learn.microsoft.com/windows/win32/api/securitybaseapi/nf-securitybaseapi-createrestrictedtoken
 * @param  hToken - the logon token of the sandbox account
 * @return the new token, or NULL on failure (GetLastError() tells why)
 */
HANDLE createLowIntegrityToken(HANDLE hToken) {
    HANDLE hRestrictedToken = NULL;
    if ( !CreateRestrictedToken(hToken, DISABLE_MAX_PRIVILEGE, 0, NULL, 0, NULL, 0, NULL,
            &hRestrictedToken) ) {
        return NULL;
    }
    // CreateProcessWithTokenW puts the token in the judger's session, which takes
    // TOKEN_ADJUST_SESSIONID besides the rights it documents (it fails with ERROR_ACCESS_DENIED
    // otherwise); TOKEN_ADJUST_DEFAULT is for the integrity level.
    HANDLE hLowToken  = NULL;
    BOOL   duplicated = DuplicateTokenEx(hRestrictedToken, TOKEN_QUERY | TOKEN_DUPLICATE |
                            TOKEN_ASSIGN_PRIMARY | TOKEN_ADJUST_DEFAULT | TOKEN_ADJUST_SESSIONID,
                            NULL, SecurityImpersonation, TokenPrimary, &hLowToken);
    DWORD  duplicateError = GetLastError();
    CloseHandle(hRestrictedToken);
    if ( !duplicated ) {
        SetLastError(duplicateError);
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
    bool                 labelled    = error == ERROR_SUCCESS &&
                                       hasLabel(pSacl, lowSid, OBJECT_INHERIT_ACE | CONTAINER_INHERIT_ACE);
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
 * Checks whether an object (a directory, a registry key) is labelled with an integrity level and
 * passes the label on to what is created in it.
 * @param  pSacl       - the object's SACL (its label; may be NULL: Medium)
 * @param  levelSid    - the SID of the integrity level
 * @param  inheritance - the inheritance flags the label must have (OBJECT_INHERIT_ACE,
 *                       CONTAINER_INHERIT_ACE)
 * @return whether the object is labelled so
 */
bool hasLabel(PACL pSacl, PSID levelSid, BYTE inheritance) {
    if ( pSacl == NULL ) {
        return false;
    }
    for ( DWORD i = 0; i < pSacl->AceCount; ++ i ) {
        PACE_HEADER ace = NULL;
        if ( !GetAce(pSacl, i, (LPVOID*) &ace) || ace->AceType != SYSTEM_MANDATORY_LABEL_ACE_TYPE ) {
            continue;
        }
        PSYSTEM_MANDATORY_LABEL_ACE label = (PSYSTEM_MANDATORY_LABEL_ACE) ace;
        // The first label applies: an object has one.
        return EqualSid((PSID) &label->SidStart, levelSid) &&
               (ace->AceFlags & inheritance) == inheritance && (ace->AceFlags & INHERIT_ONLY_ACE) == 0;
    }
    return false;
}

/**
 * Labels Medium what Windows keeps writable for Low processes in the sandbox account's profile:
 * its LocalLow folder (%USERPROFILE%\AppData\LocalLow) and its HKCU\Software\AppDataLow key, with
 * everything in them, so that a compilation or run (a Low process) cannot write to them, and so
 * cannot leave anything there for a later one. A label of their own that something in them was
 * given is replaced by the one they pass on (TREE_SEC_INFO_RESET), so nothing in them stays Low.
 * Done once the account's profile is loaded, and only where it is still needed: a folder or key
 * labelled Medium already (by an earlier judger) is left as it is, and so is one that does not
 * exist (the AppDataLow key exists only once something created it, which a Low process cannot).
 * See https://learn.microsoft.com/windows/win32/secauthz/mandatory-integrity-control and
 * https://learn.microsoft.com/windows/win32/api/aclapi/nf-aclapi-treesetnamedsecurityinfow
 * @param  hToken - the logon token of the sandbox account, whose profile is loaded
 * @return whether neither is writable at Low integrity any more (GetLastError() tells why not)
 */
bool raiseLowLabels(HANDLE hToken) {
    WCHAR        profile[MAX_PATH] = L"";
    DWORD        size              = MAX_PATH;
    std::wstring sid;
    if ( !GetUserProfileDirectoryW(hToken, profile, &size) || !getTokenUserSid(hToken, sid) ) {
        return false;
    }
    // The account's hive is loaded under HKEY_USERS\<SID>.
    return ensureMediumLabel(std::wstring(profile) + L"\\AppData\\LocalLow", SE_FILE_OBJECT,
               MEDIUM_FOLDER_LABEL, OBJECT_INHERIT_ACE | CONTAINER_INHERIT_ACE) &&
           ensureMediumLabel(L"USERS\\" + sid + L"\\Software\\AppDataLow", SE_REGISTRY_KEY,
               MEDIUM_KEY_LABEL, CONTAINER_INHERIT_ACE);
}

/**
 * Labels a folder or registry key, and everything in it, Medium (see raiseLowLabels), unless it is
 * already or does not exist.
 * @param  name        - the object's name (a path, or a key as "USERS\<SID>\...")
 * @param  type        - its type, SE_FILE_OBJECT or SE_REGISTRY_KEY
 * @param  labelSddl   - the label to give it, in SDDL
 * @param  inheritance - the inheritance flags of that label
 * @return whether the object is labelled Medium, or does not exist (GetLastError() tells why not)
 */
bool ensureMediumLabel(const std::wstring& name, SE_OBJECT_TYPE type, const wchar_t* labelSddl,
        BYTE inheritance) {
    PSID mediumSid = NULL;
    if ( !ConvertStringSidToSidW(MEDIUM_INTEGRITY_SID, &mediumSid) ) {
        return false;
    }
    PACL                 pSacl    = NULL;
    PSECURITY_DESCRIPTOR pCurrent = NULL;
    DWORD error    = GetNamedSecurityInfoW(name.c_str(), type, LABEL_SECURITY_INFORMATION, NULL, NULL,
                         NULL, &pSacl, &pCurrent);
    bool  labelled = error == ERROR_SUCCESS && hasLabel(pSacl, mediumSid, inheritance);
    if ( pCurrent != NULL ) {
        LocalFree(pCurrent);
    }
    LocalFree(mediumSid);
    if ( error == ERROR_FILE_NOT_FOUND || error == ERROR_PATH_NOT_FOUND ) {
        return true;
    }
    if ( error != ERROR_SUCCESS || labelled ) {
        SetLastError(error);
        return labelled;
    }
    PSECURITY_DESCRIPTOR pLabel = NULL;
    if ( !ConvertStringSecurityDescriptorToSecurityDescriptorW(labelSddl, SDDL_REVISION_1, &pLabel,
            NULL) ) {
        return false;
    }
    BOOL present   = FALSE;
    BOOL defaulted = FALSE;
    pSacl = NULL;
    if ( !GetSecurityDescriptorSacl(pLabel, &present, &pSacl, &defaulted) ) {
        error = GetLastError();
    } else {
        std::wstring objectName = name;
        error = TreeSetNamedSecurityInfoW(&objectName[0], type, LABEL_SECURITY_INFORMATION, NULL,
                    NULL, NULL, pSacl, TREE_SEC_INFO_RESET, NULL, ProgressInvokeNever, NULL);
    }
    LocalFree(pLabel);
    SetLastError(error);
    return error == ERROR_SUCCESS;
}

/**
 * Creates the private desktop of the programs of the sandbox account (see WINDOW_STATION_ACCESS) on
 * the judger's window station, named after the judger's process (it is created anew, never an
 * existing one opened, should someone have taken the name), and lets the account's logon session
 * use the window station. The desktop's DACL gives that logon session DESKTOP_ACCESS, the judger
 * and SYSTEM full control, and nobody else any access.
 * @param  hToken   - the logon token of the sandbox account
 * @param  hDesktop - receives the desktop
 * @param  desktop  - receives the desktop's full name, "<window station>\<desktop>"
 * @param  logonSid - receives the SID of the account's logon session, which the window station now
 *                    grants access to (to be freed with LocalFree once revoked)
 * @return whether the desktop was created and the window station's access granted (GetLastError()
 *         tells why not)
 */
bool createPrivateDesktop(HANDLE hToken, HDESK& hDesktop, std::wstring& desktop, PSID& logonSid) {
    static LONG  counter = 0;
    std::wstring sessionSid;
    std::wstring judgerSid;
    HANDLE       hJudgerToken = NULL;
    // The logon SID of the account's logon session, one of the groups of its token.
    DWORD size = 0;
    GetTokenInformation(hToken, TokenGroups, NULL, 0, &size);
    std::vector<BYTE> groups(size);
    if ( size == 0 || !GetTokenInformation(hToken, TokenGroups, groups.data(), size, &size) ) {
        return false;
    }
    PTOKEN_GROUPS tokenGroups = (PTOKEN_GROUPS) groups.data();
    for ( DWORD i = 0; i < tokenGroups->GroupCount && sessionSid.empty(); ++ i ) {
        LPWSTR string = NULL;
        if ( (tokenGroups->Groups[i].Attributes & SE_GROUP_LOGON_ID) == SE_GROUP_LOGON_ID &&
                ConvertSidToStringSidW(tokenGroups->Groups[i].Sid, &string) ) {
            sessionSid = string;
            LocalFree(string);
        }
    }
    if ( sessionSid.empty() ) {
        SetLastError(ERROR_NO_SUCH_LOGON_SESSION);
        return false;
    }
    if ( !OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY, &hJudgerToken) ) {
        return false;
    }
    bool  judgerKnown = getTokenUserSid(hJudgerToken, judgerSid);
    DWORD error       = GetLastError();
    CloseHandle(hJudgerToken);
    if ( !judgerKnown ) {
        SetLastError(error);
        return false;
    }
    wchar_t      mask[16];
    swprintf(mask, sizeof(mask) / sizeof(mask[0]), L"0x%lX", (unsigned long) DESKTOP_ACCESS);
    std::wstring sddl = std::wstring(L"D:P(A;;") + mask + L";;;" + sessionSid + L")(A;;GA;;;" +
                        judgerSid + L")(A;;GA;;;SY)" + PRIVATE_DESKTOP_LABEL;
    PSECURITY_DESCRIPTOR pSd = NULL;
    if ( !ConvertStringSecurityDescriptorToSecurityDescriptorW(sddl.c_str(), SDDL_REVISION_1, &pSd,
            NULL) ) {
        return false;
    }
    // The name of the judger's window station, which the desktop is created on.
    wchar_t station[256] = L"";
    DWORD   length       = 0;
    if ( !GetUserObjectInformationW(GetProcessWindowStation(), UOI_NAME, station, sizeof(station),
            &length) ) {
        error = GetLastError();
        LocalFree(pSd);
        SetLastError(error);
        return false;
    }
    std::wstring        name = L"voj-judger-" + std::to_wstring(GetCurrentProcessId()) + L"-" +
                               std::to_wstring(InterlockedIncrement(&counter));
    SECURITY_ATTRIBUTES sa   = { sizeof(SECURITY_ATTRIBUTES), pSd, FALSE };
    // CreateDesktopW opens a desktop that exists already: make sure none has the name.
    HDESK hExisting = OpenDesktopW(name.c_str(), 0, FALSE, READ_CONTROL);
    if ( hExisting != NULL ) {
        CloseDesktop(hExisting);
        LocalFree(pSd);
        SetLastError(ERROR_ALREADY_EXISTS);
        return false;
    }
    hDesktop = CreateDesktopW(name.c_str(), NULL, NULL, 0, GENERIC_ALL, &sa);
    error    = GetLastError();
    LocalFree(pSd);
    if ( hDesktop == NULL ) {
        SetLastError(error);
        return false;
    }
    if ( !ConvertStringSidToSidW(sessionSid.c_str(), &logonSid) || !setWindowStationAccess(logonSid, true) ) {
        error = GetLastError();
        if ( logonSid != NULL ) {
            LocalFree(logonSid);
            logonSid = NULL;
        }
        CloseDesktop(hDesktop);
        hDesktop = NULL;
        SetLastError(error);
        return false;
    }
    desktop = std::wstring(station) + L"\\" + name;
    return true;
}

/**
 * Grants a logon session WINDOW_STATION_ACCESS to the judger's window station, or revokes every
 * access the window station grants it.
 * @param  logonSid - the logon SID of the session
 * @param  grant    - whether to grant (or revoke) the access
 * @return whether the window station's DACL was changed (GetLastError() tells why not)
 */
bool setWindowStationAccess(PSID logonSid, bool grant) {
    HWINSTA              hWinsta = GetProcessWindowStation();
    PACL                 pDacl   = NULL;
    PSECURITY_DESCRIPTOR pSd     = NULL;
    DWORD error = GetSecurityInfo(hWinsta, SE_WINDOW_OBJECT, DACL_SECURITY_INFORMATION, NULL, NULL,
                      &pDacl, NULL, &pSd);
    if ( error == ERROR_SUCCESS ) {
        EXPLICIT_ACCESSW entry = {};
        entry.grfAccessPermissions = grant ? WINDOW_STATION_ACCESS : 0;
        entry.grfAccessMode        = grant ? GRANT_ACCESS : REVOKE_ACCESS;
        entry.grfInheritance       = NO_INHERITANCE;
        entry.Trustee.TrusteeForm  = TRUSTEE_IS_SID;
        entry.Trustee.TrusteeType  = TRUSTEE_IS_GROUP;
        entry.Trustee.ptstrName    = (LPWSTR) logonSid;
        PACL pNewDacl = NULL;
        error = SetEntriesInAclW(1, &entry, pDacl, &pNewDacl);
        if ( error == ERROR_SUCCESS ) {
            error = SetSecurityInfo(hWinsta, SE_WINDOW_OBJECT, DACL_SECURITY_INFORMATION, NULL, NULL,
                        pNewDacl, NULL);
            LocalFree(pNewDacl);
        }
        LocalFree(pSd);
    }
    SetLastError(error);
    return error == ERROR_SUCCESS;
}

/**
 * Gets the SID of the user of a token, as a string.
 * @param  hToken - the token (with TOKEN_QUERY access)
 * @param  sid    - receives the SID, such as "S-1-5-21-...-1001"
 * @return whether it was read (GetLastError() tells why not)
 */
bool getTokenUserSid(HANDLE hToken, std::wstring& sid) {
    BYTE   tokenUser[sizeof(TOKEN_USER) + SECURITY_MAX_SID_SIZE];
    DWORD  size   = 0;
    LPWSTR string = NULL;
    if ( !GetTokenInformation(hToken, TokenUser, tokenUser, sizeof(tokenUser), &size) ||
            !ConvertSidToStringSidW(((PTOKEN_USER) tokenUser)->User.Sid, &string) ) {
        return false;
    }
    sid = string;
    LocalFree(string);
    return true;
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
