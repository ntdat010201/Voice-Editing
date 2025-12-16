#include <jni.h>
#include "VoiceEngine.h"

static VoiceEngine engine;

extern "C" JNIEXPORT void JNICALL
Java_com_example_voiceediting_utils_NativeBridge_start(
        JNIEnv*, jobject) {
    engine.start();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_voiceediting_utils_NativeBridge_stop(
        JNIEnv*, jobject) {
    engine.stop();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_voiceediting_utils_NativeBridge_setPitch(
        JNIEnv*, jobject, jfloat pitch) {
    engine.setPitch(pitch);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_voiceediting_utils_NativeBridge_pushPcm(
        JNIEnv* env, jobject, jfloatArray data, jint frames) {
    jfloat* pcm = env->GetFloatArrayElements(data, nullptr);
    engine.pushPcm(pcm, frames);
    env->ReleaseFloatArrayElements(data, pcm, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_voiceediting_utils_NativeBridge_clearBuffer(
        JNIEnv*, jobject) {
    engine.clearBuffer();
}
