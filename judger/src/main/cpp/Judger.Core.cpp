#include "com_trunkshell_voj_judger_core_Runner.h"

#include <iostream>

JNIEXPORT jobject JNICALL Java_com_trunkshell_voj_judger_core_Runner_getRuntimeResult(
    JNIEnv* jniEnv, jobject selfReference, jstring commandLine, jstring inputFilePath, 
    jstring outputFilePath, jint timeLimit, jint memoryLimit) {
    std::cout << "Hello" << std::endl;
    return nullptr;
}