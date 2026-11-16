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
#include <string>

#include <jni.h>
/* Helpers shared by the native runners (C++ only: they use std::string). */

#ifndef _Included_org_verwandlung_voj_jni_library
#define _Included_org_verwandlung_voj_jni_library

/**
 * Gets the value of a Java String (in JNI's modified UTF-8). The JNI copy of the characters is
 * released before returning: the judger's JVM is long-lived, and an unreleased copy is C heap that
 * is never freed (one per string per run).
 * @param  jniEnv - a reference to the JNI runtime environment
 * @param  jStr   - the Java string whose value is to be retrieved (may be null)
 * @return the value of the Java string, or an empty string for null (or if the JVM is out of
 *         memory, in which case an OutOfMemoryError is pending)
 */
inline std::string getStringValue(JNIEnv* jniEnv, jstring jStr) {
    if ( jStr == nullptr ) {
        return std::string();
    }
    const char* chars = jniEnv->GetStringUTFChars(jStr, nullptr);
    if ( chars == nullptr ) {
        return std::string();
    }
    std::string value(chars);
    jniEnv->ReleaseStringUTFChars(jStr, chars);
    return value;
}

/**
 * Throws a java.lang.IllegalStateException into the Java runtime environment. It is a
 * RuntimeException, so the caller's ordinary exception handling sees a sandbox failure, whereas a
 * java.lang.Error (meant for unrecoverable JVM conditions) escaped catch (Exception). The native
 * code must still return right after calling this.
 * @param  jniEnv  - a reference to the JNI runtime environment
 * @param  message - the exception message
 * @return 0 on success, a negative value if the exception could not be thrown
 */
inline jint throwStringException(JNIEnv* jniEnv, const std::string& message) {
    jclass exClass = jniEnv->FindClass("java/lang/IllegalStateException");
    if ( exClass == nullptr ) {
        return -1; // a NoClassDefFoundError is pending instead
    }
    return jniEnv->ThrowNew(exClass, message.c_str());
}

#endif
