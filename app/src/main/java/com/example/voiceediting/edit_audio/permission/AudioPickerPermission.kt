package com.example.voiceediting.edit_audio.permission

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class AudioPickerPermission(
    private val activity: ComponentActivity,
    private val onMediaSelected: (Uri) -> Unit  // Đổi tên cho rõ là audio/video
) {

    private val mediaPickerLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                // Lấy quyền persistable để truy cập lâu dài (quan trọng cho SAF)
                try {
                    activity.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Một số provider không hỗ trợ persistable, bỏ qua lỗi
                }

                onMediaSelected(it)
            }
        }

    fun openPicker() {
        // Cho phép chọn cả audio và video
        mediaPickerLauncher.launch(arrayOf("audio/*", "video/*"))
    }
}