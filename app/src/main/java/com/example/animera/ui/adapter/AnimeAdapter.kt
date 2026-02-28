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
import com.example.animera.databinding.ItemAnimeHorizontalBinding
import com.example.animera.databinding.ItemAnimeListBinding
import com.example.animera.databinding.ItemLoadingBinding

class AnimeAdapter(
    private val onAnimeClick: (Anime) -> Unit
) : ListAdapter<AnimeAdapter.Item, RecyclerView.ViewHolder>(DiffCallback()) {

    enum class DisplayMode { GRID, VERTICAL, HORIZONTAL }
    var displayMode: DisplayMode = DisplayMode.GRID

    private companion object {
        const val VIEW_TYPE_GRID = 0
        const val VIEW_TYPE_VERTICAL = 1
        const val VIEW_TYPE_HORIZONTAL = 2
        const val VIEW_TYPE_LOADING = 3
    }

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
            is Item.AnimeItem -> {
                when (displayMode) {
                    DisplayMode.GRID -> VIEW_TYPE_GRID
                    DisplayMode.VERTICAL -> VIEW_TYPE_VERTICAL
                    DisplayMode.HORIZONTAL -> VIEW_TYPE_HORIZONTAL
                }
            }
            is Item.LoadingItem -> VIEW_TYPE_LOADING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_GRID -> GridViewHolder(ItemAnimeCardBinding.inflate(inflater, parent, false))
            VIEW_TYPE_VERTICAL -> VerticalViewHolder(ItemAnimeListBinding.inflate(inflater, parent, false))
            VIEW_TYPE_HORIZONTAL -> HorizontalViewHolder(ItemAnimeHorizontalBinding.inflate(inflater, parent, false))
            else -> LoadingViewHolder(ItemLoadingBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (item is Item.AnimeItem) {
            when (holder) {
                is GridViewHolder -> holder.bind(item.anime)
                is VerticalViewHolder -> holder.bind(item.anime)
                is HorizontalViewHolder -> holder.bind(item.anime)
            }
        }
    }

    inner class GridViewHolder(private val binding: ItemAnimeCardBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        fun bind(anime: Anime) {
            binding.tvTitle.text = anime.title
            binding.tvType.text = anime.type
            binding.tvYear.text = anime.year
            binding.tvStatus.text = anime.status
            binding.tvRating.text = anime.rating
            
            val statusBg = if (anime.statusClass == "airing") R.drawable.bg_status_airing else R.drawable.bg_status_finished
            binding.tvStatus.setBackgroundResource(statusBg)
            
            Glide.with(binding.ivPoster).load(anime.imageUrl).placeholder(R.drawable.placeholder_anime)
                .transition(DrawableTransitionOptions.withCrossFade()).centerCrop().into(binding.ivPoster)
            binding.root.setOnClickListener { onAnimeClick(anime) }
        }
    }

    inner class VerticalViewHolder(private val binding: ItemAnimeListBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        fun bind(anime: Anime) {
            binding.tvTitle.text = anime.title
            binding.tvType.text = anime.type
            binding.tvYear.text = anime.year
            val statusBg = if (anime.statusClass == "airing") R.drawable.bg_status_airing else R.drawable.bg_status_finished
            binding.tvStatus.setBackgroundResource(statusBg)
            Glide.with(binding.ivPoster).load(anime.imageUrl).placeholder(R.drawable.placeholder_anime)
                .transition(DrawableTransitionOptions.withCrossFade()).centerCrop().into(binding.ivPoster)
            binding.root.setOnClickListener { onAnimeClick(anime) }
        }
    }

    inner class HorizontalViewHolder(private val binding: ItemAnimeHorizontalBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        fun bind(anime: Anime) {
            binding.tvTitle.text = anime.title
            binding.tvType.text = anime.type
            Glide.with(binding.ivPoster).load(anime.imageUrl).placeholder(R.drawable.placeholder_anime)
                .transition(DrawableTransitionOptions.withCrossFade()).centerCrop().into(binding.ivPoster)
            binding.root.setOnClickListener { onAnimeClick(anime) }
        }
    }


    class LoadingViewHolder(binding: ItemLoadingBinding) : RecyclerView.ViewHolder(binding.root)

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = 
            oldItem is Item.AnimeItem && newItem is Item.AnimeItem && oldItem.anime.detailUrl == newItem.anime.detailUrl ||
            oldItem is Item.LoadingItem && newItem is Item.LoadingItem
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }
}
