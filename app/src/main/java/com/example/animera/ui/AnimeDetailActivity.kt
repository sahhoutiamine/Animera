package com.example.animera.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.animera.R
import com.example.animera.databinding.ActivityAnimeDetailBinding
import com.example.animera.ui.adapter.EpisodeAdapter
import com.example.animera.ui.adapter.RelatedAnimeAdapter
import com.example.animera.ui.viewmodel.AnimeDetailViewModel
import com.example.animera.ui.viewmodel.DetailUiState
import com.google.android.material.chip.Chip

class AnimeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnimeDetailBinding
    private val viewModel: AnimeDetailViewModel by viewModels()
    private lateinit var episodeAdapter: EpisodeAdapter
    private lateinit var relatedAdapter: RelatedAnimeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnimeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val detailUrl = intent.getStringExtra("DETAIL_URL") ?: run {
            finish()
            return
        }

        setupToolbar()
        setupRecyclerViews()
        observeViewModel()

        viewModel.loadDetail(detailUrl)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        episodeAdapter = EpisodeAdapter { episode ->
            val intent = Intent(this, EpisodePlayerActivity::class.java).apply {
                putExtra("EPISODE_URL", episode.watchUrl)
            }
            startActivity(intent)
        }
        binding.rvEpisodes.apply {
            layoutManager = LinearLayoutManager(this@AnimeDetailActivity)
            adapter = episodeAdapter
            isNestedScrollingEnabled = false
        }

        relatedAdapter = RelatedAnimeAdapter { related ->
            // Reload with new anime
            viewModel.loadDetail(related.detailUrl)
            binding.root.scrollTo(0, 0)
        }
        binding.rvRelated.apply {
            layoutManager = LinearLayoutManager(this@AnimeDetailActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = relatedAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is DetailUiState.Loading -> {
                    binding.loadingLayout.visibility = View.VISIBLE
                }
                is DetailUiState.Success -> {
                    binding.loadingLayout.visibility = View.GONE
                    val detail = state.detail
                    
                    binding.tvDetailTitle.text = detail.title
                    binding.tvDetailAltTitle.text = detail.altTitle
                    binding.tvSynopsis.text = detail.synopsis
                    binding.tvDetailType.text = detail.type
                    binding.tvDetailStatus.text = detail.status
                    binding.tvDetailStudio.text = detail.studio

                    // Load banner/cover
                    Glide.with(this)
                        .load(detail.coverImageUrl)
                        .placeholder(R.drawable.placeholder_anime)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(binding.ivBanner)

                    // Genres
                    binding.chipGroupGenres.removeAllViews()
                    detail.genres.forEach { genre ->
                        val chip = Chip(this).apply {
                            text = genre
                            setChipBackgroundColorResource(R.color.accent_purple)
                            setTextColor(getColor(R.color.white))
                        }
                        binding.chipGroupGenres.addView(chip)
                    }

                    episodeAdapter.submitList(detail.episodes)
                    relatedAdapter.submitList(detail.relatedAnime)
                }
                is DetailUiState.Error -> {
                    binding.loadingLayout.visibility = View.GONE
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
