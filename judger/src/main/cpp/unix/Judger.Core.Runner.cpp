#pragma GCC diagnostic ignored "-Wwrite-strings"

#include "../com_trunkshell_voj_jni_library.h"
#include "../com_trunkshell_voj_judger_core_Runner.h"

#include <algorithm>
#include <cstring>
#include <iostream>
#include <iterator>
#include <string>
#include <sstream>

#include <signal.h>
#include <spawn.h>
#include <sys/wait.h>

/**
 * A pointer to an array of character pointers to the environment strings.
 */
extern char** environ;

/**
 * Function Prototypes.
 */
posix_spawn_file_actions_t setupIoRedirection(const std::string& inputFilePath, const std::string& outputFilePath);
int createProcess(pid_t& pid, const std::string& commandLine, posix_spawn_file_actions_t& fileActions);
char** getCommandArgs(const std::string& commandLine);
int killProcess(pid_t& pid, posix_spawn_file_actions_t& fileActions);

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
    std::string commandLine = getStringValue(jniEnv, jCommandLine);
    std::string inputFilePath = getStringValue(jniEnv, jInputFilePath);
    std::string outputFilePath = getStringValue(jniEnv, jOutputFilePath);

    posix_spawn_file_actions_t fileActions = setupIoRedirection(inputFilePath, outputFilePath);

    pid_t pid = -1;
    int processStatus = createProcess(pid, commandLine, fileActions);
    if ( processStatus != 0 ) {
        throwCStringException(jniEnv, "Cannot create the process.");
    }
    
    std::cout << "PID: " << pid << std::endl;
    if ( waitpid(pid, &processStatus, 0) != -1 ) {
        std::cout << "Child exited with processStatus " << processStatus << std::endl;
    }

    return nullptr;
}

/**
 * 设置程序I/O重定向.
 * @param  inputFilePath  - 执行程序时的输入文件路径(可为NULL)
 * @param  outputFilePath - 执行程序后的输出文件路径(可为NULL)
 * @return 包含I/O重定向信息的posix_spawn_file_actions_t对象
 */
posix_spawn_file_actions_t setupIoRedirection(
    const std::string& inputFilePath, const std::string& outputFilePath) {
    posix_spawn_file_actions_t fileActions;
    posix_spawn_file_actions_init(&fileActions);

    /*if ( inputFilePath != "" ) {
        // do something
    }
    if ( outputFilePath != "" ) {
        // do something
    }*/
    return fileActions;
}

/**
 * 创建进程.
 * @param  pid         - 进程ID
 * @param  commandLine - 待执行的命令行
 * @param  fileActions - I/O重定向信息
 * @return 运行创建状态(0表示成功创建)
 */
int createProcess(pid_t& pid, const std::string& commandLine, 
    posix_spawn_file_actions_t& fileActions) {
    char** argv = getCommandArgs(commandLine);
    return posix_spawnp(&pid, argv[0], &fileActions, NULL, argv, environ);
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
 * 强制销毁进程(当触发阈值时).
 * @param  pid         - 进程ID
 * @param  fileActions - I/O重定向信息
 * @return 0, 表示进程被成功结束.
 */
int killProcess(pid_t& pid, posix_spawn_file_actions_t& fileActions) {
    posix_spawn_file_actions_destroy(&fileActions);
    return 0;
}