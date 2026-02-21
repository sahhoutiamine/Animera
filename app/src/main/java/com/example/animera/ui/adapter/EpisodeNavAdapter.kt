package com.example.animera.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.animera.R
import com.example.animera.data.model.EpisodeNavigation
import com.example.animera.databinding.ItemEpisodeNavBinding

class EpisodeNavAdapter(private val onEpisodeClick: (EpisodeNavigation) -> Unit) :
    ListAdapter<EpisodeNavigation, EpisodeNavAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemEpisodeNavBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ep: EpisodeNavigation) {
            binding.tvEpTitle.text = ep.title
            
            if (ep.isActive) {
                binding.cardEpisode.strokeColor = ContextCompat.getColor(binding.root.context, R.color.accent_purple)
                binding.tvEpTitle.setTextColor(ContextCompat.getColor(binding.root.context, R.color.accent_purple))
            } else {
                binding.cardEpisode.strokeColor = ContextCompat.getColor(binding.root.context, R.color.card_bg)
                binding.tvEpTitle.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_primary))
            }

            binding.root.setOnClickListener {
                onEpisodeClick(ep)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEpisodeNavBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<EpisodeNavigation>() {
        override fun areItemsTheSame(oldItem: EpisodeNavigation, newItem: EpisodeNavigation) = oldItem.url == newItem.url
        override fun areContentsTheSame(oldItem: EpisodeNavigation, newItem: EpisodeNavigation) = oldItem == newItem
    }
}
