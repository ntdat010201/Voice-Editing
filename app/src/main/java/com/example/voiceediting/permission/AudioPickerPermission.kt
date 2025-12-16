package com.example.voiceediting.permission

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class AudioPickerPermission(
    private val activity: ComponentActivity,
    private val onAudioSelected: (Uri) -> Unit
) {

    private val audioPickerLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                activity.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                onAudioSelected(it)
            }
        }

    fun openAudioPicker() {
        audioPickerLauncher.launch(arrayOf("audio/*"))
    }
}
