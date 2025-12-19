package com.example.voiceediting.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.voiceediting.R
import com.example.voiceediting.ui.enum.ItemType
import com.example.voiceediting.ui.model.DisplayItem

class MusicAdapter(private val onClick: (DisplayItem) -> Unit) :
    ListAdapter<DisplayItem, MusicAdapter.MusicViewHolder>(DiffCallback) {

    class MusicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.txtTitle)
        val sub: TextView = view.findViewById(R.id.txtSub)
        val image: ImageView = view.findViewById(R.id.imgArt)
    }

    // Thêm từ khóa override ở đây
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audio, parent, false)
        return MusicViewHolder(view)
    }

    // Thêm từ khóa override ở đây -> getItem(position) sẽ hết báo đỏ
    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val item = getItem(position)
        holder.title.text = item.title
        holder.sub.text = item.subTitle

        when (item.type) {
            ItemType.SONG -> {
                holder.image.setImageResource(R.drawable.bgr_circle)
            }
            ItemType.GROUP -> {
                holder.image.setImageResource(R.drawable.ic_launcher_background)
            }
            ItemType.FILE_SYSTEM -> {
                if (item.file?.isDirectory == true) {
                    holder.image.setImageResource(R.drawable.ic_launcher_background)
                } else {
                    holder.image.setImageResource(R.drawable.ic_launcher_foreground)
                }
            }
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<DisplayItem>() {
        override fun areItemsTheSame(old: DisplayItem, new: DisplayItem) =
            // So sánh ID hoặc đường dẫn để chính xác hơn title
            old.title == new.title && old.type == new.type

        override fun areContentsTheSame(old: DisplayItem, new: DisplayItem) =
            old == new
    }
}