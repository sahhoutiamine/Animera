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
                    binding.tvDetailSeason.text = detail.season
                    binding.tvDetailSource.text = detail.source
                    binding.tvDetailYear.text = detail.year
                    binding.tvEpCount.text = "${detail.episodes.size} حلقة"


                    // Load banner image with smart fallback and atmospheric effect
                    val isGeneric = detail.bannerUrl.contains("banner.jpg")
                    val hasActualBanner = detail.bannerUrl.isNotEmpty() && !isGeneric
                    
                    binding.ivBanner.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    
                    if (hasActualBanner) {
                        // Actual Wide Banner: Clear and bright
                        binding.ivBanner.clearColorFilter()
                        Glide.with(this)
                            .load(detail.bannerUrl)
                            .placeholder(R.drawable.placeholder_anime)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(binding.ivBanner)
                    } else {
                        // Poster as Banner: Apply Dark Atmosphere Effect
                        binding.ivBanner.setColorFilter(android.graphics.Color.parseColor("#BB000000"), android.graphics.PorterDuff.Mode.SRC_ATOP)
                        Glide.with(this)
                            .load(detail.coverImageUrl)
                            .placeholder(R.drawable.placeholder_anime)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(binding.ivBanner)
                    }
                        
                    // Load clear floating poster (The main reference)
                    Glide.with(this)
                        .load(detail.coverImageUrl)
                        .placeholder(R.drawable.placeholder_anime)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(binding.ivPoster)

                    // Genres
                    binding.chipGroupGenres.removeAllViews()
                    detail.genres.forEach { genre ->
                        val chip = Chip(this).apply {
                            text = genre
                            setChipBackgroundColorResource(R.color.accent_purple)
                            setTextColor(getColor(R.color.white))
                            chipStrokeWidth = 0f
                            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                                .setAllCornerSizes(32f)
                                .build()
                            textAlignment = View.TEXT_ALIGNMENT_CENTER
                        }
                        binding.chipGroupGenres.addView(chip)
                    }


                    episodeAdapter.submitList(detail.episodes)
                    relatedAdapter.submitList(detail.relatedAnime)

                    // Action Buttons
                    binding.btnTrailer.visibility = if (detail.trailerUrl.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.btnMal.visibility = if (detail.malUrl.isNotEmpty()) View.VISIBLE else View.GONE

                    binding.btnTrailer.setOnClickListener {
                        if (detail.trailerUrl.isNotEmpty()) {
                            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(detail.trailerUrl)))
                        }
                    }

                    binding.btnMal.setOnClickListener {
                        if (detail.malUrl.isNotEmpty()) {
                            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(detail.malUrl)))
                        }
                    }
                }
                is DetailUiState.Error -> {
                    binding.loadingLayout.visibility = View.GONE
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
