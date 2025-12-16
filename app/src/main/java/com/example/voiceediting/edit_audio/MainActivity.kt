package com.example.voiceediting.edit_audio

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.voiceediting.databinding.ActivityMainBinding
import com.example.voiceediting.edit_audio.permission.AudioPickerPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var audioPickerPermission: AudioPickerPermission

    private var exoPlayer: ExoPlayer? = null
    private var isPaused = false
    private var currentPitch: Float = 1.0f

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
        // Dừng nếu đang phát
        exoPlayer?.stop()

        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()

        // QUAN TRỌNG: Reset về giọng gốc trước khi phát
        currentPitch = 1.0f
        applyPitch(1.0f)  // Đảm bảo pitch = 1.0f

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
            audioPickerPermission.openPicker()  // Đổi tên hàm cho rõ
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

        binding.conTrai.setOnClickListener { applyPitchSmooth(0.8f) }
        binding.conGai.setOnClickListener { applyPitchSmooth(1.3f) }
        binding.treCon.setOnClickListener { applyPitchSmooth(1.6f) }
    }

    private fun applyPitch(pitch: Float) {
        currentPitch = pitch
        val params = PlaybackParameters(1.0f, pitch)  // speed=1.0 để video không nhanh/chậm
        exoPlayer?.playbackParameters = params
    }

    private fun applyPitchSmooth(targetPitch: Float, durationMs: Int = 300) {
        // Nếu đang cùng pitch thì bỏ qua
        if (currentPitch == targetPitch) return

        val startPitch = currentPitch
        val startTime = System.currentTimeMillis()

        // Dùng coroutine + repeatOnLifecycle để tự động cancel khi destroy
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var elapsed: Long
                var progress: Float

                do {
                    elapsed = System.currentTimeMillis() - startTime
                    progress = (elapsed.toFloat() / durationMs).coerceAtMost(1f)

                    val interpolatedPitch = startPitch + (targetPitch - startPitch) * progress
                    exoPlayer?.playbackParameters = PlaybackParameters(1.0f, interpolatedPitch)

                    delay(16)  // ~60fps, mượt mà
                } while (progress < 1f)

                // Kết thúc transition
                currentPitch = targetPitch
            }
        }
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}