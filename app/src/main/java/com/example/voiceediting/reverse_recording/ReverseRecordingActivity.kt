package com.example.voiceediting.reverse_recording

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat as AndroidAudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaRecorder
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.voiceediting.databinding.ActivityReverseRecordingBinding
import com.example.voiceediting.edit_audio.permission.AudioPickerPermission
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.nio.ByteOrder
import java.nio.ShortBuffer

class ReverseRecordingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReverseRecordingBinding
    private lateinit var audioPickerPermission: AudioPickerPermission

    private var recorder: MediaRecorder? = null
    private var exoPlayer: ExoPlayer? = null
    private var audioFile: File? = null

    private var isRecording = false
    private var isPlayingNormal = false
    private var isPlayingReverse = false
    private var isPausedReverse = false

    private var startTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormatter = SimpleDateFormat("mm:ss", Locale.getDefault())
    private var reverseTrack: AudioTrack? = null
    private var reverseSampleRate: Int = 0
    private var reverseChannelMask: Int = AndroidAudioFormat.CHANNEL_OUT_STEREO
    private var reverseWriteThread: Thread? = null
    private var reversePcmFile: File? = null
    private var reverseTotalSamples: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReverseRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        initView()
        initListener()
    }

    private fun initData() {
        // File tạm định dạng .m4a (AAC) - tốt cho ExoPlayer
        audioFile = File(cacheDir, "recording_temp.m4a")

        // Khởi tạo ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        stopPlaying()
                        updatePlayButtonIcon()
                        updateReverseButtonIcon()
                    }
                }
            })
        }

        audioPickerPermission = AudioPickerPermission(this) { uri ->
            stopRecording()
            stopPlaying()
            playFromUri(uri, normal = true)
        }
    }

    private fun initView() {
        updateTimeDisplay(0)
        updatePlayButtonIcon()
        updateReverseButtonIcon()
        binding.record.text = "ghi"
    }

    private fun initListener() {
        // Nút ghi âm / dừng ghi
        binding.record.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        // Nút phát / dừng bình thường
        binding.playAndPause.setOnClickListener {
            if (audioFile?.exists() == true ||  exoPlayer?.currentMediaItem != null) {
                if (isPlayingReverse) {
                    stopPlaying() // Dừng reverse trước nếu đang chạy
                }
                toggleNormalPlay()
            }
        }

        // Nút phát ngược
        binding.reverse.setOnClickListener {
            if (audioFile?.exists() == true ||  exoPlayer?.currentMediaItem!= null) {
                if (isPlayingNormal) {
                    stopPlaying() // Dừng normal trước nếu đang chạy
                }
                toggleReversePlay()
            }
        }

        // Nút chọn file
        binding.addFile.setOnClickListener {
            audioPickerPermission.openPicker()
        }
    }

    private fun startRecording() {
        try {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            startTime = System.currentTimeMillis()
            binding.record.text = "■"
            startTimer()
            stopPlaying()
        } catch (e: Exception) {
            Log.e("Record", "Lỗi ghi âm", e)
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            try { stop() } catch (e: Exception) { /* ignore */ }
            release()
        }
        recorder = null
        isRecording = false
        binding.record.text = "ghi"
        handler.removeCallbacks(updateTimerRunnable)
    }

    private fun startTimer() {
        handler.post(updateTimerRunnable)
    }

    private val updateTimerRunnable: Runnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val elapsed = System.currentTimeMillis() - startTime
                updateTimeDisplay(elapsed)
                handler.postDelayed(this, 500)
            }
        }
    }

    private fun updateTimeDisplay(millis: Long) {
        binding.timeRecord.text = timeFormatter.format(millis)
    }

    private fun preparePlayerFromFile(normal: Boolean) {
        audioFile?.let {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(it))
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            if (normal) {
                exoPlayer?.playbackParameters = PlaybackParameters(1f) // Normal speed
            }
        }
    }

    private fun playFromUri(uri: Uri, normal: Boolean) {
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        if (normal) {
            exoPlayer?.playbackParameters = PlaybackParameters(1f)
            exoPlayer?.play()
            isPlayingNormal = true
            updatePlayButtonIcon()
        }
    }

    private fun toggleNormalPlay() {
        if (isPlayingNormal) {
            exoPlayer?.pause()
            isPlayingNormal = false
        } else {
            if (exoPlayer?.playbackState == Player.STATE_ENDED || exoPlayer?.playbackParameters?.speed != 1f) {
                preparePlayerFromFile(normal = true)
            }
            exoPlayer?.play()
            isPlayingNormal = true
            isPlayingReverse = false
            stopReversePlayback()
            updateReverseButtonIcon()
        }
        updatePlayButtonIcon()
    }

    @SuppressLint("Range")
    private fun toggleReversePlay() {
        if (isPlayingReverse) {
            isPausedReverse = true
            reverseTrack?.pause()
            isPlayingReverse = false
        } else {
            ensureReverseBufferReady()
            startReversePlayback()
            isPlayingReverse = true
            isPlayingNormal = false
            updatePlayButtonIcon()
        }
        updateReverseButtonIcon()
    }

    private fun stopPlaying() {
        exoPlayer?.stop()
        isPlayingNormal = false
        isPlayingReverse = false
        stopReversePlayback()
        updatePlayButtonIcon()
        updateReverseButtonIcon()
    }

    private fun updatePlayButtonIcon() {
        binding.playAndPause.text = if (isPlayingNormal) "■" else "▶"
    }

    private fun updateReverseButtonIcon() {
        binding.reverse.text = if (isPlayingReverse) "■⏪" else "⏪"
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause() // Pause khi app background
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        exoPlayer?.release()
        handler.removeCallbacksAndMessages(null)
        stopReversePlayback()
    }

    private fun ensureReverseBufferReady() {
        if (reversePcmFile?.exists() == true && reverseTotalSamples > 0) return
        val sourceUri: Uri = exoPlayer?.currentMediaItem?.localConfiguration?.uri
            ?: audioFile?.takeIf { it.exists() }?.let { Uri.fromFile(it) }
            ?: return
        val extractor = MediaExtractor()
        extractor.setDataSource(this, sourceUri, null)
        var audioTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                extractor.selectTrack(i)
                reverseSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                reverseChannelMask = if (channelCount == 1) AndroidAudioFormat.CHANNEL_OUT_MONO else AndroidAudioFormat.CHANNEL_OUT_STEREO
                break
            }
        }
        val format = extractor.getTrackFormat(audioTrackIndex)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()
        reversePcmFile = File(cacheDir, "reverse_pcm.pcm")
        val fos = reversePcmFile!!.outputStream().buffered()
        val bufferInfo = MediaCodec.BufferInfo()
        var totalWrittenBytes: Long = 0
        while (true) {
            val inputIndex = codec.dequeueInputBuffer(10000)
            if (inputIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputIndex)!!
                val size = extractor.readSampleData(inputBuffer, 0)
                if (size < 0) {
                    codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex >= 0) {
                val outBuf = codec.getOutputBuffer(outputIndex)!!
                outBuf.order(ByteOrder.nativeOrder())
                val shortCount = bufferInfo.size / 2
                val sb: ShortBuffer = outBuf.asShortBuffer()
                val tmp = ShortArray(shortCount)
                sb.get(tmp)
                val bb = java.nio.ByteBuffer.allocate(shortCount * 2).order(ByteOrder.nativeOrder())
                bb.asShortBuffer().put(tmp)
                fos.write(bb.array())
                totalWrittenBytes += (shortCount * 2).toLong()
                codec.releaseOutputBuffer(outputIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
        fos.flush()
        fos.close()
        codec.stop()
        codec.release()
        extractor.release()
        reverseTotalSamples = totalWrittenBytes / 2
    }

    private fun startReversePlayback() {
        val pcm = reversePcmFile ?: return
        val minBuf = AudioTrack.getMinBufferSize(
            reverseSampleRate,
            reverseChannelMask,
            AndroidAudioFormat.ENCODING_PCM_16BIT
        )
        reverseTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AndroidAudioFormat.Builder()
                .setSampleRate(reverseSampleRate)
                .setChannelMask(reverseChannelMask)
                .setEncoding(AndroidAudioFormat.ENCODING_PCM_16BIT)
                .build(),
            minBuf,
            AudioTrack.MODE_STREAM,
            AudioTrack.WRITE_NON_BLOCKING
        ).apply { play() }

        isPausedReverse = false
        reverseWriteThread = Thread {
            val raf = java.io.RandomAccessFile(pcm, "r")
            val blockSamples = minBuf / 2
            var remaining = reverseTotalSamples
            val byteBlock = ByteArray(blockSamples * 2)
            while (!isPausedReverse && remaining > 0) {
                val toReadSamples = if (remaining >= blockSamples) blockSamples else remaining.toInt()
                val toReadBytes = toReadSamples * 2
                val readPosBytes = (remaining - toReadSamples) * 2
                raf.seek(readPosBytes)
                val read = raf.read(byteBlock, 0, toReadBytes)
                if (read <= 0) break
                val sb = java.nio.ByteBuffer.wrap(byteBlock, 0, read).order(ByteOrder.nativeOrder()).asShortBuffer()
                val shorts = ShortArray(sb.remaining())
                sb.get(shorts)
                reverseTrack?.write(shorts, 0, shorts.size)
                remaining -= toReadSamples
            }
            raf.close()
            isPlayingReverse = false
            updateReverseButtonIcon()
            reverseTrack?.stop()
        }.apply { start() }
    }

    private fun stopReversePlayback() {
        isPausedReverse = true
        reverseWriteThread?.interrupt()
        reverseWriteThread = null
        reverseTrack?.stop()
        reverseTrack?.release()
        reverseTrack = null
    }
}
