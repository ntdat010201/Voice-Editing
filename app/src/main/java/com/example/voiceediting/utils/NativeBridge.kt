package com.example.voiceediting.utils

@Suppress("KotlinJniMissingFunction")
object NativeBridge {
    init {
        System.loadLibrary("voice_engine")
    }

    // chuẩn bị
    external fun start()

    //  loại âm thanh
    external fun setPitch(pitch: Float)

    //pause
    external fun clearBuffer()

    // dừng hẳn
    external fun stop()

    external fun pushPcm(data: FloatArray, frames: Int)

}


