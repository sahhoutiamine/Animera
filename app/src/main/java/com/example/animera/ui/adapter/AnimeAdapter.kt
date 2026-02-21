package com.example.animera.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.animera.R
import com.example.animera.data.model.Anime
import com.example.animera.databinding.ItemAnimeCardBinding
import com.example.animera.databinding.ItemLoadingBinding

private const val VIEW_TYPE_ANIME = 0
private const val VIEW_TYPE_LOADING = 1

class AnimeAdapter(
    private val onAnimeClick: (Anime) -> Unit
) : ListAdapter<AnimeAdapter.Item, RecyclerView.ViewHolder>(DiffCallback()) {

    sealed class Item {
        data class AnimeItem(val anime: Anime) : Item()
        object LoadingItem : Item()
    }


    fun submitAnimeList(animes: List<Anime>, showLoading: Boolean) {
        val newList = animes.map { Item.AnimeItem(it) } +
                if (showLoading) listOf(Item.LoadingItem) else emptyList()
        submitList(newList)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Item.AnimeItem -> VIEW_TYPE_ANIME
            is Item.LoadingItem -> VIEW_TYPE_LOADING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ANIME -> {
                val binding = ItemAnimeCardBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                AnimeViewHolder(binding)
            }
            else -> {
                val binding = ItemLoadingBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                LoadingViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Item.AnimeItem -> (holder as AnimeViewHolder).bind(item.anime)
            is Item.LoadingItem -> { /* just show spinner */ }
        }
    }

    inner class AnimeViewHolder(
        private val binding: ItemAnimeCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(anime: Anime) {
            binding.tvTitle.text = anime.title
            binding.tvType.text = anime.type
            binding.tvYear.text = anime.year
            binding.tvStatus.text = anime.status

            // Status styling
            when (anime.statusClass) {
                "airing" -> {
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_airing)
                }
                "finished" -> {
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_finished)
                }
                else -> {
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_airing)
                }
            }

            // Load image
            Glide.with(binding.ivPoster.context)
                .load(anime.imageUrl)
                .placeholder(R.drawable.placeholder_anime)
                .error(R.drawable.placeholder_anime)
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .centerCrop()
                .into(binding.ivPoster)

            binding.root.setOnClickListener { onAnimeClick(anime) }
        }
    }

    class LoadingViewHolder(
        binding: ItemLoadingBinding
    ) : RecyclerView.ViewHolder(binding.root)

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return when {
                oldItem is Item.AnimeItem && newItem is Item.AnimeItem ->
                    oldItem.anime.detailUrl == newItem.anime.detailUrl
                oldItem is Item.LoadingItem && newItem is Item.LoadingItem -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}
