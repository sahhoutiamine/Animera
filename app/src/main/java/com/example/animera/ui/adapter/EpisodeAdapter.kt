package com.example.animera.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.animera.R
import com.example.animera.data.model.Episode
import com.example.animera.databinding.ItemEpisodeBinding

class EpisodeAdapter(
    private val onEpisodeClick: (Episode) -> Unit
) : ListAdapter<Episode, EpisodeAdapter.EpisodeViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding = ItemEpisodeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EpisodeViewHolder(
        private val binding: ItemEpisodeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(episode: Episode) {
            binding.tvEpisodeTitle.text = episode.title
            binding.tvEpisodeNumber.text = "EP ${episode.number}"

            Glide.with(binding.ivEpisodeThumbnail.context)
                .load(episode.thumbnailUrl)
                .placeholder(R.drawable.placeholder_anime)
                .error(R.drawable.placeholder_anime)
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .centerCrop()
                .into(binding.ivEpisodeThumbnail)

            binding.root.setOnClickListener { onEpisodeClick(episode) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Episode>() {
        override fun areItemsTheSame(oldItem: Episode, newItem: Episode) =
            oldItem.watchUrl == newItem.watchUrl
        override fun areContentsTheSame(oldItem: Episode, newItem: Episode) =
            oldItem == newItem
    }
}
