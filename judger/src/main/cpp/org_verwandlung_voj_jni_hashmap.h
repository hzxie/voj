/**
 * HashMap class for JNI.
 * Ref: https://gist.github.com/MasseGuillaume/2500529
 * 
 * @author Guillaume Masse
 */
#include <map>
#include <string>

#include <jni.h>
 /* Header for class org_verwandlung_voj_jni_library */

#ifndef _Included_org_verwandlung_voj_jni_hashmap
#define _Included_org_verwandlung_voj_jni_hashmap
#ifdef __cplusplus
extern "C" {
#endif

class JHashMap {
    public:
        void put(std::string, jint);
        jobject toJObject(JNIEnv*);
    private:
        jobject getJIntegerObject(JNIEnv*, jint);
        std::map<std::string, jint> _hashMap;
};

/**
 * 向JHashMap中存入数值.
 * @param key   - HashMap的Key
 * @param value - HashMap的Value
 */
void JHashMap::put(std::string key, jint value) {
    _hashMap.insert(std::pair<std::string, jint>(key, value));
}

/**
 * 将C++中的Map转换为Java中的HashMap对象.
 * @param  jniEnv - JNI 运行环境引用
 * @return 对应HashMap对象的jobject对象
 */
jobject JHashMap::toJObject(JNIEnv* jniEnv) {
    jclass    mapClass  = jniEnv->FindClass("java/util/HashMap");
    jmethodID initMap   = jniEnv->GetMethodID(mapClass, "<init>", "()V");
    jobject   hashMap   = jniEnv->NewObject(mapClass, initMap);

    jmethodID putMethod = jniEnv->GetMethodID(
        mapClass, 
        "put", 
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
    );

    for( auto itr = _hashMap.begin(); itr != _hashMap.end(); ++ itr ) {
        jstring key     = jniEnv->NewStringUTF(itr->first.c_str());
        jobject value   = getJIntegerObject(jniEnv, itr->second);

        jniEnv->CallObjectMethod(
            hashMap, 
            putMethod,
            key,
            value
        );
    }
    return hashMap;
}

/**
 * 将jint转换为java/lang/Integer对象.
 * @param  jniEnv - JNI 运行环境引用
 * @param  value  - 待转换的值
 * @return 对应的java/lang/Integer对象
 */
jobject JHashMap::getJIntegerObject(JNIEnv* jniEnv, jint value) {
    jclass    intClass = jniEnv->FindClass("java/lang/Integer");
    jmethodID initInt  = jniEnv->GetMethodID(intClass, "<init>", "(I)V");
    jobject   integer  = jniEnv->NewObject(intClass, initInt, value);

    return integer;
}

#ifdef __cplusplus
}
#endif
#endif