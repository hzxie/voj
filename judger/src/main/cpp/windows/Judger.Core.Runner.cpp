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
#define INT_MAX 2147483647

#include <cstdint> 
#include <future>
#include <iostream>
#include <limits>
#include <sstream>
#include <string>
#include <thread>

#include <windows.h>
#include <userenv.h>
#include <psapi.h>
#include <tlhelp32.h>

#include "../org_verwandlung_voj_jni_hashmap.h"
#include "../org_verwandlung_voj_jni_library.h"
#include "../org_verwandlung_voj_judger_core_Runner.h"

/**
 * Function Prototypes.
 */
bool setupIoRedirection(std::wstring, std::wstring, HANDLE&, HANDLE&);
void setupStartupInfo(STARTUPINFOW&, HANDLE&, HANDLE&);
bool createProcess(const std::wstring&, const std::wstring&, const std::wstring&, HANDLE&, LPVOID, STARTUPINFOW&, PROCESS_INFORMATION&);
DWORD runProcess(PROCESS_INFORMATION&, jint, jint, jint&, jint&);
jint getMaxMemoryUsage(PROCESS_INFORMATION&, jint);
jint getCurrentMemoryUsage(HANDLE&);
long long getMillisecondsNow();
bool killProcess(PROCESS_INFORMATION&);
DWORD getExitCode(HANDLE&);
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

    HANDLE              hInput              = {0};
    HANDLE              hOutput             = {0};
    HANDLE              hToken              = {0};
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

    if ( !createProcess(commandLine, username, password, hToken, lpEnvironment, startupInfo, processInfo) ) {
        throwStringException(jniEnv, getErrorMessage("CreateProcess"));
    }

    exitCode = runProcess(processInfo, timeLimit, memoryLimit, timeUsage, memoryUsage);
    CloseHandle(hInput);
    CloseHandle(hOutput);

    result.put("usedTime", timeUsage);
    result.put("usedMemory", memoryUsage);
    result.put("exitCode", exitCode);

    return result.toJObject(jniEnv);
}

/**
 * Redirects the child process's I/O.
 * @param inputFilePath  - the input file path
 * @param outputFilePath - the output file path
 * @param hInput         - the input file handle
 * @param hOutput        - the output file handle
 */
bool setupIoRedirection(std::wstring inputFilePath, std::wstring outputFilePath, 
        HANDLE& hInput, HANDLE& hOutput) {
    SECURITY_ATTRIBUTES sa;
    sa.nLength                  = sizeof(sa);
    sa.lpSecurityDescriptor     = NULL;
    sa.bInheritHandle           = TRUE;

    if ( !inputFilePath.empty() ) {
        hInput = CreateFileW(inputFilePath.c_str(), GENERIC_READ, 0, &sa,
                    OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
        if ( hInput == INVALID_HANDLE_VALUE )  {
            return false;
        }
    }
    if ( !outputFilePath.empty() ) {
        hOutput = CreateFileW(outputFilePath.c_str(), GENERIC_WRITE, 0, &sa,
                    CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
        if ( hInput == INVALID_HANDLE_VALUE )  {
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
 * Creates a process.
 * @param  commandLine   - the command line to execute
 * @param  username      - the Windows username
 * @param  password      - the Windows password
 * @param  hToken        - a token that represents the specified user
 * @param  lpEnvironment - an environment block for the new process
 * @param  startupInfo   - a STARTUPINFO structure
 * @param  processInfo   - a PROCESS_INFORMATION structure that receives identification
 *                         information for the new process, including a handle to the process
 * @return whether the process was created successfully
 */
bool createProcess(const std::wstring& commandLine, const std::wstring& username, 
        const std::wstring& password, HANDLE& hToken, LPVOID lpEnvironment,  
        STARTUPINFOW& startupInfo, PROCESS_INFORMATION& processInfo) {
    WCHAR   szUserProfile[256]  = L"";
    DWORD   dwSize              = sizeof(szUserProfile) / sizeof(WCHAR);
    LPCWSTR lpUsername          = getConstWideStringPointer(username);
    LPCWSTR lpDomain            = getConstWideStringPointer(L".");
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
 * Runs the process.
 * @param  processInfo - the PROCESS_INFORMATION structure containing the process information
 * @param  timeLimit   - the runtime time limit (ms)
 * @param  memoryLimit - the runtime memory limit (KB)
 * @param  timeUsage   - the runtime time used (ms)
 * @param  memoryUsage - the runtime memory used (KB)
 * @return the process exit status
 */
DWORD runProcess(PROCESS_INFORMATION& processInfo, jint timeLimit, 
    jint memoryLimit, jint& timeUsage, jint& memoryUsage) {
    int  reservedTime = 200;
    auto feature      = std::async(std::launch::async, getMaxMemoryUsage, std::ref(processInfo), memoryLimit);

    ResumeThread(processInfo.hThread);
    long long startTime = getMillisecondsNow();
    WaitForSingleObject(processInfo.hProcess, timeLimit + reservedTime);
    long long endTime = getMillisecondsNow();
    timeUsage = endTime - startTime;

    if ( getExitCode(processInfo.hProcess) == STILL_ACTIVE ) {
        killProcess(processInfo);
    }
    memoryUsage  = feature.get();

    return getExitCode(processInfo.hProcess);
}

/**
 * Gets the maximum runtime memory usage.
 * @param  processInfo - the PROCESS_INFORMATION structure containing the process information
 * @param  memoryLimit - the runtime memory limit (KB)
 * @return the maximum runtime memory usage
 */
jint getMaxMemoryUsage(PROCESS_INFORMATION& processInfo, jint memoryLimit) {
    jint maxMemoryUsage     = 0,
         currentMemoryUsage = 0;
    do {
        currentMemoryUsage = getCurrentMemoryUsage(processInfo.hProcess);
        if ( currentMemoryUsage > maxMemoryUsage ) {
            maxMemoryUsage = currentMemoryUsage;
        }
        if ( memoryLimit != 0 && currentMemoryUsage > memoryLimit ) {
            killProcess(processInfo);
        }
        Sleep(200);
    } while ( getExitCode(processInfo.hProcess) == STILL_ACTIVE );

    return maxMemoryUsage;
}

/**
 * Gets the memory usage.
 * @param  hProcess - the process handle
 * @return the current physical memory usage (KB)
 */
jint getCurrentMemoryUsage(HANDLE& hProcess) {
    PROCESS_MEMORY_COUNTERS pmc;
    jint  currentMemoryUsage = 0;

    if ( !GetProcessMemoryInfo(hProcess, &pmc, sizeof(pmc)) ) {
        return 0;
    }
    currentMemoryUsage = pmc.PeakWorkingSetSize >> 10;

    if ( currentMemoryUsage < 0 ) {
        currentMemoryUsage = INT_MAX >> 10;
    }
    return currentMemoryUsage;
}

/**
 * Gets the current system time.
 * Used to measure the program's running time.
 * @return the current system time (in milliseconds)
 */
long long getMillisecondsNow() {
    static LARGE_INTEGER frequency;
    static BOOL useQpf = QueryPerformanceFrequency(&frequency);
    if ( useQpf ) {
        LARGE_INTEGER now;
        QueryPerformanceCounter(&now);
        return (1000LL * now.QuadPart) / frequency.QuadPart;
    } else {
        return GetTickCount();
    }
}

/**
 * Forcibly destroys the process (when a threshold is triggered).
 * @param  processInfo - the PROCESS_INFORMATION structure containing the process information
 * @return whether the process destruction completed successfully
 */
bool killProcess(PROCESS_INFORMATION& processInfo) {
    DWORD           processId   = processInfo.dwProcessId;
    PROCESSENTRY32 processEntry = {0};
    processEntry.dwSize         = sizeof(PROCESSENTRY32);
    HANDLE handleSnap           = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    
    if ( Process32First(handleSnap, &processEntry) ) {
        BOOL isContinue = TRUE;

        do {
            if ( processEntry.th32ParentProcessID == processId ) {
                HANDLE hChildProcess = OpenProcess(PROCESS_ALL_ACCESS, FALSE, processEntry.th32ProcessID);
                if ( hChildProcess ) {
                    TerminateProcess(hChildProcess, 1);
                    CloseHandle(hChildProcess);
                }
            }
            isContinue = Process32Next(handleSnap, &processEntry);
        } while ( isContinue );
        
        HANDLE hBaseProcess = OpenProcess(PROCESS_ALL_ACCESS, FALSE, processId);
        if ( hBaseProcess ) {
            TerminateProcess(hBaseProcess, 1);
            CloseHandle(hBaseProcess);
        }
    }

    if ( getExitCode(processInfo.hProcess) == STILL_ACTIVE )  {
        return false;
    }
    return true;
}

/**
 * Gets the application's exit status.
 * 0 means a normal exit, 259 means still running.
 * @param  hProcess - the process handle
 * @return the exit status
 */
DWORD getExitCode(HANDLE& hProcess) {
    DWORD exitCode = 0;
    GetExitCodeProcess(hProcess, &exitCode);

    return exitCode;
}

/**
 * Gets the Windows API error message.
 * @param  apiName - the Windows API name
 * @return the Windows API error message
 */
std::string getErrorMessage(const std::string& apiName) {
    LPVOID lpvMessageBuffer;

    FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER |
        FORMAT_MESSAGE_FROM_SYSTEM,
        NULL, GetLastError(),
        MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
        (LPSTR)&lpvMessageBuffer, 0, NULL);

    std::stringstream stringStream;
    std::string errorMessage((LPSTR)lpvMessageBuffer);

    stringStream << "API:     " << apiName << std::endl
                 << "Code:    " << GetLastError() << std::endl
                 << "Message: " << errorMessage << std::endl;
    LocalFree(lpvMessageBuffer);

    return stringStream.str();
}

/**
 * Gets a std::wstring string.
 * @param  str - a std::string string
 * @return a std::wstring string
 */
std::wstring getWideString(const std::string& str) {
    std::wstring wstr(str.begin(), str.end());
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
