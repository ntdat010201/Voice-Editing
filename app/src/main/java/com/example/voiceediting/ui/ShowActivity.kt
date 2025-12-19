package com.example.voiceediting.ui

import android.content.ContentUris
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.voiceediting.databinding.ActivityShowBinding
import com.example.voiceediting.ui.adapter.MusicAdapter
import com.example.voiceediting.ui.enum.ItemType
import com.example.voiceediting.ui.model.DisplayItem
import com.example.voiceediting.ui.model.ModelAudio
import java.io.File

class ShowActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShowBinding
    private val allSongs = mutableListOf<ModelAudio>() // Danh sách gốc quét từ máy
    private lateinit var adapter: MusicAdapter
    private val navigationStack = mutableListOf<List<DisplayItem>>() // Để xử lý nút Back

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadAllSongs() // Quét dữ liệu từ máy

        // Gán sự kiện Click cho các TextView (Tab phổ thông)
        binding.btnTabSongs.setOnClickListener {
            highlightTab(binding.btnTabSongs)
            showSongs()
        }
        binding.btnTabArtists.setOnClickListener {
            highlightTab(binding.btnTabArtists)
            showArtists()
        }
        binding.btnTabFolders.setOnClickListener {
            highlightTab(binding.btnTabFolders)
            showFolders()
        }
        binding.btnTabFiles.setOnClickListener {
            highlightTab(binding.btnTabFiles)
            showFileSystem(Environment.getExternalStorageDirectory())
        }

        // Mặc định chọn Tab đầu tiên
        highlightTab(binding.btnTabSongs)
    }

    private fun highlightTab(selectedTab: TextView) {
        val tabs = listOf(
            binding.btnTabSongs,
            binding.btnTabArtists,
            binding.btnTabFolders,
            binding.btnTabFiles
        )
        tabs.forEach { it.setTextColor(android.graphics.Color.GRAY) } // Reset tất cả về màu xám
        selectedTab.setTextColor(android.graphics.Color.BLACK) // Tab được chọn màu đen
    }

    private fun setupRecyclerView() {
        adapter = MusicAdapter { item ->
            when (item.type) {
                ItemType.SONG ->{} /*playMusic(item.audioData!!)*/
                ItemType.GROUP -> {
                    // Khi click vào Nghệ sĩ hoặc Thư mục -> Hiện danh sách bài hát con
                    val children = item.subList?.map {
                        DisplayItem(it.title, it.artist ?: "Unknown", ItemType.SONG, it)
                    } ?: emptyList()
                    updateList(children)
                }

                ItemType.FILE_SYSTEM -> {
                    if (item.file?.isDirectory == true) showFileSystem(item.file)
                    else if (item.file?.name?.endsWith(".mp3") == true) {
                        /*playFile(item.file)*/
                    }
                }
            }
        }
        binding.recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    // --- LOGIC PHÂN LOẠI ---

    private fun showSongs() {
        val list = allSongs.map { DisplayItem(it.title, it.artist ?: "Unknown", ItemType.SONG, it) }
        updateList(list, clearStack = true)
    }

    private fun showArtists() {
        val list = allSongs.groupBy { it.artist ?: "Unknown" }.map { (name, songs) ->
            DisplayItem(name, "${songs.size} bài hát", ItemType.GROUP, subList = songs)
        }
        updateList(list, clearStack = true)
    }

    private fun showFolders() {
        val list = allSongs.groupBy { it.path.substringBeforeLast("/") }.map { (path, songs) ->
            DisplayItem(
                path.substringAfterLast("/"),
                "${songs.size} bài hát",
                ItemType.GROUP,
                subList = songs
            )
        }
        updateList(list, clearStack = true)
    }

    private fun showFileSystem(dir: File) {
        val files = dir.listFiles()?.map {
            DisplayItem(it.name, it.absolutePath, ItemType.FILE_SYSTEM, file = it)
        }?.sortedBy { !it.file!!.isDirectory } ?: emptyList()
        updateList(files)
    }

    // --- QUẢN LÝ HIỂN THỊ ---

    private fun updateList(newList: List<DisplayItem>, clearStack: Boolean = false) {
        if (clearStack) navigationStack.clear()
        navigationStack.add(newList)
        adapter.submitList(newList)
    }

    override fun onBackPressed() {
        if (navigationStack.size > 1) {
            navigationStack.removeAt(navigationStack.size - 1)
            adapter.submitList(navigationStack.last())
        } else {
            super.onBackPressed()
        }
    }


    private fun loadAllSongs() {
        allSongs.clear()

        // Truy cập vào vùng nhớ ngoài
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        // Tuyển tập các cột dữ liệu quan trọng
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,          // ID duy nhất
            MediaStore.Audio.Media.TITLE,        // Tên bài hát
            MediaStore.Audio.Media.ARTIST,       // Nghệ sĩ
            MediaStore.Audio.Media.DURATION,     // Thời lượng (ms)
            MediaStore.Audio.Media.DATA,         // Đường dẫn file vật lý (Để lọc Folder)
            MediaStore.Audio.Media.ALBUM_ID      // ID album (Để lấy ảnh cover)
        )

        // Lọc: Chỉ lấy file là nhạc, loại bỏ các file ghi âm hoặc nhạc chuông ngắn (ví dụ dưới 5s)
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        // Sử dụng query để lấy dữ liệu
        val cursor = contentResolver.query(uri, projection, selection, null, sortOrder)

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val title = c.getString(titleCol) ?: "Unknown Title"
                val artist = c.getString(artistCol) ?: "Unknown Artist"
                val duration = c.getLong(durationCol)
                val path = c.getString(dataCol) ?: ""
                val albumId = c.getLong(albumIdCol)

                // 1. Tạo Content URI để phát nhạc (Cách này chạy tốt trên Android 10+ Scoped Storage)
                val musicUri =
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                        .toString()

                // 2. Tạo URI ảnh bìa (Chuẩn cho mọi bản Android)
                val artUri = ContentUris.withAppendedId(
                    android.net.Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                // 3. Đưa vào Model
                allSongs.add(
                    ModelAudio(
                        title = title,
                        uri = musicUri,
                        duration = duration,
                        artUri = artUri,
                        artist = artist,
                        path = path
                    )
                )
            }
        }

        // Sau khi load xong, hiển thị danh sách bài hát ngay lập tức
        showSongs()
    }

    fun formatTime(duration: Long): String {
        val sec = (duration / 1000) % 60
        val min = (duration / (1000 * 60)) % 60
        return String.format("%02d:%02d", min, sec)
    }
}