package com.example.voiceediting.ui.model

data class ModelAudio(
    val title: String,
    val uri: String,
    val duration: Long,
    val artUri: String,
    val artist: String? = "Unknown",
    val path: String
)