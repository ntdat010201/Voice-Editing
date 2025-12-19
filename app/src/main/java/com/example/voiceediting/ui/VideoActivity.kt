package com.example.voiceediting.ui

import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.voiceediting.R
import com.example.voiceediting.databinding.ActivityVideoBinding
import com.example.voiceediting.ui.adapter.MusicAdapter
import com.example.voiceediting.ui.adapter.VideoAdapter
import com.example.voiceediting.ui.model.DisplayItem
import com.example.voiceediting.ui.model.ModelAudio
import com.example.voiceediting.ui.model.ModelVideo

class VideoActivity : AppCompatActivity() {
    private lateinit var binding : ActivityVideoBinding

    private val allVideos = mutableListOf<ModelVideo>()
    private lateinit var adapter: VideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
    }

    private fun initData() {
        adapter = VideoAdapter { video ->
            playVideo(video)
        }
        binding.rcvVideo.layoutManager = GridLayoutManager(this, 3)
        binding.rcvVideo.adapter = adapter
        loadAllVideos()

    }

    private fun playVideo(video: ModelVideo) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(android.net.Uri.parse(video.uri), "video/*")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    private fun loadAllVideos() {
        allVideos.clear()
        val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        // Chỉ lấy 3 cột cần thiết
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA
        )

        val cursor = contentResolver.query(uri, projection, null, null, null)

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

            while (c.moveToNext()) {
                val videoUri = ContentUris.withAppendedId(uri, c.getLong(idCol)).toString()

                allVideos.add(ModelVideo(
                    uri = videoUri,
                    duration = c.getLong(durationCol),
                    path = c.getString(dataCol)
                ))
            }
        }
        showVideo()
    }

    private fun showVideo() {
        adapter.submitList(allVideos)
    }

}