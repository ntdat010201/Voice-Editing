package com.example.voiceediting

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.AudioTrack
import android.media.AudioAttributes
import android.media.AudioFormat as AndroidAudioFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.voiceediting.databinding.ActivityMainBinding
import com.example.voiceediting.permission.AudioPickerPermission
import com.example.voiceediting.utils.NativeBridge
import java.util.concurrent.Executors
import android.media.MediaPlayer
import android.media.PlaybackParams
import java.nio.ByteOrder


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var audioPickerPermission: AudioPickerPermission
    private val executorService = Executors.newSingleThreadExecutor()

    private var isPaused = false
    private var isPlaying = false
    private var mediaPlayer: MediaPlayer? = null
    private var currentPitch: Float = 1.0f
    private var audioTrack: AudioTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initView()
        initListener()
    }

    private fun playAudioWithMediaPlayer(uri: Uri) {
        if (isPlaying) return
        isPlaying = true
        isPaused = false
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@MainActivity, uri)
            setOnCompletionListener {
                this@MainActivity.isPlaying = false
                this@MainActivity.isPaused = false
            }
            prepare()
            val params = PlaybackParams()
            params.setSpeed(1.0f)
            params.setPitch(currentPitch)
            playbackParams = params
            start()
        }
    }
    private fun initData() {
        NativeBridge.start()
        audioPickerPermission = AudioPickerPermission(this) { uri ->
            Log.d("DAT", "initData: " + "URI")
            playAudioWithMediaPlayer(uri)
        }

    }

    private fun initView() {

    }

    private fun initListener() {
        binding.addFile.setOnClickListener {
            if (isPlaying) {
                pauseAudio()
            }
            audioPickerPermission.openAudioPicker()
        }

        binding.pause.setOnClickListener {
            if (isPlaying && !isPaused) {
                pauseAudio()
            } else if (isPlaying && isPaused) {
                resumeAudio()
            }
        }

        binding.conTrai.setOnClickListener {
            applyPitch(0.8f)
        }

        binding.conGai.setOnClickListener {
            applyPitch(1.3f)
        }

        binding.treCon.setOnClickListener {
            applyPitch(1.6f)
        }

    }

    private fun playAudio(uri: Uri) {
        // Nếu đã có luồng đang chạy, không chạy lại (hoặc dừng luồng cũ trước)
        if (isPlaying) return
        isPlaying = true
        isPaused = false

        executorService.execute {
            var extractor: MediaExtractor? = null
            var codec: MediaCodec? = null

            try {
                extractor = MediaExtractor()
                extractor.setDataSource(this, uri, null)

                // --- 1. Tìm và Cấu hình Audio Track/Codec ---
                var audioTrackIndex = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        extractor.selectTrack(i)
                        break
                    }
                }

                val format = extractor.getTrackFormat(audioTrackIndex)
                val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                val channelMask =
                    if (channelCount == 1) AndroidAudioFormat.CHANNEL_OUT_MONO
                    else AndroidAudioFormat.CHANNEL_OUT_STEREO

                codec = MediaCodec.createDecoderByType(
                    format.getString(MediaFormat.KEY_MIME)!!
                )
                codec.configure(format, null, null, 0)
                codec.start()

                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    channelMask,
                    AndroidAudioFormat.ENCODING_PCM_16BIT
                )
                audioTrack = AudioTrack(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                    AndroidAudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelMask)
                        .setEncoding(AndroidAudioFormat.ENCODING_PCM_16BIT)
                        .build(),
                    minBuf,
                    AudioTrack.MODE_STREAM,
                    AudioTrack.WRITE_NON_BLOCKING
                ).apply { play() }

                // --- 2. Vòng lặp Giải mã Chính (có kiểm tra Pause) ---
                val bufferInfo = MediaCodec.BufferInfo()
                // Ghi ra audio device bằng PCM 16-bit

                var presentationTimeUs: Long = 0

                while (isPlaying) { // Chạy cho đến khi dừng hoặc kết thúc
                    if (isPaused) {
                        Thread.sleep(50) // Chờ ngắn khi bị tạm dừng
                        continue
                    }

                    // --- Đẩy dữ liệu vào Codec ---
                    val inputIndex = codec.dequeueInputBuffer(10000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)!!
                        val size = extractor.readSampleData(inputBuffer, 0)

                        if (size < 0) {
                            // Báo hiệu kết thúc luồng cho Codec
                            codec.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                        } else {
                            presentationTimeUs = extractor.sampleTime
                            codec.queueInputBuffer(
                                inputIndex, 0, size,
                                presentationTimeUs, 0
                            )
                            extractor.advance()
                        }
                    }

                    // --- Lấy dữ liệu đã giải mã ra ---
                    val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                    if (outputIndex >= 0) {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            break // Thoát vòng lặp khi hết file
                        }

                        val out = codec.getOutputBuffer(outputIndex)!!
                        out.order(ByteOrder.nativeOrder())
                        val shortCount = bufferInfo.size / 2
                        val pcmShorts = ShortArray(shortCount)
                        out.asShortBuffer().get(pcmShorts)
                        audioTrack?.write(pcmShorts, 0, pcmShorts.size)

                        codec.releaseOutputBuffer(outputIndex, false)
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // Xử lý khi format đầu ra thay đổi
                        // Log.d(TAG, "Output format changed: ${codec.outputFormat}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // --- 3. DỌN DẸP TÀI NGUYÊN ---
                isPlaying = false // Đánh dấu kết thúc chơi nhạc
                isPaused = false
                codec?.stop()
                codec?.release()
                extractor?.release()
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
            }
        }
    }

    private fun pauseAudio() {
        if (isPlaying && !isPaused) {
            isPaused = true
            audioTrack?.pause()
            NativeBridge.clearBuffer()
            mediaPlayer?.pause()
        }
    }

    private fun resumeAudio() {
        if (isPlaying && isPaused) {
            isPaused = false
            audioTrack?.play()
            mediaPlayer?.start()
        }
    }

    private fun applyPitch(pitch: Float) {
        currentPitch = pitch
        mediaPlayer?.let {
            val params = it.playbackParams ?: PlaybackParams()
            params.setSpeed(1.0f)
            params.setPitch(pitch)
            it.playbackParams = params
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NativeBridge.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
