package com.example.voiceediting.ui.model

import com.example.voiceediting.ui.enum.ItemType
import java.io.File

data class DisplayItem(
    val title: String,
    val subTitle: String,
    val type: ItemType,
    val audioData: ModelAudio? = null,        // Có dữ liệu nếu là bài hát
    val subList: List<ModelAudio>? = null,    // Có danh sách nếu là Nhóm (Nghệ sĩ/Thư mục)
    val file: File? = null            // Dùng cho mục Tệp tin khác
)