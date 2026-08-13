#include <jni.h>

extern "C" JNIEXPORT jint JNICALL
Java_com_papersama_filmvault_MainActivity_nativeMarker(JNIEnv *, jobject) {
    return 64;
}
