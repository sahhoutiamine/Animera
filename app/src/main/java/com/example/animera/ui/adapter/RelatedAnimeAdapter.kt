package com.example.animera.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.animera.R
import com.example.animera.data.model.RelatedAnime
import com.example.animera.databinding.ItemRelatedAnimeBinding

class RelatedAnimeAdapter(
    private val onAnimeClick: (RelatedAnime) -> Unit
) : ListAdapter<RelatedAnime, RelatedAnimeAdapter.RelatedViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelatedViewHolder {
        val binding = ItemRelatedAnimeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RelatedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RelatedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RelatedViewHolder(
        private val binding: ItemRelatedAnimeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(anime: RelatedAnime) {
            binding.tvTitle.text = anime.title
            binding.tvRating.text = anime.rating

            Glide.with(binding.ivPoster.context)
                .load(anime.imageUrl)
                .placeholder(R.drawable.placeholder_anime)
                .error(R.drawable.placeholder_anime)
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .centerCrop()
                .into(binding.ivPoster)

            binding.root.setOnClickListener { onAnimeClick(anime) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RelatedAnime>() {
        override fun areItemsTheSame(oldItem: RelatedAnime, newItem: RelatedAnime) =
            oldItem.detailUrl == newItem.detailUrl
        override fun areContentsTheSame(oldItem: RelatedAnime, newItem: RelatedAnime) =
            oldItem == newItem
    }
}
