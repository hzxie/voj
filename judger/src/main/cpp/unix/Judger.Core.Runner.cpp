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

#include "../org_verwandlung_voj_jni_hashmap.h"
#include "../org_verwandlung_voj_jni_library.h"
#include "../org_verwandlung_voj_judger_core_Runner.h"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <cstring>
#include <future>
#include <iostream>
#include <iterator>
#include <limits>
#include <sstream>
#include <string>
#include <vector>

#include <fcntl.h>
#include <signal.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#define  STDIN  0
#define  STDOUT 1
#define  STDERR 2

/**
 * A pointer to an array of character pointers to the environment strings.
 */
extern char** environ;

/**
 * Function Prototypes.
 */
bool createProcess(pid_t&, sigset_t&);
void setupIoRedirection(const std::string&, const std::string&);
void setupRunUser();
int runProcess(pid_t, sigset_t, const std::string&, int, int, int&, int&);
char** getCommandArgs(const std::string& commandLine);
bool isCurrentUsedMemoryIgnored(int, int);
int getMaxUsedMemory(pid_t, int);
int getCurrentUsedMemory(pid_t);
long long getMillisecondsNow();
int killProcess(pid_t&);

/**
 * JNI调用入口.
 * 获取程序运行结果.
 * @param  jniEnv          - JNI 运行环境引用
 * @param  selfReference   - 对调用Java的对象的引用
 * @param  jCommandLine    - 待执行的命令行
 * @param  jUsername       - Unix用户名
 * @param  jPassword       - Unix密码
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
    std::string commandLine         = getStringValue(jniEnv, jCommandLine);
    std::string inputFilePath       = getStringValue(jniEnv, jInputFilePath);
    std::string outputFilePath      = getStringValue(jniEnv, jOutputFilePath);

    std::cout << "[INFO] Command Line: " << commandLine << std::endl;

    JHashMap    result;
    jint        usedTime            = 0;
    jint        usedMemory          = 0;
    jint        exitCode            = 127;

    pid_t       pid                 = -1;
    sigset_t    sigset;
    if ( !createProcess(pid, sigset) ) {
        throwCStringException(jniEnv, "Failed to fork a process.");
    }
    // Setup I/O Redirection for Child Process
    if ( pid == 0 ) {
        setupRunUser();
        setupIoRedirection(inputFilePath, outputFilePath);
    }
    exitCode = runProcess(pid, sigset, commandLine, timeLimit, memoryLimit, usedTime, usedMemory);

    std::cout << "[DEBUG] usedTime: " << usedTime << " ms" << std::endl;
    std::cout << "[DEBUG] usedMemory: " << usedMemory  << " KB" << std::endl;
    std::cout << "[DEBUG] exitCode: " << exitCode << std::endl;
    
    result.put("usedTime", usedTime);
    result.put("usedMemory", usedMemory);
    result.put("exitCode", exitCode);

    return result.toJObject(jniEnv);
}

/**
 * 创建进程.
 * @param  pid    - 进程ID
 * @param  sigset - 进程的标记
 * @return 运行创建状态(-1表示未成功创建, 0表示子进程)
 */
bool createProcess(pid_t& pid, sigset_t& sigset) {
    sigset_t originalSigset;
    sigemptyset (&sigset);
    sigaddset (&sigset, SIGCHLD);
    if ( sigprocmask(SIG_BLOCK, &sigset, &originalSigset) < 0 ) {
        return false;
    }

    pid         = fork();
    if ( pid == -1 ) {
        return false;
    }
    return true;
}

/**
 * 设置程序I/O重定向.
 * @param  inputFilePath  - 执行程序时的输入文件路径(可为NULL)
 * @param  outputFilePath - 执行程序后的输出文件路径(可为NULL)
 */
void setupIoRedirection(
    const std::string& inputFilePath, const std::string& outputFilePath) {
    if ( inputFilePath != "" ) {
        int inputFileDescriptor = open(inputFilePath.c_str(), O_RDONLY);
        dup2(inputFileDescriptor, STDIN);
        close(inputFileDescriptor);
    }
    if ( outputFilePath != "" ) {
        int outputFileDescriptor = open(outputFilePath.c_str(), O_CREAT | O_WRONLY);
        chmod(outputFilePath.c_str(), S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
        dup2(outputFileDescriptor, STDOUT);
        dup2(outputFileDescriptor, STDERR);
        close(outputFileDescriptor);
    }
}

void setupRunUser() {
    while ( setgid(1536) != 0 ) { 
        std::cout <<  "[WARN] setgid(1536) failed." << std::endl;
        sleep(1); 
    }
    while ( setuid(1536) != 0 ) { 
        std::cout <<  "[WARN] setuid(1536) failed." << std::endl;
        sleep(1); 
    }
    while ( setresuid(1536, 1536, 1536) != 0 ) {
        std::cout <<  "[WARN] setresuid(1536, 1536, 1536) failed." << std::endl;
        sleep(1);
    }
}

/**
 * 运行进程.
 * @param  pid         - 子进程ID
 * @param  sigset      - 进程的标记
 * @param  commandLine - 命令行
 * @param  timeLimit   - 运行时时间限制(ms)
 * @param  memoryLimit - 运行时空间限制(KB)
 * @param  UsedTime    - 运行时时间占用(ms)
 * @param  UsedMemory  - 运行时空间占用(ms)
 * @return 进程退出状态
 */
int runProcess(pid_t pid, sigset_t sigset, const std::string& commandLine, int timeLimit, 
    int memoryLimit, int& UsedTime, int& UsedMemory) {
    char**            argv       = getCommandArgs(commandLine);
    long long         startTime  = 0;
    long long         endTime    = 0;
    int               exitCode   = 0;
    std::future<int>  feature;

    // Setup Monitor in Parent Process
    if ( pid > 0 ) {
        // Memory Monitor
        feature     = std::async(std::launch::async, getMaxUsedMemory, pid, memoryLimit);
        
        // Time Monitor
        struct timespec timeout;
        timeout.tv_sec  = timeLimit / 1000;
        timeout.tv_nsec = timeLimit % 1000 * 1000000;
        
        startTime       = getMillisecondsNow();
        do {
            if ( sigtimedwait(&sigset, NULL, &timeout) < 0 ) {
                if ( errno == EINTR ) {
                    /* Interrupted by a signal other than SIGCHLD. */
                    continue;
                } else if (errno == EAGAIN) {
                    killProcess(pid);
                } else {
                    return 127;
                }
            }
            break;
        } while ( true );
    }
    // Run Child Process
    if ( pid == 0 ) {
        _exit(execvp(argv[0], argv));
    }

    // Collect information in Parent Process
    waitpid(pid, &exitCode, 0);
    endTime     = getMillisecondsNow();
    UsedTime    = endTime - startTime;
    UsedMemory  = feature.get();

    return exitCode;
}

/**
 * 获取命令行参数列表.
 * @param  commandLine - 命令行
 * @return 命令行参数列表
 */
char** getCommandArgs(const std::string& commandLine) {
    std::istringstream iss(commandLine);
    std::vector<std::string> args = {
        std::istream_iterator<std::string>{iss},
        std::istream_iterator<std::string>{}
    };

    size_t numberOfArguments = args.size();
    char** argv = new char*[numberOfArguments + 1]();

    for ( size_t i = 0; i < numberOfArguments; ++ i ) {
        char* arg = new char[ args[i].size() + 1 ];
        strcpy(arg, args[i].c_str());
        argv[i] = arg;
    }
    argv[numberOfArguments] = nullptr;

    return argv;
}

/**
 * 是否忽略当前获得的内存占用值.
 * 由于在实际运行过程中, 程序可能会获取到JVM环境中的内存占用.
 * 对于这种情况, 我们应忽略这个值.
 * @param  currentUsedMemory - 当前获取到的内存占用
 * @return 是否忽略当前获取到的内存占用
 */
bool isCurrentUsedMemoryIgnored(int currentUsedMemory) {
    int jvmUsedMemory = getCurrentUsedMemory(getpid());
    // std::cout << "[DEBUG] Current Memory of JVM: " << jvmUsedMemory << " KB" << std::endl;
    if ( currentUsedMemory >= jvmUsedMemory / 2 &&
         currentUsedMemory <= jvmUsedMemory * 2 ) {
        return true;
    }
    return false;
}

/**
 * 获取运行时内存占用最大值
 * @param  pid         - 进程ID
 * @param  memoryLimit - 运行时空间限制(KB)
 * @return 运行时内存占用最大值
 */
int getMaxUsedMemory(pid_t pid, int memoryLimit) {
    int  maxUsedMemory     = 0,
         currentUsedMemory = 0;
    do {
        currentUsedMemory = getCurrentUsedMemory(pid);
        // std::cout << "[DEBUG] Current Memory of PID# " << pid << ": " << currentUsedMemory << " KB" << std::endl;
        if ( currentUsedMemory > maxUsedMemory && 
            !isCurrentUsedMemoryIgnored(currentUsedMemory) ) {
            maxUsedMemory = currentUsedMemory;
        }
        if ( memoryLimit != 0 && maxUsedMemory > memoryLimit ) {
            killProcess(pid);
        }
        usleep(500);
    } while ( currentUsedMemory != 0 );

    return maxUsedMemory;
}

/**
 * 获取内存占用情况.
 * @param  pid - 进程ID
 * @return 当前物理内存使用量(KB)
 */
int getCurrentUsedMemory(pid_t pid) {
    int    currentUsedMemory    = 0;
    long   residentSetSize      = 0L;

    std::string filePath("/proc/");
    filePath += std::to_string(pid) + "/statm";
    const char* cFilePath = filePath.c_str();
    
    FILE* fp = fopen(cFilePath, "r");
    if ( fp != NULL ) {
        if ( fscanf(fp, "%*s%ld", &residentSetSize) == 1 ) {
            currentUsedMemory = (int)residentSetSize * (int)sysconf(_SC_PAGESIZE) >> 10;
            if ( currentUsedMemory < 0 ) {
                currentUsedMemory = std::numeric_limits<int32_t>::max() >> 10;
            }
        }
        fclose(fp);
    }
    return currentUsedMemory;
}

/**
 * 获取当前系统时间.
 * 用于统计程序运行时间.
 * @return 当前系统时间(以毫秒为单位)
 */
long long getMillisecondsNow() {
    long            milliseconds;
    time_t          seconds;
    struct timespec spec;

    clock_gettime(CLOCK_REALTIME, &spec);
    seconds                 = spec.tv_sec;
    milliseconds            = round(spec.tv_nsec / 1.0e6);
    long long currentTime   = seconds * 1000 + milliseconds;

    return currentTime;
}

/**
 * 强制销毁进程(当触发阈值时).
 * @param  pid         - 进程ID
 * @return 0, 表示进程被成功结束.
 */
int killProcess(pid_t& pid) {
    return kill(pid, SIGKILL);
}
