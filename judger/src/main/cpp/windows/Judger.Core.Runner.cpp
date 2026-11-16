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

#include <algorithm>
#include <cstdint>
#include <cwchar>
#include <sstream>
#include <string>
#include <vector>

#include <windows.h>
#include <aclapi.h>
#include <fwpmu.h>
#include <rpc.h>
#include <sddl.h>
#include <userenv.h>
#include <psapi.h>

#include "../org_verwandlung_voj_jni_library.h"
#include "../org_verwandlung_voj_judger_core_Runner.h"

/**
 * How often (ms) the parent checks the program's CPU time, peak memory and output while it runs.
 * The memory limit is enforced on the peak working set by polling, as on Linux (see
 * getPeakMemoryUsage).
 */
#define POLL_INTERVAL_MS 2

/**
 * The limit the sandbox stopped a program for (SandboxResult.Stop): Java judges the verdict.
 */
static const jint STOP_NONE   = 0;
static const jint STOP_TIME   = 1;
static const jint STOP_MEMORY = 2;
static const jint STOP_OUTPUT = 3;

/** The exit code of a run ended for starting a process (STATUS_ACCESS_DENIED). */
static const UINT STARTED_A_PROCESS_EXIT_CODE = 0xC0000022;

/**
 * How long (ms) the runner waits, once the program is gone, for the rest of its Job (what a
 * compiler left running) to be killed and gone, before it gives up on the run.
 */
static const DWORD JOB_EMPTY_TIMEOUT_MS = 5000;

/**
 * The longest command line CreateProcessWithTokenW takes (in characters, the terminating NUL not
 * included). A longer one is refused before the call, with a message that says why.
 */
static const size_t MAX_COMMAND_LINE_LENGTH = 1024;

/**
 * The error mode of every compilation and run: no error box that nobody is there to close (a
 * program missing a DLL ends at once, a runtime error, not a time limit exceeded). Set on the
 * suspended process through NtSetInformationProcess(ProcessDefaultHardErrorMode), where the bit of
 * SEM_FAILCRITICALERRORS means the opposite, so it is left clear.
 */
static const ULONG PROCESS_DEFAULT_HARD_ERROR_MODE = 12;
static const ULONG HARD_ERROR_MODE                 = SEM_NOGPFAULTERRORBOX | SEM_NOOPENFILEERRORBOX;
typedef LONG (NTAPI *NtSetInformationProcessFunction)(HANDLE, ULONG, PVOID, ULONG);
typedef ULONG (NTAPI *RtlNtStatusToDosErrorFunction)(LONG);

/**
 * The integrity level of every compilation and run: Low, the counterpart of Landlock on Linux. A
 * Low process may write only to what is labelled Low ("no write up"), whatever the DACL grants:
 * its work directory (WORK_DIRECTORY_LABEL), not the account's profile or registry, nor the
 * directories every account may write to. What Windows keeps writable for Low processes (LocalLow,
 * AppDataLow) is labelled Medium by raiseLowLabels.
 */
static const wchar_t* const LOW_INTEGRITY_SID     = L"S-1-16-4096";
static const wchar_t* const MEDIUM_INTEGRITY_SID  = L"S-1-16-8192";
static const wchar_t* const MEDIUM_FOLDER_LABEL   = L"S:(ML;OICI;NW;;;ME)";
static const wchar_t* const MEDIUM_KEY_LABEL      = L"S:(ML;CI;NW;;;ME)";
/** The label of a work directory: Low, inherited by what is created in it. */
static const wchar_t* const WORK_DIRECTORY_LABEL  = L"S:(ML;OICI;NW;;;LW)";

/**
 * The private desktop of every compilation and run, on the judger's window station (user32 fails
 * to initialise on any other but WinSta0): its DACL gives the account's logon session
 * DESKTOP_ACCESS, the judger and SYSTEM full control and nobody else anything, and the window
 * station gives the logon session the least user32 needs (WINDOW_STATION_ACCESS), revoked at log
 * off. A program cannot see or send input to the operator's windows.
 */
static const DWORD WINDOW_STATION_ACCESS = READ_CONTROL | WINSTA_ACCESSGLOBALATOMS | WINSTA_EXITWINDOWS;
static const DWORD DESKTOP_ACCESS        = READ_CONTROL | DESKTOP_READOBJECTS | DESKTOP_CREATEWINDOW |
                                           DESKTOP_CREATEMENU | DESKTOP_WRITEOBJECTS;
static const wchar_t* const PRIVATE_DESKTOP_LABEL = L"S:(ML;;NW;;;LW)";

/**
 * The network filter of the sandbox account (see blockNetwork): the layers it blocks, for IPv4 and
 * IPv6, every socket of the account's processes being bound (implicitly too), listening,
 * connecting or accepting, loopback included; and the condition that matches the account. The
 * keys and flags are those of the SDK's fwpmu.h, which MinGW's lacks.
 * See https://learn.microsoft.com/windows/win32/fwp/management-filtering-layer-identifiers-
 */
static const GUID BLOCKED_LAYERS[] = {
    // FWPM_LAYER_ALE_RESOURCE_ASSIGNMENT_V4 and _V6
    { 0x1247d66d, 0x0b60, 0x4a15, { 0x8d, 0x44, 0x71, 0x55, 0xd0, 0xf5, 0x3a, 0x0c } },
    { 0x55a650e1, 0x5f0a, 0x4eca, { 0xa6, 0x53, 0x88, 0xf5, 0x3b, 0x26, 0xaa, 0x8c } },
    // FWPM_LAYER_ALE_AUTH_LISTEN_V4 and _V6
    { 0x88bb5dad, 0x76d7, 0x4227, { 0x9c, 0x71, 0xdf, 0x0a, 0x3e, 0xd7, 0xbe, 0x7e } },
    { 0x7ac9de24, 0x17dd, 0x4814, { 0xb4, 0xbd, 0xa9, 0xfb, 0xc9, 0x5a, 0x32, 0x1b } },
    // FWPM_LAYER_ALE_AUTH_CONNECT_V4 and _V6
    { 0xc38d57d1, 0x05a7, 0x4c33, { 0x90, 0x4f, 0x7f, 0xbc, 0xee, 0xe6, 0x0e, 0x82 } },
    { 0x4a72393b, 0x319f, 0x44bc, { 0x84, 0xc3, 0xba, 0x54, 0xdc, 0xb3, 0xb6, 0xb4 } },
    // FWPM_LAYER_ALE_AUTH_RECV_ACCEPT_V4 and _V6
    { 0xe1cd9fe7, 0xf4b5, 0x4273, { 0x96, 0xc0, 0x59, 0x2e, 0x48, 0x7b, 0x86, 0x50 } },
    { 0xa3b42c97, 0x9f04, 0x4672, { 0xb8, 0x7e, 0xce, 0xe9, 0xc4, 0x83, 0x25, 0x7f } },
};
/** FWPM_CONDITION_ALE_USER_ID. */
static const GUID  ALE_USER_ID_CONDITION      = { 0xaf043a0a, 0xb34d, 0x4f86,
                                                  { 0x97, 0x9c, 0xc9, 0x03, 0x71, 0xaf, 0x6e, 0x66 } };
static const UINT32 SESSION_FLAG_DYNAMIC      = 0x00000001; // FWPM_SESSION_FLAG_DYNAMIC
static const UINT32 FILTER_FLAG_CLEAR_ACTION_RIGHT = 0x00000008; // FWPM_FILTER_FLAG_CLEAR_ACTION_RIGHT

/**
 * The sandbox account's logon session, which every compilation and run uses, from
 * logOnSandboxAccount to logOffSandboxAccount. Guarded by sandboxSessionLock.
 */
struct SandboxSession {
    HANDLE       hMutex;    // Global\voj-sandbox-<SID>: no other judger uses the account
    HANDLE       hEngine;   // the dynamic WFP session that holds the network filter
    HANDLE       hToken;    // the account's logon token
    HANDLE       hProfile;  // its loaded profile (registry hive), unloaded with the session
    HANDLE       hRunToken; // the token every program runs with (see createLowIntegrityToken)
    HDESK        hDesktop;  // the private desktop of its programs (see createPrivateDesktop)
    std::wstring desktop;   // "<window station>\<desktop>", the programs' lpDesktop
    PSID         logonSid;  // the SID of the logon session, granted access to the window station
};
static SandboxSession sandboxSession     = { NULL, NULL, NULL, NULL, NULL, NULL, L"", NULL };
static SRWLOCK        sandboxSessionLock = SRWLOCK_INIT;

/**
 * Function Prototypes.
 */
bool setupIoRedirection(const std::wstring&, const std::wstring&, HANDLE&, HANDLE&);
bool getJudgerOnlySecurityDescriptor(PSECURITY_DESCRIPTOR&);
HANDLE createJobObject(DWORD, jint, bool, HANDLE);
bool drainJobMessages(HANDLE, bool&, DWORD, DWORD&, bool&);
bool isConsoleHost(DWORD);
bool emptyJob(HANDLE, HANDLE, bool&);
bool disableErrorDialogs(HANDLE);
bool createProcess(const std::vector<std::wstring>&, const std::vector<std::wstring>&, const std::wstring&, STARTUPINFOW&, PROCESS_INFORMATION&, std::string&);
std::wstring quoteArguments(const std::vector<std::wstring>&);
void closeSandboxSession();
bool claimSandboxAccount(HANDLE, HANDLE&);
bool blockNetwork(HANDLE, HANDLE&);
HANDLE createLowIntegrityToken(HANDLE);
bool labelLowIntegrity(const std::wstring&);
bool hasLabel(PACL, PSID, BYTE);
bool raiseLowLabels(HANDLE);
bool ensureMediumLabel(const std::wstring&, SE_OBJECT_TYPE, const wchar_t*, BYTE);
bool createPrivateDesktop(HANDLE, HDESK&, std::wstring&, PSID&);
bool setWindowStationAccess(PSID, bool);
bool getTokenUserSid(HANDLE, std::wstring&);

bool getPeakMemoryUsage(HANDLE, jint&);
bool getCpuTimeUsage(HANDLE, jint&);
std::string getErrorMessage(const std::string&);
std::wstring getWideStringValue(JNIEnv*, jstring);
std::vector<std::wstring> getWideStringValues(JNIEnv*, jobjectArray);

/**
 * JNI call entry point.
 * Runs a program in the sandbox and measures it. The command, environment and limits are the
 * judger's (see SandboxPolicy.java); this only enforces them. The program runs with the session's
 * restricted Low-integrity token (see logOnSandboxAccount) in a Job Object, which caps its
 * processes and committed memory, keeps it from the user interface and kills it as a whole, on the
 * session's private desktop.
 * @param  jniEnv          - a reference to the JNI runtime environment
 * @param  selfReference   - a reference to the calling Java object
 * @param  jArguments      - the program (resolved by the judger) and its arguments
 * @param  jEnvironment    - the whole environment of the program ("NAME=value")
 * @param  jWorkDirectory  - the directory the program runs in, and the only one it may write to
 * @param  jInputFile      - the file to read stdin from (may be NULL)
 * @param  jOutputFile     - the file receiving stdout, and stderr for a compilation (a run's is
 *                           discarded). Created by the judger (see setupIoRedirection)
 * @param  compilation     - whether a trusted compiler runs, rather than the submission
 * @param  timeLimit       - the CPU time limit (ms)
 * @param  wallTimeLimit   - the wall-clock limit (ms)
 * @param  memoryLimit     - the limit of the peak working set (KB)
 * @param  memoryBackstop  - the Job's commit limit (KB): of the whole Job for a run, of each
 *                           process for a compilation
 * @param  stackLimit      - not enforced: a program's stack is the one its executable's header
 *                           asks for (SizeOfStackReserve), which only its linker decides
 * @param  outputLimit     - the largest output (KB)
 * @param  processLimit    - the Job's active process limit
 * @param  fileLimit       - not enforced: Windows has no limit on a process's handles to set
 * @param  signalQueueLimit - not enforced: Windows has no signals to queue
 * @return the CPU time (ms), the peak memory (KB), the exit code and the limit the program was
 *         stopped for; throws a Java exception instead if the sandbox could not be set up
 */
JNIEXPORT jintArray JNICALL Java_org_verwandlung_voj_judger_core_Runner_execute(
    JNIEnv* jniEnv, jobject selfReference, jobjectArray jArguments, jobjectArray jEnvironment,
    jstring jWorkDirectory, jstring jInputFile, jstring jOutputFile, jboolean compilation,
    jint timeLimit, jint wallTimeLimit, jint memoryLimit, jint memoryBackstop, jint stackLimit,
    jint outputLimit, jint processLimit, jint fileLimit, jint signalQueueLimit) {
    (void) stackLimit;
    (void) fileLimit;
    (void) signalQueueLimit;
    std::vector<std::wstring> arguments      = getWideStringValues(jniEnv, jArguments);
    std::vector<std::wstring> environment    = getWideStringValues(jniEnv, jEnvironment);
    std::wstring              inputFilePath  = getWideStringValue(jniEnv, jInputFile);
    std::wstring              outputFilePath = getWideStringValue(jniEnv, jOutputFile);
    std::wstring              workDirectory  = getWideStringValue(jniEnv, jWorkDirectory);
    LONGLONG                  outputLimitBytes = (LONGLONG) outputLimit * 1024;

    HANDLE              hInput              = NULL;
    HANDLE              hOutput             = NULL;
    HANDLE              hError              = NULL;
    HANDLE              hJob                = NULL;
    HANDLE              hCompletionPort     = NULL;
    PROCESS_INFORMATION processInfo         = {0};
    STARTUPINFOW        startupInfo         = {0};
    bool                jobInUse            = false; // processes may be (or have been) in the Job
    bool                memoryLimitHit      = false;
    DWORD               consoleHost         = 0;     // the run's console host, once it joined
    bool                otherProcess        = false; // a run started another process

    jint                timeUsage           = 0;
    jint                memoryUsage         = 0;
    DWORD               exitCode            = 127;

    // Releases everything acquired so far. Every early exit below goes through it: a pending Java
    // exception does not stop this function, so each failure must clean up and return explicitly.
    // Whatever is left of the Job's processes is killed, and gone, first.
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
    auto fail = [&](const std::string& apiName) -> jintArray {
        std::string message = getErrorMessage(apiName);
        cleanup();
        throwStringException(jniEnv, message);
        return NULL;
    };

    if ( arguments.empty() ) {
        SetLastError(ERROR_INVALID_PARAMETER);
        return fail("Execute (the command line is empty)");
    }
    if ( !setupIoRedirection(inputFilePath, outputFilePath, hInput, hOutput) ) {
        return fail("SetupIoRedirection");
    }
    // A compilation's diagnostics (stderr) are the compile log, but a submission's stderr must never
    // reach the judged output: discard it.
    if ( compilation ) {
        hError = hOutput;
    } else {
        SECURITY_ATTRIBUTES nulSa = { sizeof(SECURITY_ATTRIBUTES), NULL, TRUE };
        hError = CreateFileW(L"NUL", GENERIC_WRITE, FILE_SHARE_READ | FILE_SHARE_WRITE, &nulSa,
                    OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
        if ( hError == INVALID_HANDLE_VALUE ) {
            hError = NULL;
        }
    }
    startupInfo.cb         = sizeof(STARTUPINFOW);
    startupInfo.dwFlags    = STARTF_USESTDHANDLES;
    startupInfo.hStdInput  = hInput;
    startupInfo.hStdOutput = hOutput;
    startupInfo.hStdError  = hError;

    // The Job's notifications (an allocation refused by the commit limit) arrive on this port.
    hCompletionPort = CreateIoCompletionPort(INVALID_HANDLE_VALUE, NULL, 0, 1);
    if ( hCompletionPort == NULL ) {
        return fail("CreateIoCompletionPort");
    }
    // Without the Job none of the limits holds, so the program is not started without it. A
    // compiler's commit limit is per process (cc1, as, ld, ...), a run's for the whole Job.
    hJob = createJobObject((DWORD) processLimit, memoryBackstop, !compilation, hCompletionPort);
    if ( hJob == NULL ) {
        return fail("CreateJobObject");
    }
    if ( !labelLowIntegrity(workDirectory) ) {
        return fail("LabelLowIntegrity");
    }
    // Created suspended, and assigned to the Job before it runs a single instruction.
    std::string failedApi;
    if ( !createProcess(arguments, environment, workDirectory, startupInfo, processInfo,
            failedApi) ) {
        return fail(failedApi);
    }
    // A process outside the Job would have no limits: kill it while it is still suspended.
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

    // Poll while the program runs, and kill the whole Job as soon as it reaches a limit: CPU time
    // (of every process of the Job), the peak working set or an allocation the commit limit
    // refused, the size of the output file, and wall-clock time; or, for a run, as soon as a
    // process but its console host joins the Job (see drainJobMessages).
    DWORD     runPid     = compilation ? 0 : processInfo.dwProcessId;
    jint      stop       = STOP_NONE;
    ULONGLONG startTick  = GetTickCount64();
    for ( ;; ) {
        DWORD waitResult = WaitForSingleObject(processInfo.hProcess, POLL_INTERVAL_MS);
        if ( waitResult == WAIT_OBJECT_0 ) {
            break; // the process exited on its own
        }
        if ( waitResult == WAIT_FAILED ) {
            return fail("WaitForSingleObject");
        }
        LARGE_INTEGER outputSize = {};
        if ( !getCpuTimeUsage(hJob, timeUsage) ) {
            return fail("GetCpuTimeUsage");
        }
        if ( !drainJobMessages(hCompletionPort, memoryLimitHit, runPid, consoleHost, otherProcess) ) {
            return fail("GetQueuedCompletionStatus");
        }
        if ( !getPeakMemoryUsage(processInfo.hProcess, memoryUsage) ) {
            return fail("GetPeakMemoryUsage");
        }
        if ( otherProcess ) {
            // Not a limit: the run is ended as a program that may not do this (a runtime error).
            if ( !TerminateJobObject(hJob, STARTED_A_PROCESS_EXIT_CODE) ||
                 WaitForSingleObject(processInfo.hProcess, INFINITE) != WAIT_OBJECT_0 ) {
                return fail("TerminateJobObject");
            }
            break;
        }
        if ( !GetFileSizeEx(hOutput, &outputSize) ) {
            return fail("GetFileSizeEx");
        }
        if ( timeUsage > timeLimit || GetTickCount64() - startTick > (ULONGLONG) wallTimeLimit ) {
            stop = STOP_TIME;
        } else if ( memoryLimitHit || memoryUsage > memoryLimit ) {
            stop = STOP_MEMORY;
        } else if ( outputSize.QuadPart >= outputLimitBytes ) {
            stop = STOP_OUTPUT;
        }
        if ( stop != STOP_NONE ) {
            if ( !TerminateJobObject(hJob, 1) && !TerminateProcess(processInfo.hProcess, 1) ) {
                return fail("TerminateJobObject");
            }
            if ( WaitForSingleObject(processInfo.hProcess, INFINITE) != WAIT_OBJECT_0 ) {
                return fail("WaitForSingleObject");
            }
            break;
        }
    }

    if ( !GetExitCodeProcess(processInfo.hProcess, &exitCode) ) {
        return fail("GetExitCodeProcess");
    }
    // The figures persist after the process exits (its handle is still open). A refused allocation
    // is posted before the allocating call returns, so it is queued by now.
    if ( !getCpuTimeUsage(hJob, timeUsage) ) {
        return fail("GetCpuTimeUsage");
    }
    if ( !getPeakMemoryUsage(processInfo.hProcess, memoryUsage) ) {
        return fail("GetPeakMemoryUsage");
    }
    if ( !drainJobMessages(hCompletionPort, memoryLimitHit, runPid, consoleHost, otherProcess) ) {
        return fail("GetQueuedCompletionStatus");
    }
    if ( stop == STOP_NONE && memoryLimitHit ) {
        stop = STOP_MEMORY;
    }

    // Every process of the Job (what a compiler left running) is killed, and gone, before the
    // judger removes the work directory, which a process that still had a file of it open would
    // keep it from doing.
    if ( !emptyJob(hJob, hCompletionPort, memoryLimitHit) ) {
        std::string message = getErrorMessage("EmptyJob");
        jobInUse = false;
        cleanup();
        throwStringException(jniEnv, message);
        return NULL;
    }
    jobInUse = false;
    cleanup();

    jint      values[] = { timeUsage, memoryUsage, (jint) exitCode, stop };
    jintArray result   = jniEnv->NewIntArray(4);
    if ( result != NULL ) {
        jniEnv->SetIntArrayRegion(result, 0, 4, values);
    }
    return result;
}

/**
 * JNI call entry point.
 * Logs the sandbox account on: the logon session every compilation and run uses, until
 * logOffSandboxAccount. It claims the account for this judger (see claimSandboxAccount), keeps the
 * account's processes off the network (see blockNetwork), loads the account's profile (programs
 * expect HKCU; the JVM finds user.home there) with the logon token, as the run token cannot create
 * the profile of an account that has none yet, and creates the token and the desktop every program
 * gets. The password is not kept.
 * @param  jniEnv    - a reference to the JNI runtime environment
 * @param  jClass    - the calling Java class
 * @param  jUsername - the name of the account
 * @param  jPassword - its password
 */
JNIEXPORT void JNICALL Java_org_verwandlung_voj_judger_core_Runner_logOnSandboxAccount(
    JNIEnv* jniEnv, jclass jClass, jstring jUsername, jstring jPassword) {
    std::wstring username = getWideStringValue(jniEnv, jUsername);
    std::wstring password = getWideStringValue(jniEnv, jPassword);
    AcquireSRWLockExclusive(&sandboxSessionLock);
    SandboxSession& session = sandboxSession;
    closeSandboxSession();
    std::string  failedApi;
    PROFILEINFOW profileInfo = {0};
    profileInfo.dwSize     = sizeof(profileInfo);
    profileInfo.dwFlags    = PI_NOUI;
    profileInfo.lpUserName = &username[0];
    if ( !LogonUserW(username.c_str(), L".", password.c_str(), LOGON32_LOGON_BATCH,
            LOGON32_PROVIDER_DEFAULT, &session.hToken) ) {
        session.hToken = NULL;
        failedApi      = "LogonUser";
    } else if ( !claimSandboxAccount(session.hToken, session.hMutex) ) {
        failedApi = "ClaimSandboxAccount (another judger on this host runs its programs as " +
                    std::string("the account: give each judger an account of its own)");
    } else if ( !blockNetwork(session.hToken, session.hEngine) ) {
        failedApi = "BlockNetwork";
    } else if ( !LoadUserProfileW(session.hToken, &profileInfo) ) {
        failedApi = "LoadUserProfile";
    } else {
        session.hProfile = profileInfo.hProfile;
        if ( !raiseLowLabels(session.hToken) ) {
            failedApi = "RaiseLowLabels";
        } else if ( (session.hRunToken = createLowIntegrityToken(session.hToken)) == NULL ) {
            failedApi = "CreateLowIntegrityToken";
        } else if ( !createPrivateDesktop(session.hToken, session.hDesktop, session.desktop,
                        session.logonSid) ) {
            failedApi = "CreatePrivateDesktop";
        }
    }
    std::string message = failedApi.empty() ? "" : getErrorMessage(failedApi);
    if ( !failedApi.empty() ) {
        closeSandboxSession();
    }
    SecureZeroMemory(&password[0], password.size() * sizeof(wchar_t));
    ReleaseSRWLockExclusive(&sandboxSessionLock);
    if ( !message.empty() ) {
        throwStringException(jniEnv, message);
    }
}

/**
 * JNI call entry point.
 * Logs the sandbox account off, if it is logged on (see closeSandboxSession). Called as the
 * judger exits.
 * @param  jniEnv - a reference to the JNI runtime environment
 * @param  jClass - the calling Java class
 */
JNIEXPORT void JNICALL Java_org_verwandlung_voj_judger_core_Runner_logOffSandboxAccount(
    JNIEnv* jniEnv, jclass jClass) {
    AcquireSRWLockExclusive(&sandboxSessionLock);
    closeSandboxSession();
    ReleaseSRWLockExclusive(&sandboxSessionLock);
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
 * Creates the Job Object of a program: it caps the active processes and the committed memory, cuts
 * the program off from the user interface, reports its events to a completion port, and kills the
 * whole tree when closed.
 * @param  activeProcessLimit - the maximum number of processes that may be active in the Job
 * @param  memoryBackstop     - the commit limit (KB). An allocation beyond it fails,
 *                              and the Job posts JOB_OBJECT_MSG_(JOB|PROCESS)_MEMORY_LIMIT, which
 *                              stops the program for its memory
 * @param  jobWideMemory      - whether the limit applies to all processes together (a run) or to
 *                              each process (a compilation)
 * @param  hCompletionPort    - the completion port that receives the Job's messages
 * @return the Job Object handle, or NULL on failure (GetLastError() tells why)
 */
HANDLE createJobObject(DWORD activeProcessLimit, jint memoryBackstop, bool jobWideMemory,
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

    // Associated before any process joins: every message of the Job is posted to the port.
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
        JOB_OBJECT_LIMIT_ACTIVE_PROCESS |
        (jobWideMemory ? JOB_OBJECT_LIMIT_JOB_MEMORY : JOB_OBJECT_LIMIT_PROCESS_MEMORY);
    jeli.BasicLimitInformation.ActiveProcessLimit = activeProcessLimit;
    unsigned long long hardLimitBytes = (unsigned long long) memoryBackstop * 1024ULL;
    SIZE_T             hardLimit      = ( hardLimitBytes > (unsigned long long) SIZE_MAX )
                                      ? SIZE_MAX : (SIZE_T) hardLimitBytes;
    if ( jobWideMemory ) {
        jeli.JobMemoryLimit = hardLimit;
    } else {
        jeli.ProcessMemoryLimit = hardLimit;
    }
    if ( !SetInformationJobObject(hJob, JobObjectExtendedLimitInformation, &jeli, sizeof(jeli)) ) {
        return failJob();
    }

    // No clipboard, global atoms, other desktops, display or system settings, logging off, nor the
    // USER objects of processes outside the Job, on top of the private desktop.
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
 * Takes every message the Job has posted so far off its completion port: whether the commit limit
 * refused an allocation, and, for a run, whether a process but the program and its console host
 * joined the Job. A run is a single process, but its Job must hold its console host too (since
 * Windows 8 a console program starts it as it initialises, and CreateProcessWithTokenW cannot
 * start one without a console), so it holds two: a run that frees its console could otherwise
 * start a process in the console host's place, whose memory would go unmeasured.
 * See https://learn.microsoft.com/windows/win32/api/winnt/ns-winnt-jobobject_associate_completion_port
 * @param  hCompletionPort - the Job's completion port
 * @param  memoryLimitHit  - set when an allocation was refused by the Job's commit limit
 * @param  runPid          - the program's process id for a run (0 for a compilation, which may
 *                           start processes)
 * @param  consoleHost     - the run's console host, once it joined the Job
 * @param  otherProcess    - set when any other process joined the Job of a run
 * @return whether the port could be read (GetLastError() tells why not)
 */
bool drainJobMessages(HANDLE hCompletionPort, bool& memoryLimitHit, DWORD runPid,
        DWORD& consoleHost, bool& otherProcess) {
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
        // For a process message, lpOverlapped carries the process id.
        DWORD pid = (DWORD) (ULONG_PTR) overlapped;
        if ( message == JOB_OBJECT_MSG_NEW_PROCESS && runPid != 0 && pid != runPid ) {
            if ( consoleHost == 0 && isConsoleHost(pid) ) {
                consoleHost = pid;
            } else {
                otherProcess = true;
            }
        }
    }
}

/**
 * Tells whether a process is the system's console host (%SystemRoot%\System32\conhost.exe).
 * @param  pid - the process id
 * @return whether it is, false if it cannot be told (e.g. it is gone already)
 */
bool isConsoleHost(DWORD pid) {
    wchar_t system[MAX_PATH];
    wchar_t image[MAX_PATH];
    DWORD   length  = MAX_PATH;
    UINT    size    = GetSystemDirectoryW(system, MAX_PATH);
    HANDLE  process = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, FALSE, pid);
    bool    known   = process != NULL && QueryFullProcessImageNameW(process, 0, image, &length);
    if ( process != NULL ) {
        CloseHandle(process);
    }
    std::wstring expected = std::wstring(system, size) + L"\\conhost.exe";
    return known && size > 0 && size < MAX_PATH &&
           CompareStringOrdinal(image, (int) length, expected.c_str(), (int) expected.size(),
                                TRUE) == CSTR_EQUAL;
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
 * Creates the program's process, suspended, with the session's run token (see
 * logOnSandboxAccount), on its private desktop, with a console without a window.
 * CreateProcessWithTokenW needs the judger to hold SeImpersonatePrivilege (an elevated
 * administrator does); CreateProcessWithLogonW, which needs no privilege, takes no token whose
 * integrity level could be lowered.
 * @param  arguments     - the program and its arguments
 * @param  environment   - the environment of the program ("NAME=value")
 * @param  workDirectory - the directory the process starts in
 * @param  startupInfo   - its STARTUPINFO, with its standard handles
 * @param  processInfo   - receives identification information for the new process
 * @param  failedApi     - receives the name of the call that failed, if one did
 * @return whether the process was created (GetLastError() tells why not)
 */
bool createProcess(const std::vector<std::wstring>& arguments,
        const std::vector<std::wstring>& environment, const std::wstring& workDirectory,
        STARTUPINFOW& startupInfo, PROCESS_INFORMATION& processInfo, std::string& failedApi) {
    std::wstring commandLine = quoteArguments(arguments);
    if ( commandLine.size() > MAX_COMMAND_LINE_LENGTH ) {
        failedApi = "CreateProcessWithToken (the command line is longer than its limit of " +
                    std::to_string(MAX_COMMAND_LINE_LENGTH) + " characters)";
        SetLastError(ERROR_INVALID_PARAMETER);
        return false;
    }
    // An environment block is sorted by name, case-insensitively, in Unicode order.
    std::vector<std::wstring> variables = environment;
    std::sort(variables.begin(), variables.end(), [](const std::wstring& a, const std::wstring& b) {
        size_t nameA = a.find(L'=');
        size_t nameB = b.find(L'=');
        return CompareStringOrdinal(a.c_str(), (int) nameA, b.c_str(), (int) nameB, TRUE) ==
               CSTR_LESS_THAN;
    });
    std::vector<wchar_t> environmentBlock;
    for ( const std::wstring& variable : variables ) {
        environmentBlock.insert(environmentBlock.end(), variable.begin(), variable.end());
        environmentBlock.push_back(L'\0');
    }
    if ( environmentBlock.empty() ) {
        environmentBlock.push_back(L'\0');
    }
    environmentBlock.push_back(L'\0');
    std::wstring currentDirectory = workDirectory;
    std::replace(currentDirectory.begin(), currentDirectory.end(), L'/', L'\\');
    // CreateProcessWithTokenW may write to the command line, which is therefore copied.
    std::vector<wchar_t> mutableCommandLine(commandLine.begin(), commandLine.end());
    mutableCommandLine.push_back(L'\0');

    AcquireSRWLockShared(&sandboxSessionLock);
    bool created = false;
    if ( sandboxSession.hRunToken == NULL ) {
        failedApi = "CreateProcessWithToken (the sandbox account is not logged on)";
        SetLastError(ERROR_NOT_LOGGED_ON);
    } else {
        std::wstring desktop  = sandboxSession.desktop;
        startupInfo.lpDesktop = &desktop[0];
        created = CreateProcessWithTokenW(sandboxSession.hRunToken, 0, arguments[0].c_str(),
                      mutableCommandLine.data(),
                      CREATE_UNICODE_ENVIRONMENT | CREATE_SUSPENDED | CREATE_NO_WINDOW,
                      environmentBlock.data(), currentDirectory.c_str(), &startupInfo,
                      &processInfo) != FALSE;
        startupInfo.lpDesktop = NULL;
        failedApi = "CreateProcessWithToken";
    }
    DWORD error = GetLastError();
    ReleaseSRWLockShared(&sandboxSessionLock);
    SetLastError(error);
    return created;
}

/**
 * Builds a command line that the C runtime (CommandLineToArgvW) splits back into the arguments:
 * an argument with a space, a tab or a quote is quoted, with its quotes, and the backslashes
 * before them or before the closing quote, escaped.
 * @param  arguments - the arguments
 * @return the command line
 */
std::wstring quoteArguments(const std::vector<std::wstring>& arguments) {
    std::wstring commandLine;
    for ( const std::wstring& argument : arguments ) {
        if ( !commandLine.empty() ) {
            commandLine += L' ';
        }
        if ( !argument.empty() && argument.find_first_of(L" \t\n\v\"") == std::wstring::npos ) {
            commandLine += argument;
            continue;
        }
        commandLine += L'"';
        size_t backslashes = 0;
        for ( wchar_t c : argument ) {
            if ( c == L'\\' ) {
                ++ backslashes;
                continue;
            }
            commandLine.append(c == L'"' ? backslashes * 2 + 1 : backslashes, L'\\');
            commandLine += c;
            backslashes = 0;
        }
        commandLine.append(backslashes * 2, L'\\');
        commandLine += L'"';
    }
    return commandLine;
}

/**
 * Claims the sandbox account for this judger with a named mutex keyed by its SID, which every
 * judger on the host sees: two judgers whose programs ran as one account could open, kill or
 * inject into each other's programs.
 * @param  hToken - the logon token of the sandbox account
 * @param  hMutex - receives the mutex, held until the session is closed
 * @return whether the account is this judger's (GetLastError() is ERROR_ALREADY_EXISTS if another
 *         judger holds it)
 */
bool claimSandboxAccount(HANDLE hToken, HANDLE& hMutex) {
    std::wstring sid;
    if ( !getTokenUserSid(hToken, sid) ) {
        return false;
    }
    hMutex = CreateMutexW(NULL, FALSE, (L"Global\\voj-sandbox-" + sid).c_str());
    if ( hMutex != NULL && GetLastError() == ERROR_ALREADY_EXISTS ) {
        CloseHandle(hMutex);
        hMutex = NULL;
        SetLastError(ERROR_ALREADY_EXISTS);
    }
    return hMutex != NULL;
}

/**
 * Keeps every process of the sandbox account off the network, loopback included, the counterpart
 * of the seccomp filters on Linux: block filters, at every ALE layer (BLOCKED_LAYERS), of a dynamic
 * WFP session, which Windows removes with the session when the judger exits, however it exits.
 * The filters match the account's SID (FWPM_CONDITION_ALE_USER_ID), and are hard: no permit of the
 * firewall overrides them. A service that acts on a program's behalf, such as the DNS client, is
 * not the account, and not blocked (see docs/deployment.md).
 * See https://learn.microsoft.com/windows/win32/fwp/filter-arbitration
 * @param  hToken  - the logon token of the sandbox account
 * @param  hEngine - receives the WFP session, held until the session is closed
 * @return whether every filter is in place (GetLastError() tells why not)
 */
bool blockNetwork(HANDLE hToken, HANDLE& hEngine) {
    std::wstring sid;
    if ( !getTokenUserSid(hToken, sid) ) {
        return false;
    }
    // Matches a token of the account: the descriptor grants it FWP_ACTRL_MATCH_FILTER (CC).
    std::wstring         sddl = L"O:SYG:SYD:(A;;CC;;;" + sid + L")";
    PSECURITY_DESCRIPTOR pSd  = NULL;
    ULONG                size = 0;
    if ( !ConvertStringSecurityDescriptorToSecurityDescriptorW(sddl.c_str(), SDDL_REVISION_1, &pSd,
            &size) ) {
        return false;
    }
    FWPM_SESSION0 session = {};
    session.flags = SESSION_FLAG_DYNAMIC;
    DWORD error = FwpmEngineOpen0(NULL, RPC_C_AUTHN_WINNT, NULL, &session, &hEngine);
    if ( error != ERROR_SUCCESS ) {
        hEngine = NULL;
    }
    FWPM_SUBLAYER0 sublayer = {};
    if ( error == ERROR_SUCCESS ) {
        error = UuidCreate(&sublayer.subLayerKey) == RPC_S_OK ? ERROR_SUCCESS : ERROR_GEN_FAILURE;
    }
    if ( error == ERROR_SUCCESS ) {
        sublayer.displayData.name = const_cast<wchar_t*>(L"voj-judger sandbox");
        sublayer.weight           = 0xFFFF;
        error = FwpmSubLayerAdd0(hEngine, &sublayer, NULL);
    }
    FWP_BYTE_BLOB          blob      = { size, (UINT8*) pSd };
    FWPM_FILTER_CONDITION0 condition = {};
    condition.fieldKey                = ALE_USER_ID_CONDITION;
    condition.matchType               = FWP_MATCH_EQUAL;
    condition.conditionValue.type     = FWP_SECURITY_DESCRIPTOR_TYPE;
    condition.conditionValue.sd       = &blob;
    for ( const GUID& layer : BLOCKED_LAYERS ) {
        if ( error != ERROR_SUCCESS ) {
            break;
        }
        FWPM_FILTER0 filter = {};
        filter.displayData.name    = const_cast<wchar_t*>(L"voj-judger: no network for the sandbox account");
        filter.flags               = FILTER_FLAG_CLEAR_ACTION_RIGHT;
        filter.layerKey            = layer;
        filter.subLayerKey         = sublayer.subLayerKey;
        filter.weight.type         = FWP_UINT8;
        filter.weight.uint8        = 15;
        filter.numFilterConditions = 1;
        filter.filterCondition     = &condition;
        filter.action.type         = FWP_ACTION_BLOCK;
        error = FwpmFilterAdd0(hEngine, &filter, NULL, NULL);
    }
    LocalFree(pSd);
    if ( error != ERROR_SUCCESS && hEngine != NULL ) {
        FwpmEngineClose0(hEngine);
        hEngine = NULL;
    }
    SetLastError(error);
    return error == ERROR_SUCCESS;
}

/**
 * Closes the sandbox account's logon session, if it is open: unloads its profile, closes its tokens
 * and desktop, revokes the access to the window station, removes the network filter and gives the
 * account up. Called with sandboxSessionLock held exclusively, when no program of the session runs.
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
    if ( session.hEngine != NULL ) {
        FwpmEngineClose0(session.hEngine);
        session.hEngine = NULL;
    }
    if ( session.hMutex != NULL ) {
        CloseHandle(session.hMutex);
        session.hMutex = NULL;
    }
    if ( session.hToken != NULL ) {
        CloseHandle(session.hToken);
        session.hToken = NULL;
    }
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
 * the judger's window station, and lets the account's logon session use the window station. The
 * desktop's DACL gives that logon session DESKTOP_ACCESS, the judger and SYSTEM full control, and
 * nobody else any access. CreateDesktopW opens a desktop of that name that exists already, with
 * the DACL it has: the name is random, so nobody can have created it first.
 * @param  hToken   - the logon token of the sandbox account
 * @param  hDesktop - receives the desktop
 * @param  desktop  - receives the desktop's full name, "<window station>\<desktop>"
 * @param  logonSid - receives the SID of the account's logon session, which the window station now
 *                    grants access to (to be freed with LocalFree once revoked)
 * @return whether the desktop was created and the window station's access granted (GetLastError()
 *         tells why not)
 */
bool createPrivateDesktop(HANDLE hToken, HDESK& hDesktop, std::wstring& desktop, PSID& logonSid) {
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
    UUID     uuid;
    RPC_WSTR uuidString = NULL;
    if ( UuidCreate(&uuid) != RPC_S_OK || UuidToStringW(&uuid, &uuidString) != RPC_S_OK ) {
        LocalFree(pSd);
        SetLastError(ERROR_GEN_FAILURE);
        return false;
    }
    std::wstring name = L"voj-judger-" + std::wstring((wchar_t*) uuidString);
    RpcStringFreeW(&uuidString);
    SECURITY_ATTRIBUTES sa = { sizeof(SECURITY_ATTRIBUTES), pSd, FALSE };
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
 * Gets the peak memory usage: the peak working set (resident memory) of the program's process,
 * the counterpart of the VmHWM the Linux runner measures (a compilation: of its top-level process,
 * as on Linux). Memory committed but never touched (a JVM's initial heap) does not count. A program
 * can trim its own working set, which keeps its peak below what it touched: that is bounded only by
 * the Job's commit limit (see docs/deployment.md).
 * @param  hProcess - the program's process
 * @param  memory   - receives the peak memory usage (KB)
 * @return whether the memory could be measured (GetLastError() tells why not)
 */
bool getPeakMemoryUsage(HANDLE hProcess, jint& memory) {
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
 * Gets the CPU time (user + kernel) of every process that ran in the Job, the running ones
 * included, as wait4 counts a compilation's on Linux.
 * @param  hJob    - the Job Object handle
 * @param  cpuTime - receives the CPU time used (ms)
 * @return whether the CPU time could be measured (GetLastError() tells why not)
 */
bool getCpuTimeUsage(HANDLE hJob, jint& cpuTime) {
    JOBOBJECT_BASIC_ACCOUNTING_INFORMATION accounting = {0};
    if ( !QueryInformationJobObject(hJob, JobObjectBasicAccountingInformation,
            &accounting, sizeof(accounting), NULL) ) {
        return false;
    }
    // In 100-nanosecond intervals.
    unsigned long long cpuMs = ((unsigned long long) accounting.TotalUserTime.QuadPart +
                                (unsigned long long) accounting.TotalKernelTime.QuadPart) / 10000ULL;
    cpuTime = ( cpuMs > (unsigned long long)INT32_MAX ) ? INT32_MAX : (jint)cpuMs;
    return true;
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

/**
 * Gets the values of a Java String[] as wide strings (see getWideStringValue).
 * @param  jniEnv - a reference to the JNI runtime environment
 * @param  jArray - the array (may be null)
 * @return its values
 */
std::vector<std::wstring> getWideStringValues(JNIEnv* jniEnv, jobjectArray jArray) {
    std::vector<std::wstring> values;
    jsize length = jArray == NULL ? 0 : jniEnv->GetArrayLength(jArray);
    for ( jsize i = 0; i < length; ++ i ) {
        jstring jValue = (jstring) jniEnv->GetObjectArrayElement(jArray, i);
        values.push_back(getWideStringValue(jniEnv, jValue));
        jniEnv->DeleteLocalRef(jValue);
    }
    return values;
}
