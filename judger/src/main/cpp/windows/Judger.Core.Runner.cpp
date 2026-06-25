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
#pragma GCC diagnostic ignored "-Wwrite-strings"
#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0600
#endif

#include <cstdint>
#include <iostream>
#include <sstream>
#include <string>

#include <windows.h>
#include <userenv.h>

#include "../org_verwandlung_voj_jni_hashmap.h"
#include "../org_verwandlung_voj_jni_library.h"
#include "../org_verwandlung_voj_judger_core_Runner.h"

/**
 * The maximum number of processes a sandboxed program (or compiler) may have running at once. This
 * is enforced by the Job Object and primarily blocks fork bombs; it must stay high enough for the
 * compiler's helper-process chain (cc1, as, ld, ...).
 */
#define ACTIVE_PROCESS_LIMIT 64

/**
 * Extra wall-clock time (ms) granted on top of the CPU time limit, so a process that blocks/sleeps
 * without consuming CPU is still terminated.
 */
#define WALL_TIME_BUFFER_MS 1000

/**
 * How often (ms) the parent samples the child's peak memory while it runs. The memory limit is
 * enforced by polling the Job's peak accounting (a soft limit) rather than the Job's hard
 * ProcessMemoryLimit, so that an offending peak rises just above the limit before the process is
 * killed and the run is classified as MLE - matching the Linux runner. The hard cap is intentionally
 * not used: under it an allocation simply fails and the program crashes, which is indistinguishable
 * from a plain runtime error.
 */
#define MEMORY_POLL_INTERVAL_MS 2

/**
 * Function Prototypes.
 */
bool setupIoRedirection(const std::wstring&, const std::wstring&, HANDLE&, HANDLE&);
void setupStartupInfo(STARTUPINFOW&, HANDLE&, HANDLE&);
HANDLE createJobObject();
bool createProcess(const std::wstring&, const std::wstring&, const std::wstring&, HANDLE&, LPVOID&, STARTUPINFOW&, PROCESS_INFORMATION&);
jint getPeakMemoryUsage(HANDLE& hJob);
jint getCpuTimeUsage(HANDLE& hProcess);
DWORD getExitCode(HANDLE& hProcess);
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
 * @param  jOutputFilePath - the output file path after running the program (may be NULL)
 * @param  timeLimit       - the program execution time limit (ms, 0 means no limit)
 * @param  memoryLimit     - the program execution memory limit (KB, 0 means no limit)
 * @return a Map<String, Object> object containing the runtime result
 */
JNIEXPORT jobject JNICALL Java_org_verwandlung_voj_judger_core_Runner_getRuntimeResult(
    JNIEnv* jniEnv, jobject selfReference, jstring jCommandLine, jstring jUsername,
    jstring jPassword, jstring jInputFilePath, jstring jOutputFilePath, jint timeLimit,
    jint memoryLimit) {
    std::wstring        commandLine         = getWideString(getStringValue(jniEnv, jCommandLine));
    std::wstring        username            = getWideString(getStringValue(jniEnv, jUsername));
    std::wstring        password            = getWideString(getStringValue(jniEnv, jPassword));
    std::wstring        inputFilePath       = getWideString(getStringValue(jniEnv, jInputFilePath));
    std::wstring        outputFilePath      = getWideString(getStringValue(jniEnv, jOutputFilePath));

    HANDLE              hInput              = NULL;
    HANDLE              hOutput             = NULL;
    HANDLE              hToken              = NULL;
    HANDLE              hJob                = NULL;
    LPVOID              lpEnvironment       = NULL;
    PROCESS_INFORMATION processInfo         = {0};
    STARTUPINFOW        startupInfo         = {0};

    JHashMap            result;
    jint                timeUsage           = 0;
    jint                memoryUsage         = 0;
    DWORD               exitCode            = 127;

    if ( !setupIoRedirection(inputFilePath, outputFilePath, hInput, hOutput) ) {
        throwStringException(jniEnv, getErrorMessage("SetupIoRedirection"));
    }
    setupStartupInfo(startupInfo, hInput, hOutput);

    // The Job Object enforces the active-process limit (anti fork bomb) and the UI restrictions, and
    // kills the whole process tree atomically when it is closed. The memory limit is enforced below
    // by polling the Job's peak accounting (a soft limit), not the Job's hard ProcessMemoryLimit.
    hJob = createJobObject();

    // The process is created suspended, assigned to the Job Object before it runs a single
    // instruction, and only then resumed - so the limits are in force from the very beginning.
    if ( !createProcess(commandLine, username, password, hToken, lpEnvironment, startupInfo, processInfo) ) {
        throwStringException(jniEnv, getErrorMessage("CreateProcess"));
    }
    if ( hJob != NULL ) {
        AssignProcessToJobObject(hJob, processInfo.hProcess);
    }
    ResumeThread(processInfo.hThread);

    // Poll while the child runs: enforce the memory limit as a soft limit (let the peak rise just
    // above the limit, then kill, so the run is classified as MLE) and apply a wall-clock guard for a
    // process that blocks/sleeps while consuming little CPU.
    DWORD     waitTimeout         = ( timeLimit > 0 ) ? (DWORD)(timeLimit + WALL_TIME_BUFFER_MS) : INFINITE;
    bool      killedByWallTimeout = false;
    bool      memoryExceeded      = false;
    ULONGLONG startTick           = GetTickCount64();
    for ( ;; ) {
        if ( WaitForSingleObject(processInfo.hProcess, MEMORY_POLL_INTERVAL_MS) == WAIT_OBJECT_0 ) {
            break; // the process exited on its own
        }
        if ( memoryLimit > 0 && getPeakMemoryUsage(hJob) > memoryLimit ) {
            memoryExceeded = true;
            if ( hJob != NULL ) {
                TerminateJobObject(hJob, 1);
            } else {
                TerminateProcess(processInfo.hProcess, 1);
            }
            WaitForSingleObject(processInfo.hProcess, INFINITE);
            break;
        }
        if ( waitTimeout != INFINITE && GetTickCount64() - startTick > (ULONGLONG) waitTimeout ) {
            killedByWallTimeout = true;
            if ( hJob != NULL ) {
                TerminateJobObject(hJob, 1);
            } else {
                TerminateProcess(processInfo.hProcess, 1);
            }
            WaitForSingleObject(processInfo.hProcess, INFINITE);
            break;
        }
    }

    exitCode    = getExitCode(processInfo.hProcess);
    timeUsage   = getCpuTimeUsage(processInfo.hProcess);
    memoryUsage = getPeakMemoryUsage(hJob); // the Job's peak accounting persists after the process exits

    // A process killed for exceeding the wall-clock guard consumes little CPU, so force the reported
    // time to the limit, ensuring the Java side classifies it as a Time Limit Exceeded.
    if ( killedByWallTimeout && timeLimit > 0 && timeUsage < timeLimit ) {
        timeUsage = timeLimit;
    }
    // A process killed for exceeding the memory limit must report at least the limit, so the Java
    // side classifies it as a Memory Limit Exceeded.
    if ( memoryExceeded && memoryLimit > 0 && memoryUsage < memoryLimit ) {
        memoryUsage = memoryLimit;
    }

    if ( lpEnvironment != NULL ) {
        DestroyEnvironmentBlock(lpEnvironment);
    }
    if ( hToken != NULL ) {
        CloseHandle(hToken);
    }
    CloseHandle(processInfo.hThread);
    CloseHandle(processInfo.hProcess);
    // Closing the job kills any surviving processes in it (JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE).
    if ( hJob != NULL ) {
        CloseHandle(hJob);
    }
    if ( hInput != NULL ) {
        CloseHandle(hInput);
    }
    if ( hOutput != NULL ) {
        CloseHandle(hOutput);
    }

    result.put("usedTime", timeUsage);
    result.put("usedMemory", memoryUsage);
    result.put("exitCode", (jint)exitCode);

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
        hOutput = CreateFileW(outputFilePath.c_str(), GENERIC_WRITE, FILE_SHARE_READ, &sa,
                    CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
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
 */
void setupStartupInfo(STARTUPINFOW& startupInfo, HANDLE& hInput, HANDLE& hOutput) {
    startupInfo.cb              = sizeof(STARTUPINFOW);
    startupInfo.dwFlags        |= STARTF_USESTDHANDLES;
    startupInfo.hStdInput       = hInput;
    startupInfo.hStdError       = hOutput;
    startupInfo.hStdOutput      = hOutput;
}

/**
 * Creates a Job Object that enforces the active-process limit and UI restrictions and kills the
 * whole tree when closed. No hard memory limit is set: memory is enforced by polling the Job's peak
 * accounting (see the run loop), which the Job tracks regardless of whether a memory limit is set.
 * @return the Job Object handle, or NULL on failure
 */
HANDLE createJobObject() {
    HANDLE hJob = CreateJobObjectW(NULL, NULL);
    if ( hJob == NULL ) {
        return NULL;
    }

    JOBOBJECT_EXTENDED_LIMIT_INFORMATION jeli = {0};
    jeli.BasicLimitInformation.LimitFlags =
        JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE | JOB_OBJECT_LIMIT_DIE_ON_UNHANDLED_EXCEPTION |
        JOB_OBJECT_LIMIT_ACTIVE_PROCESS;
    jeli.BasicLimitInformation.ActiveProcessLimit = ACTIVE_PROCESS_LIMIT;
    SetInformationJobObject(hJob, JobObjectExtendedLimitInformation, &jeli, sizeof(jeli));

    // Cut the sandboxed program off from the interactive desktop (clipboard, global atoms, etc.).
    JOBOBJECT_BASIC_UI_RESTRICTIONS uiRestrictions = {0};
    uiRestrictions.UIRestrictionsClass =
        JOB_OBJECT_UILIMIT_DESKTOP | JOB_OBJECT_UILIMIT_DISPLAYSETTINGS |
        JOB_OBJECT_UILIMIT_EXITWINDOWS | JOB_OBJECT_UILIMIT_GLOBALATOMS |
        JOB_OBJECT_UILIMIT_READCLIPBOARD | JOB_OBJECT_UILIMIT_WRITECLIPBOARD |
        JOB_OBJECT_UILIMIT_SYSTEMPARAMETERS;
    SetInformationJobObject(hJob, JobObjectBasicUIRestrictions, &uiRestrictions, sizeof(uiRestrictions));

    return hJob;
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
 * @param  hToken        - receives a token that represents the specified user
 * @param  lpEnvironment - receives the environment block for the new process
 * @param  startupInfo   - a STARTUPINFO structure
 * @param  processInfo   - receives identification information for the new process
 * @return whether the process was created successfully
 */
bool createProcess(const std::wstring& commandLine, const std::wstring& username,
        const std::wstring& password, HANDLE& hToken, LPVOID& lpEnvironment,
        STARTUPINFOW& startupInfo, PROCESS_INFORMATION& processInfo) {
    WCHAR   szUserProfile[256]  = L"";
    DWORD   dwSize              = sizeof(szUserProfile) / sizeof(WCHAR);
    LPCWSTR lpUsername          = getConstWideStringPointer(username);
    LPCWSTR lpDomain            = L".";
    LPCWSTR lpPassword          = getConstWideStringPointer(password);
    LPWSTR  lpCommandLine       = getWideStringPointer(commandLine);

    if ( !LogonUserW(lpUsername, lpDomain, lpPassword, LOGON32_LOGON_BATCH,
            LOGON32_PROVIDER_DEFAULT, &hToken) ) {
        return false;
    }
    if ( !CreateEnvironmentBlock(&lpEnvironment, hToken, TRUE) ) {
        return false;
    }
    if ( !GetUserProfileDirectoryW(hToken, szUserProfile, &dwSize) ) {
        return false;
    }
    if ( !CreateProcessWithLogonW(lpUsername, lpDomain, lpPassword,
            LOGON_WITH_PROFILE, NULL, lpCommandLine,
            CREATE_UNICODE_ENVIRONMENT | CREATE_SUSPENDED | CREATE_NO_WINDOW,
            lpEnvironment, szUserProfile, &startupInfo, &processInfo) ) {
        return false;
    }
    return true;
}

/**
 * Gets the peak memory usage from the Job Object's accounting.
 * @param  hJob - the Job Object handle
 * @return the peak physical memory usage (KB)
 */
jint getPeakMemoryUsage(HANDLE& hJob) {
    if ( hJob == NULL ) {
        return 0;
    }
    JOBOBJECT_EXTENDED_LIMIT_INFORMATION jeli = {0};
    if ( !QueryInformationJobObject(hJob, JobObjectExtendedLimitInformation,
            &jeli, sizeof(jeli), NULL) ) {
        return 0;
    }
    unsigned long long peakKb = (unsigned long long)jeli.PeakProcessMemoryUsed >> 10;
    if ( peakKb > (unsigned long long)INT32_MAX ) {
        return INT32_MAX;
    }
    return (jint)peakKb;
}

/**
 * Gets the CPU time (user + kernel) used by the process.
 * @param  hProcess - the process handle
 * @return the CPU time used (ms)
 */
jint getCpuTimeUsage(HANDLE& hProcess) {
    FILETIME creationTime, exitTime, kernelTime, userTime;
    if ( !GetProcessTimes(hProcess, &creationTime, &exitTime, &kernelTime, &userTime) ) {
        return 0;
    }
    // FILETIME is measured in 100-nanosecond intervals.
    unsigned long long cpu100ns = fileTimeToUll(kernelTime) + fileTimeToUll(userTime);
    unsigned long long cpuMs = cpu100ns / 10000ULL;
    if ( cpuMs > (unsigned long long)INT32_MAX ) {
        return INT32_MAX;
    }
    return (jint)cpuMs;
}

/**
 * Gets the application's exit status.
 * @param  hProcess - the process handle
 * @return the exit status
 */
DWORD getExitCode(HANDLE& hProcess) {
    DWORD exitCode = 0;
    GetExitCodeProcess(hProcess, &exitCode);
    return exitCode;
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
    LPVOID lpvMessageBuffer;
    DWORD  errorCode = GetLastError();

    FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER |
        FORMAT_MESSAGE_FROM_SYSTEM,
        NULL, errorCode,
        MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
        (LPSTR)&lpvMessageBuffer, 0, NULL);

    std::stringstream stringStream;
    std::string errorMessage((LPSTR)lpvMessageBuffer);

    stringStream << "API:     " << apiName << std::endl
                 << "Code:    " << errorCode << std::endl
                 << "Message: " << errorMessage << std::endl;
    LocalFree(lpvMessageBuffer);

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
