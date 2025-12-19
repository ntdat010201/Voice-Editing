package com.example.voiceediting.ui.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.voiceediting.ui.utils.PreferencesUtils

class AudioPermissionHelper(
    private val context: Context,
    private val onPermissionGranted: () -> Unit
) {

    private val requiredPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    fun registerLauncher(launcher: ActivityResultLauncher<String>) {
        this.requestPermissionLauncher = launcher
    }

    fun checkAndRequest() {
        when {
            isPermissionGranted() -> {
                onPermissionGranted()
            }
            PreferencesUtils.getAudioDenialCount(context) >= 2 -> {
                showGoToSettingsDialog()
            }
            else -> {
                requestPermission()
            }
        }
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, requiredPermission) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        requestPermissionLauncher.launch(requiredPermission)
    }

    fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            PreferencesUtils.resetAudioDenialCount(context)
            onPermissionGranted()
        } else {
            PreferencesUtils.incrementAudioDenialCount(context)
            if (PreferencesUtils.getAudioDenialCount(context) >= 2) {
                showGoToSettingsDialog()
            } else {
                showRationaleDialog()
            }
        }
    }

    private fun showRationaleDialog() {
        AlertDialog.Builder(context)
            .setTitle("Cần quyền truy cập âm thanh")
            .setMessage("Ứng dụng cần quyền đọc file audio để hiển thị danh sách nhạc của bạn. Bạn có thể cấp quyền trong lần tiếp theo.")
            .setPositiveButton("Đã hiểu", null)
            .setCancelable(true)
            .show()
    }

    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(context)
            .setTitle("Quyền bị từ chối")
            .setMessage("Bạn đã từ chối quyền nhiều lần. Vui lòng vào Cài đặt ứng dụng và bật quyền đọc file audio thủ công.")
            .setPositiveButton("Mở Cài đặt") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Hủy", null)
            .setCancelable(true)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
}