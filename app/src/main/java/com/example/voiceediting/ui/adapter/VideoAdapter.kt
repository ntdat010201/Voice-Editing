package com.example.voiceediting.ui.adapter

import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.voiceediting.R
import com.example.voiceediting.ui.model.ModelVideo

class VideoAdapter(private val onClick: (ModelVideo) -> Unit) :
    ListAdapter<ModelVideo, VideoAdapter.VideoViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }


    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val item = getItem(position)

        holder.time.text = formatTime(item.duration)
        holder.itemView.setOnClickListener { onClick(item) }
        Glide.with(holder.itemView.context)
            .load(item.uri)
            .centerCrop()
            .into(holder.frame)
    }

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val time: TextView = view.findViewById(R.id.time_video)
        val frame: ImageView = view.findViewById(R.id.video)
    }


    object DiffCallback : DiffUtil.ItemCallback<ModelVideo>() {
        override fun areItemsTheSame(old: ModelVideo, new: ModelVideo) =
            // So sánh ID hoặc đường dẫn để chính xác hơn title
            old.path == new.path && old.path == new.path

        override fun areContentsTheSame(old: ModelVideo, new: ModelVideo) =
            old == new
    }

    private fun formatTime(duration: Long): String {
        val seconds = (duration / 1000) % 60
        val minutes = (duration / (1000 * 60)) % 60
        val hours = (duration / (1000 * 60 * 60))

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
