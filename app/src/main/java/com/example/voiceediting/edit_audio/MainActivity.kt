package com.example.voiceediting.edit_audio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.voiceediting.databinding.ActivityMainBinding
import com.example.voiceediting.edit_audio.permission.AudioPickerPermission
import com.example.voiceediting.reverse_recording.ReverseRecordingActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var audioPickerPermission: AudioPickerPermission

    private var exoPlayer: ExoPlayer? = null
    private var isPaused = false
    private var currentPitch: Float = 1.0f

    private var currentVolume: Float = 1.0f
    private var fanJob: Job? = null
    private var effectJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initExoPlayer()
        initData()
        initListener()
    }

    @OptIn(UnstableApi::class)
    private fun initExoPlayer() {
        exoPlayer = ExoPlayer.Builder(this)
            .build()

        // Gắn PlayerView – tự xử lý audio/video
        binding.playerView.player = exoPlayer
        binding.playerView.useController = true  // hiển thị nút play/pause/seek
        binding.playerView.controllerAutoShow = true
    }

    private fun playMedia(uri: Uri) {
        // 1. Dừng nếu đang phát
        exoPlayer?.stop()

        // 2. QUAN TRỌNG: Hủy ngay hiệu ứng Quạt nếu đang chạy từ bài trước
        fanJob?.cancel()

        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()

        // 3. Reset toàn bộ về giọng gốc sạch sẽ
        currentPitch = 1.0f
        currentVolume = 1.0f

        // Áp dụng trực tiếp thông số gốc cho ExoPlayer
        exoPlayer?.playbackParameters = PlaybackParameters(1.0f, 1.0f) // Reset Speed & Pitch
        exoPlayer?.volume = 1.0f // Reset Volume

        exoPlayer?.play()
        isPaused = false
    }

    private fun initData() {
        // Cho phép chọn cả audio và video
        audioPickerPermission = AudioPickerPermission(this) { uri ->
            Log.d("DAT", "Selected URI: $uri")
            playMedia(uri)  // hàm play với ExoPlayer như trước
        }
    }

    private fun initListener() {
        binding.addFile.setOnClickListener {
            if (exoPlayer?.isPlaying == true) {
                exoPlayer?.pause()
            }
            audioPickerPermission.openPicker()
        }

        binding.pause.setOnClickListener {
            if (exoPlayer?.isPlaying == true) {
                exoPlayer?.pause()
                isPaused = true
            } else if (isPaused) {
                exoPlayer?.play()
                isPaused = false
            }
        }

        binding.conTrai.setOnClickListener {
            applyVoiceEffect(targetPitch = 0.9f, targetSpeed = 1.0f, targetVolume = 1.0f)
        }
        binding.conGai.setOnClickListener {
            applyVoiceEffect(targetPitch = 1.25f, targetSpeed = 1.0f, targetVolume = 1.0f)
        }
        binding.treCon.setOnClickListener {
            applyVoiceEffect(targetPitch = 1.5f, targetSpeed = 1.05f, targetVolume = 1.0f)
        }
        binding.khiHelium.setOnClickListener {
            applyVoiceEffect(targetPitch = 1.8f, targetSpeed = 1.05f, targetVolume = 1.0f)
        }
        binding.tiengOng.setOnClickListener {
            applyVoiceEffect(targetPitch = 1.9f, targetSpeed = 1.2f, targetVolume = 0.95f)
        }
        binding.quaiVat.setOnClickListener {
            applyVoiceEffect(targetPitch = 0.65f, targetSpeed = 0.9f, targetVolume = 1.1f)
        }
        binding.khongLo.setOnClickListener {
            applyVoiceEffect(targetPitch = 0.7f, targetSpeed = 0.9f, targetVolume = 1.2f)
        }
        binding.quyDu.setOnClickListener {
            applyVoiceEffect(targetPitch = 0.6f, targetSpeed = 0.85f, targetVolume = 1.0f)
        }
        binding.tuThan.setOnClickListener {
            applyVoiceEffect(targetPitch = 0.6f, targetSpeed = 0.8f, targetVolume = 1.0f)
        }
        binding.tamLinh.setOnClickListener {
            applyVoiceEffect(targetPitch = 0.85f, targetSpeed = 0.9f, targetVolume = 0.75f)
        }
        binding.nguoiNgoaiHanhTinh.setOnClickListener {
            applyVoiceEffect(targetPitch = 1.6f, targetSpeed = 1.15f, targetVolume = 0.9f)
        }
        binding.dienThoai.setOnClickListener {
            applyVoiceEffect(targetPitch = 1.1f, targetSpeed = 1.0f, targetVolume = 0.65f)
        }
        binding.duoiNuoc.setOnClickListener {
            applyVoiceEffect(targetPitch = 0.85f, targetSpeed = 0.9f, targetVolume = 0.6f)
        }
        binding.robot.setOnClickListener {
            applyVoiceEffect(targetPitch = 1.0f, targetSpeed = 1.0f, targetVolume = 1.0f)
        }
        binding.quat.setOnClickListener {
            applyVoiceEffect(
                targetPitch = 1.05f,
                targetSpeed = 1.0f,
                targetVolume = 1.0f,
                isFanEffect = true
            )
        }
        binding.reverse.setOnClickListener {
            startActivity(Intent(this@MainActivity, ReverseRecordingActivity::class.java))
        }
    }

    private fun applyVoiceEffect(
        targetPitch: Float, //Độ cao giọng nói
        targetSpeed: Float = 1.0f, //Tốc độ phát
        targetVolume: Float = 1.0f, //Cường độ/Âm lượng/to nhỏ
        isFanEffect: Boolean = false, //Hiệu ứng Quạt
    ) {
        // 1. Hủy cả job quạt và job chuyển đổi đang chạy
        fanJob?.cancel()
        effectJob?.cancel()

        // Cập nhật thông số hiện tại
        currentPitch = targetPitch
        currentVolume = targetVolume

        // Áp dụng ngay lập tức (Synchronous) - Tránh spam coroutine khi click nhanh
        exoPlayer?.playbackParameters = PlaybackParameters(targetSpeed, targetPitch)
        exoPlayer?.volume = targetVolume

        // 2. Xử lý hiệu ứng Quạt (Vibrato) nếu có
        if (isFanEffect) {
            fanJob = lifecycleScope.launch {
                try {
                    while (isActive) {
                        exoPlayer?.volume = targetVolume * 0.3f
                        exoPlayer?.playbackParameters =
                            PlaybackParameters(targetSpeed, targetPitch - 0.03f)
                        delay(45)

                        exoPlayer?.volume = targetVolume
                        exoPlayer?.playbackParameters = PlaybackParameters(targetSpeed, targetPitch)
                        delay(45)
                    }
                } finally {
                    // Đảm bảo trả về trạng thái chuẩn khi bị hủy
                    exoPlayer?.volume = targetVolume
                    exoPlayer?.playbackParameters = PlaybackParameters(targetSpeed, targetPitch)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        fanJob?.cancel()
        effectJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}