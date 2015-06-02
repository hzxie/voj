#pragma GCC diagnostic ignored "-Wwrite-strings"

#include "../com_trunkshell_voj_jni_hashmap.h"
#include "../com_trunkshell_voj_jni_library.h"
#include "../com_trunkshell_voj_judger_core_Runner.h"

#include <algorithm>
#include <cstdint>
#include <cstring>
#include <future>
#include <iostream>
#include <iterator>
#include <limits>
#include <string>
#include <sstream>

#include <fcntl.h>
#include <signal.h>
#include <sys/stat.h>
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
bool createProcess(pid_t& pid);
void setupIoRedirection(const std::string&, const std::string&);
int runProcess(pid_t, const std::string&, int, int, int&, int&);
char** getCommandArgs(const std::string& commandLine);
int getMaxMemoryUsage(pid_t, int);
int getCurrentMemoryUsage(pid_t);
long long getMillisecondsNow();
int killProcess(pid_t& pid);

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
JNIEXPORT jobject JNICALL Java_com_trunkshell_voj_judger_core_Runner_getRuntimeResult(
    JNIEnv* jniEnv, jobject selfReference, jstring jCommandLine, jstring jUsername,
    jstring jPassword, jstring jInputFilePath, jstring jOutputFilePath, jint timeLimit, 
    jint memoryLimit) {

    std::cout << "================START================" << std::endl;
    std::string commandLine         = getStringValue(jniEnv, jCommandLine);
    std::string inputFilePath       = getStringValue(jniEnv, jInputFilePath);
    std::string outputFilePath      = getStringValue(jniEnv, jOutputFilePath);

    JHashMap    result;
    jint        timeUsage           = 0;
    jint        memoryUsage         = 0;
    jint        exitCode            = 127;

    pid_t       pid                 = -1;
    if ( !createProcess(pid) ) {
        throwCStringException(jniEnv, "Failed to fork a process.");
    }
    std::cout << "1 # getpid(): " << getpid() << std::endl;
    std::cout << "2 # Child PID: " << pid << std::endl;
    // Setup I/O Redirection for Child Process
    if ( pid == 0 ) {
        setupIoRedirection(inputFilePath, outputFilePath);
    }
    exitCode = runProcess(pid, commandLine, 10000, 0, timeUsage, memoryUsage);

    std::cout << "9 # Command Line: " << commandLine << std::endl;
    std::cout << "10 # exitCode in JNI: " << exitCode << std::endl;
    std::cout << "11 # timeUsage in JNI: " << timeUsage << std::endl;
    std::cout << "12 # memoryUsage in JNI: " << memoryUsage << std::endl;

    result.put("timeUsage", timeUsage);
    result.put("memoryUsage", memoryUsage);
    result.put("exitCode", exitCode);

    std::cout << "=================END=================" << std::endl;

    return result.toJObject(jniEnv);
}

/**
 * 创建进程.
 * @param  pid - 进程ID
 * @return 运行创建状态(-1表示未成功创建, 0表示子进程)
 */
bool createProcess(pid_t& pid) {
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

/**
 * 运行进程.
 * @param  pid         - 子进程ID
 * @param  commandLine - 命令行
 * @param  timeLimit   - 运行时时间限制(ms)
 * @param  memoryLimit - 运行时空间限制(KB)
 * @param  timeUsage   - 运行时时间占用(ms)
 * @param  memoryUsage - 运行时空间占用(ms)
 * @return 进程退出状态
 */
int runProcess(pid_t pid, const std::string& commandLine, int timeLimit, 
    int memoryLimit, int& timeUsage, int& memoryUsage) {
    char**            argv       = getCommandArgs(commandLine);
    long long         startTime  = 0;
    long long         endTime    = 0;
    int               exitCode   = 0;
    std::future<int>  feature;

    // Setup Monitor in Parent Process
    if ( pid > 0 ) {
        feature     = std::async(std::launch::async, getMaxMemoryUsage, pid, memoryLimit);
        startTime   = getMillisecondsNow();
    }
    // Run Child Process
    if ( pid == 0 ) {
        alarm(timeUsage / 1000);
        _exit(execvp(argv[0], argv));
    }

    // Collect information in Parent Process
    waitpid(pid, &exitCode, 0);
    endTime     = getMillisecondsNow();
    timeUsage   = endTime - startTime;
    memoryUsage = feature.get();

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
 * 获取运行时内存占用最大值
 * @param  pid         - 进程ID
 * @param  memoryLimit - 运行时空间限制(KB)
 * @return 运行时内存占用最大值
 */
int getMaxMemoryUsage(pid_t pid, int memoryLimit) {
    int  maxMemoryUsage     = 0,
         currentMemoryUsage = 0;
    do {
        currentMemoryUsage = getCurrentMemoryUsage(pid);
        std::cout << "currentMemoryUsage: [PID #" << pid << "]" << currentMemoryUsage << std::endl;
        if ( currentMemoryUsage > maxMemoryUsage ) {
            maxMemoryUsage = currentMemoryUsage;
        }
        if ( memoryLimit != 0 && currentMemoryUsage > memoryLimit ) {
            killProcess(pid);
        }
        usleep(50000);
    } while ( currentMemoryUsage != 0 );

    return maxMemoryUsage;
}

/**
 * 获取内存占用情况.
 * @param  pid - 进程ID
 * @return 当前物理内存使用量(KB)
 */
int getCurrentMemoryUsage(pid_t pid) {
    int    currentMemoryUsage   = 0;
    long   residentSetSize      = 0L;
    FILE*  fp                   = NULL;
    
    std::stringstream stringStream;
    stringStream << "/proc/" << pid << "/statm";
    const char* filePath = stringStream.str().c_str();

    if ( (fp = fopen( filePath, "r" )) != NULL ) {
        if ( fscanf(fp, "%*s%ld", &residentSetSize) == 1 ) {
            currentMemoryUsage = (int)residentSetSize * (int)sysconf( _SC_PAGESIZE) >> 10;
            if ( currentMemoryUsage < 0 ) {
                currentMemoryUsage = std::numeric_limits<int32_t>::max() >> 10;
            }
        }
        fclose(fp);
    }
    return currentMemoryUsage;
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