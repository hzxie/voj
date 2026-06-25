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
#include <spawn.h>
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
posix_spawn_file_actions_t setupIoRedirection(const std::string&, const std::string&);
int createProcess(pid_t&, const std::string&, posix_spawn_file_actions_t&);
int runProcess(pid_t&, int, int, int&, int&);
char** getCommandArgs(const std::string& commandLine);
int getRunningTime(pid_t, int);
int getMaxUsedMemory(pid_t, int);
int getCurrentUsedMemory(pid_t);
long long getMillisecondsNow();
int killProcess(pid_t&);
bool isProcessRunning(pid_t&);

/**
 * JNI call entry point.
 * Gets the program's runtime result.
 * @param  jniEnv          - a reference to the JNI runtime environment
 * @param  selfReference   - a reference to the calling Java object
 * @param  jCommandLine    - the command line to execute
 * @param  jUsername       - the Unix username
 * @param  jPassword       - the Unix password
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
    std::string commandLine         = getStringValue(jniEnv, jCommandLine);
    std::string inputFilePath       = getStringValue(jniEnv, jInputFilePath);
    std::string outputFilePath      = getStringValue(jniEnv, jOutputFilePath);

    std::cout << "[INFO] Command Line: " << commandLine << std::endl;

    JHashMap    result;
    jint        usedTime            = 0;
    jint        usedMemory          = 0;
    jint        exitCode            = 127;

    // Setup I/O Redirection for Child Process
    posix_spawn_file_actions_t fileActions = setupIoRedirection(inputFilePath, outputFilePath);

    // Create Process
    pid_t       pid                 = -1;
    int         processStatus       = createProcess(pid, commandLine, fileActions);
    if ( processStatus != 0 ) {
        std::cout << "[WARN] Cannot create the process." << std::endl;
    }

    exitCode = runProcess(pid, timeLimit, memoryLimit, usedTime, usedMemory);
    posix_spawn_file_actions_destroy(&fileActions);

    std::cout << "[DEBUG] usedTime: " << usedTime << " ms" << std::endl;
    std::cout << "[DEBUG] usedMemory: " << usedMemory  << " KB" << std::endl;
    std::cout << "[DEBUG] exitCode: " << exitCode << std::endl;
    
    result.put("usedTime", usedTime);
    result.put("usedMemory", usedMemory);
    result.put("exitCode", exitCode);
    return result.toJObject(jniEnv);
}

/**
 * Creates a process.
 * @param  pid         - the child process ID
 * @param  commandLine - the command line to execute
 * @param  fileActions - the I/O redirection information
 * @return the process creation status (0 means created successfully)
 */
int createProcess(pid_t& pid, const std::string& commandLine, 
    posix_spawn_file_actions_t& fileActions) {
    char** argv = getCommandArgs(commandLine);
    return posix_spawnp(&pid, argv[0], &fileActions, NULL, argv, environ);
}

/**
 * Sets up the program's I/O redirection.
 * @param  inputFilePath  - the input file path when running the program (may be NULL)
 * @param  outputFilePath - the output file path after running the program (may be NULL)
 */
posix_spawn_file_actions_t setupIoRedirection(
    const std::string& inputFilePath, const std::string& outputFilePath) {
    posix_spawn_file_actions_t fileActions;
    posix_spawn_file_actions_init(&fileActions);
    
    if ( inputFilePath != "" ) {
        int inputFileDescriptor = open(inputFilePath.c_str(), O_RDONLY);
        posix_spawn_file_actions_adddup2(&fileActions, inputFileDescriptor, STDIN);
        posix_spawn_file_actions_addclose(&fileActions, inputFileDescriptor);
    }
    if ( outputFilePath != "" ) {
        int outputFileDescriptor = open(outputFilePath.c_str(), O_CREAT | O_WRONLY);
        chmod(outputFilePath.c_str(), S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
        posix_spawn_file_actions_adddup2(&fileActions, outputFileDescriptor, STDOUT);
        posix_spawn_file_actions_adddup2(&fileActions, outputFileDescriptor, STDERR);
        posix_spawn_file_actions_addclose(&fileActions, outputFileDescriptor);
    }
    return fileActions;
}

/**
 * Runs the process.
 * @param  pid         - the child process ID
 * @param  timeLimit   - the runtime time limit (ms)
 * @param  memoryLimit - the runtime memory limit (KB)
 * @param  usedTime    - the runtime time used (ms)
 * @param  usedMemory  - the runtime memory used (KB)
 * @return the process exit status
 */
int runProcess(pid_t& pid, int timeLimit, int memoryLimit, int& usedTime, int& usedMemory) {
    std::future<int> memFeature = std::async(std::launch::async, getMaxUsedMemory, pid, memoryLimit);
    std::future<int> timeFeature  = std::async(std::launch::async, getRunningTime, pid, timeLimit);
    usedMemory = memFeature.get();
    usedTime = timeFeature.get();

    int exitCode = 0;
    do {
        usleep(50000);
    } while ( waitpid(pid, &exitCode, 0) == pid );
    return exitCode;
}

/**
 * Gets the list of command-line arguments.
 * @param  commandLine - the command line
 * @return the list of command-line arguments
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
 * Gets the runtime time.
 * @param  pid       - the process ID
 * @param  timeLimit - the runtime time limit (ms)
 * @return the runtime time
 */
int getRunningTime(pid_t pid, int timeLimit) {
    long long  startTime = getMillisecondsNow(),
               endTime   = 0;
    int        usedTime = 0;

    do {
        endTime     = getMillisecondsNow();
        usedTime    = endTime - startTime;
        if ( usedTime > timeLimit ) {
            killProcess(pid);
        }
        usleep(500);
    } while ( isProcessRunning(pid) );

    return usedTime;
}

/**
 * Whether the currently obtained memory usage value should be ignored.
 * During actual execution, the program may read the memory usage of the JVM environment.
 * In that case, we should ignore this value.
 * @param  currentUsedMemory - the currently obtained memory usage
 * @return whether the currently obtained memory usage should be ignored
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
 * Gets the maximum runtime memory usage.
 * @param  pid         - the process ID
 * @param  memoryLimit - the runtime memory limit (KB)
 * @return the maximum runtime memory usage
 */
int getMaxUsedMemory(pid_t pid, int memoryLimit) {
    int  maxUsedMemory     = 0,
         currentUsedMemory = 0;
    do {
        currentUsedMemory = getCurrentUsedMemory(pid);
        std::cout << "[DEBUG] Current Memory of PID# " << pid << ": "
                  << currentUsedMemory << " KB" << std::endl;
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
 * Gets the memory usage.
 * @param  pid - the process ID
 * @return the current physical memory usage (KB)
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
 * Gets the current system time.
 * Used to measure the program's running time.
 * @return the current system time (in milliseconds)
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
 * Forcibly destroys the process (when a threshold is triggered).
 * @param  pid - the process ID
 * @return 0, indicating the process was terminated successfully.
 */
int killProcess(pid_t& pid) {
    return kill(pid, SIGKILL);
}

/**
 * Checks whether the process is still running.
 * @param  pid - the process ID
 * @return whether the process is still running.
 */
bool isProcessRunning(pid_t& pid) {
    std::string filePath("/proc/");
    filePath += std::to_string(pid) + "/stat";
    const char* cFilePath = filePath.c_str();
    
    FILE* fp = fopen(cFilePath, "r");
    if ( fp != NULL ) {
        char state;
        if (fscanf(fp, "%*d %*s %c", &state) == 1) {
            return state != 'Z';
        }
        fclose(fp);
    }
    return false;
}
