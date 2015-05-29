#pragma GCC diagnostic ignored "-Wwrite-strings"

#include "../com_trunkshell_voj_jni_library.h"
#include "../com_trunkshell_voj_judger_core_Runner.h"

#include <iostream>
#include <string>

#include <windows.h>
#include <userenv.h>

/**
 * Function Prototypes.
 */
bool createProcess(const std::wstring&, const std::wstring&, const std::wstring&, HANDLE&, LPVOID, STARTUPINFOW&, PROCESS_INFORMATION&);
std::wstring getWideString(const std::string& str);
LPWSTR getWideStringPointer(const std::wstring&);
LPCWSTR getConstWideStringPointer(const std::wstring&);
bool killProcess(LPVOID, HANDLE&, PROCESS_INFORMATION);

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
JNIEXPORT jobject JNICALL Java_com_trunkshell_voj_judger_core_Runner_getRuntimeResult(
    JNIEnv* jniEnv, jobject selfReference, jstring jCommandLine, jstring jUsername,
    jstring jPassword, jstring jInputFilePath, jstring jOutputFilePath, jint timeLimit, 
    jint memoryLimit) {
    std::wstring        commandLine         = getWideString(getStringValue(jniEnv, jCommandLine));
    std::wstring        username            = getWideString(getStringValue(jniEnv, jUsername));
    std::wstring        password            = getWideString(getStringValue(jniEnv, jPassword));
    std::wstring        inputFilePath       = getWideString(getStringValue(jniEnv, jInputFilePath));
    std::wstring        outputFilePath      = getWideString(getStringValue(jniEnv, jOutputFilePath));

    HANDLE              hToken;
    LPVOID              lpEnvironment       = NULL;
    PROCESS_INFORMATION processInfo         = {0};
    STARTUPINFOW        startupInfo         = {0};

    if ( !createProcess(commandLine, username, password, hToken, lpEnvironment, startupInfo, processInfo) ) {
        throwException(jniEnv, "Failed to create the process.");
    }
    std::cout << "PID: " << processInfo.dwProcessId << std::endl;

    return nullptr;
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
            LOGON_WITH_PROFILE, NULL, lpCommandLine, CREATE_UNICODE_ENVIRONMENT,
            lpEnvironment, szUserProfile, &startupInfo, &processInfo) ) {
        return false;
    }
    return true;
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

/**
 * 强制销毁进程(当触发阈值时).
 * @param  lpEnvironment - an environment block for the new process
 * @param  startupInfo   - a STARTUPINFO structure
 * @param  processInfo   - a PROCESS_INFORMATION structure that receives identification 
 *                         information for the new process, including a handle to the process
 * @return 销毁进程操作是否成功完成
 */
bool killProcess(LPVOID lpEnvironment, HANDLE& hToken,
    PROCESS_INFORMATION processInfo) {
    if ( !DestroyEnvironmentBlock(lpEnvironment) ) {
        return false;
    }
    CloseHandle(hToken);
    CloseHandle(processInfo.hProcess);
    CloseHandle(processInfo.hThread);

    return true;
}