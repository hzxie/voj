/* Verwandlung Online Judge - A cross-platform judge online system
 * Copyright (C) 2018 Haozhe Xie <cshzxie@gmail.com>
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
 *
 *
 *                              _ooOoo_  
 *                             o8888888o  
 *                             88" . "88  
 *                             (| -_- |)  
 *                             O\  =  /O  
 *                          ____/`---'\____  
 *                        .'  \\|     |//  `.  
 *                       /  \\|||  :  |||//  \  
 *                      /  _||||| -:- |||||-  \  
 *                      |   | \\\  -  /// |   |  
 *                      | \_|  ''\---/''  |   |  
 *                      \  .-\__  `-`  ___/-. /  
 *                    ___`. .'  /--.--\  `. . __  
 *                 ."" '<  `.___\_<|>_/___.'  >'"".  
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |  
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /  
 *           ======`-.____`-.___\_____/___.-`____.-'======  
 *                              `=---=' 
 *
 *                          HERE BE BUDDHA
 *
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
 * JNI调用入口.
 * 获取程序运行结果.
 * @param  jniEnv          - JNI 运行环境引用
 * @param  selfReference   - 对调用Java的对象的引用
 * @param  jCommandLine    - 待执行的命令行
 * @param  jUsername       - Windows用户名
 * @param  jPassword       - Windows密码
 * @param  jInputFilePath  - 执行程序时的输入文件路径(可为NULL)
 * @param  jOutputFilePath - 执行程序后的输出文件路径(可为NULL)
 * @param  timeLimit       - 程序执行时间限制(ms, 0为不限制)
 * @param  memoryLimit     - 程序执行内存限制(KB, 0为不限制)
 * @return 一个包含运行结果的Map<String, Object>对象
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
 * 重定向子进程的I/O.
 * @param inputFilePath  - 输入文件路径
 * @param outputFilePath - 输出文件路径
 * @param hInput         - 输入文件句柄
 * @param hOutput        - 输出文件句柄
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
 * 根据I/O重定向信息重新设置startupInfo.
 * @param startupInfo - STARTUPINFOW结构体
 * @param hInput      - 文件输入句柄
 * @param hOutput     - 文件输出句柄
 */
void setupStartupInfo(STARTUPINFOW& startupInfo, HANDLE& hInput, HANDLE& hOutput) {
    startupInfo.cb              = sizeof(STARTUPINFOW);
    startupInfo.dwFlags        |= STARTF_USESTDHANDLES;
    startupInfo.hStdInput       = hInput;
    startupInfo.hStdError       = hOutput;
    startupInfo.hStdOutput      = hOutput;
}

/**
 * 创建进程.
 * @param  commandLine   - 待执行的命令行
 * @param  username      - Windows用户名
 * @param  password      - Windows密码
 * @param  hToken        - a token that represents the specified user
 * @param  lpEnvironment - an environment block for the new process
 * @param  startupInfo   - a STARTUPINFO structure
 * @param  processInfo   - a PROCESS_INFORMATION structure that receives identification 
 *                         information for the new process, including a handle to the process
 * @return 进程是否创建成功
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
 * 运行进程.
 * @param  processInfo - 包含进程信息的PROCESS_INFORMATION结构体
 * @param  timeLimit   - 运行时时间限制(ms)
 * @param  memoryLimit - 运行时空间限制(KB)
 * @param  timeUsage   - 运行时时间占用(ms)
 * @param  memoryUsage - 运行时空间占用(ms)
 * @return 进程退出状态
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
 * 获取运行时内存占用最大值
 * @param  processInfo - 包含进程信息的PROCESS_INFORMATION结构体
 * @param  memoryLimit - 运行时空间限制(KB)
 * @return 运行时内存占用最大值
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
 * 获取内存占用情况.
 * @param  hProcess - 进程句柄
 * @return 当前物理内存使用量(KB)
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
 * 获取当前系统时间.
 * 用于统计程序运行时间.
 * @return 当前系统时间(以毫秒为单位)
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
 * 强制销毁进程(当触发阈值时).
 * @param  processInfo - 包含进程信息的PROCESS_INFORMATION结构体
 * @return 销毁进程操作是否成功完成
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
 * 获取应用程序退出状态.
 * 0表示正常退出, 259表示仍在运行.
 * @param  hProcess - 进程的句柄
 * @return 退出状态
 */
DWORD getExitCode(HANDLE& hProcess) {
    DWORD exitCode = 0;
    GetExitCodeProcess(hProcess, &exitCode);

    return exitCode;
}

/**
 * 获取Windows API异常信息.
 * @param  apiName - Windows API名称
 * @return Windows API异常信息
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
 * 获取std::wstring类型的字符串.
 * @param  str - std::string类型的字符串
 * @return wstring类型的字符串
 */
std::wstring getWideString(const std::string& str) {
    std::wstring wstr(str.begin(), str.end());
    return wstr;
}

/**
 * 获取std::wstring对应LPWSTR类型的指针.
 * @param  str - std::wstring类型的字符串
 * @return 对应LPWSTR类型的指针
 */
LPWSTR getWideStringPointer(const std::wstring& str) {
    return const_cast<LPWSTR>(str.c_str());
}

/**
 * 获取std::wstring对应LPCWSTR类型的指针.
 * @param  str - std::wstring类型的字符串
 * @return 对应LPCWSTR类型的指针
 */
LPCWSTR getConstWideStringPointer(const std::wstring& str) {
    return str.c_str();
}
