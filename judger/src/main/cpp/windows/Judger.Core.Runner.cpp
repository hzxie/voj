#include "../com_trunkshell_voj_jni_library.h"
#include "../com_trunkshell_voj_judger_core_Runner.h"

#include <iostream>

JNIEXPORT jobject JNICALL Java_com_trunkshell_voj_judger_core_Runner_getRuntimeResult(
    JNIEnv* jniEnv, jobject selfReference, jstring jCommandLine, jstring username,
    jstring password, jstring jInputFilePath, jstring jOutputFilePath, jint timeLimit, 
    jint memoryLimit) {
    
    std::cout << "#1: ";
    std::cout << getStringValue(jniEnv, commandLine) << std::endl;
    std::cout << "#2: ";
    // std::cout << getStringValue(jniEnv, inputFilePath) << std::endl;
    std::cout << "#3: ";
    std::cout << getStringValue(jniEnv, outputFilePath) << std::endl;
    std::cout << "#4: ";
    std::cout << timeLimit << std::endl;
    std::cout << "#5: ";
    std::cout << memoryLimit << std::endl;

    return nullptr;
}