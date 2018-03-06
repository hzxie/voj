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
#include <string>

#include <jni.h>
/* Header for class org_verwandlung_voj_jni_library */

#ifndef _Included_org_verwandlung_voj_jni_library
#define _Included_org_verwandlung_voj_jni_library
#ifdef __cplusplus
extern "C" {
#endif

/**
 * 获取Java中String的值.
 * @param  jniEnv - JNI 运行环境引用
 * @param  jStr   - 待获取值的Java字符串
 * @return Java字符串的值
 */
const char* getStringValue(JNIEnv* JniEnv, jstring jStr) {
    if ( jStr == nullptr || jStr == NULL ) {
        return "";
    }
    const char* str = JniEnv->GetStringUTFChars(jStr, 0);
    return str;
}

/**
 * 抛出异常至Java运行环境.
 * @param  jniEnv  - JNI 运行环境引用
 * @param  message - 异常信息
 * @return Java.lang.Error对象
 */
jint throwCStringException(JNIEnv* jniEnv, char* message) {
    jclass exClass;
    char* className = "java/lang/Error";
    exClass = jniEnv->FindClass(className);

    return jniEnv->ThrowNew(exClass, message);
}

/**
 * 抛出异常至Java运行环境.
 * @param  jniEnv  - JNI 运行环境引用
 * @param  message - 异常信息
 * @return Java.lang.Error对象
 */
jint throwStringException(JNIEnv* jniEnv, std::string message) {
    char* pMessage = const_cast<char *>(message.c_str());
    return throwCStringException(jniEnv, pMessage);
}

#ifdef __cplusplus
}
#endif
#endif
