package com.example.animera.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.animera.data.model.VideoServer
import com.example.animera.databinding.ItemServerChipBinding

class ServerAdapter(private val onServerClick: (VideoServer) -> Unit) :
    ListAdapter<VideoServer, ServerAdapter.ViewHolder>(DiffCallback()) {

    private var selectedPosition = 0

    inner class ViewHolder(private val binding: ItemServerChipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(server: VideoServer, isSelected: Boolean) {
            binding.chipServer.text = server.name
            binding.chipServer.isChecked = isSelected
            binding.chipServer.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val oldPos = selectedPosition
                    selectedPosition = adapterPosition
                    notifyItemChanged(oldPos)
                    notifyItemChanged(selectedPosition)
                    onServerClick(server)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServerChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }

    class DiffCallback : DiffUtil.ItemCallback<VideoServer>() {
        override fun areItemsTheSame(oldItem: VideoServer, newItem: VideoServer) = oldItem.url == newItem.url
        override fun areContentsTheSame(oldItem: VideoServer, newItem: VideoServer) = oldItem == newItem
    }
}
