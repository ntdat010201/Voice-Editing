package com.example.voiceediting.ui.utils

import android.content.Context
import android.content.SharedPreferences

object PreferencesUtils {

    private const val PREFS_NAME = "audio_permission_prefs"
    private const val KEY_AUDIO_DENIAL_COUNT = "audio_denial_count"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Lấy số lần từ chối quyền audio
    fun getAudioDenialCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_AUDIO_DENIAL_COUNT, 0)
    }

    // Tăng số lần từ chối lên 1
    fun incrementAudioDenialCount(context: Context) {
        val current = getAudioDenialCount(context)
        getPrefs(context).edit().putInt(KEY_AUDIO_DENIAL_COUNT, current + 1).apply()
    }

    // Reset về 0 (khi người dùng cấp quyền thành công)
    fun resetAudioDenialCount(context: Context) {
        getPrefs(context).edit().putInt(KEY_AUDIO_DENIAL_COUNT, 0).apply()
    }

    // Nếu cần thêm các key khác sau này, cứ thêm hàm tương tự ở đây
}