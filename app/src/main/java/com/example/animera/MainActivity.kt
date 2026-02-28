package com.example.animera

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animera.data.model.Anime
import com.example.animera.databinding.ActivityMainBinding
import com.example.animera.ui.AnimeDetailActivity
import com.example.animera.ui.adapter.AnimeAdapter
import com.example.animera.ui.viewmodel.AnimeListViewModel
import com.example.animera.ui.viewmodel.UiState

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AnimeListViewModel by viewModels()
    
    private lateinit var mainAdapter: AnimeAdapter
    private lateinit var horizontalAdapter: AnimeAdapter
    
    // UI Mode state
    private var isExploreMode = false
    private var isGridView = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Toggle Sidebar
        binding.btnProfile.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        // Navigation Menu (Click listeners for sidebar options)
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_favorites -> showToast("Favorites (Coming Soon)")
                R.id.nav_current_watch -> showToast("Current Watch (Coming Soon)")
                R.id.nav_settings -> showToast("Settings (Coming Soon)")
                R.id.nav_anime_list -> switchToExploreMode()
            }
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            true
        }

        // Adapters
        mainAdapter = AnimeAdapter { anime ->
            openDetail(anime)
        }
        horizontalAdapter = AnimeAdapter { anime ->
            openDetail(anime)
        }
        horizontalAdapter.displayMode = AnimeAdapter.DisplayMode.HORIZONTAL

        // RecyclerViews
        binding.rvHorizontal.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvHorizontal.adapter = horizontalAdapter

        setupMainRecyclerView()

        // Search
        binding.cvSearch.setOnClickListener {
            binding.etSearch.requestFocus()
            // Show keyboard manually if needed
        }

        binding.etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }

        // See All
        binding.btnSeeAll.setOnClickListener {
            switchToExploreMode()
        }

        // View Toggle
        binding.btnToggleView.setOnClickListener {
            isGridView = !isGridView
            updateViewToggleIcon()
            setupMainRecyclerView()
            // Re-submit list to refresh view holders
            val currentList = (viewModel.uiState.value as? UiState.Success)?.animeList ?: emptyList()
            mainAdapter.submitAnimeList(currentList, viewModel.canLoadMore())
        }

        // Pagination for main list (handling via NestedScrollView)
        binding.nestedScrollView.setOnScrollChangeListener { v, _, scrollY, _, _ ->
            if (viewModel.canLoadMore()) {
                val childHeight = binding.nestedScrollView.getChildAt(0).measuredHeight
                val scrollViewHeight = binding.nestedScrollView.measuredHeight
                
                // If we are near the bottom (1000px threshold)
                if (scrollY + scrollViewHeight >= childHeight - 1000) {
                    viewModel.loadNextPage()
                }
            }
        }

        // Swipe Refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (isExploreMode) {
                val query = binding.etSearch.text.toString()
                if (query.isNotEmpty()) viewModel.searchAnime(query) else viewModel.loadHomeData()
            } else {
                viewModel.loadHomeData()
            }
        }
    }

    private fun setupMainRecyclerView() {
        mainAdapter.displayMode = if (isGridView) AnimeAdapter.DisplayMode.GRID else AnimeAdapter.DisplayMode.VERTICAL
        binding.recyclerView.layoutManager = if (isGridView) {
            GridLayoutManager(this, 2).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (mainAdapter.getItemViewType(position) == 3) 2 else 1 // Loading spinner (type 3) takes full width
                    }
                }
            }
        } else {
            LinearLayoutManager(this)
        }
        binding.recyclerView.adapter = mainAdapter
    }

    private fun updateViewToggleIcon() {
        binding.btnToggleView.setImageResource(if (isGridView) R.drawable.ic_list else R.drawable.ic_grid)
    }

    private fun switchToExploreMode() {
        if (!isExploreMode) {
            isExploreMode = true
            binding.layoutHome.visibility = View.GONE
            binding.layoutExplore.visibility = View.VISIBLE
            
            // Sync Current Data to main list
            val currentList = (viewModel.uiState.value as? UiState.Success)?.animeList ?: emptyList()
            mainAdapter.submitAnimeList(currentList, viewModel.canLoadMore())
            
            // Re-setup layout manager to ensure grid is active
            setupMainRecyclerView()
        }
    }

    private fun switchToHomeMode() {
        if (isExploreMode) {
            isExploreMode = false
            binding.layoutHome.visibility = View.VISIBLE
            binding.layoutExplore.visibility = View.GONE
            binding.etSearch.text = null
            
            // Clear search focus
            binding.etSearch.clearFocus()
        }
    }

    private fun observeViewModel() {
        viewModel.featuredAnime.observe(this) { anime ->
            anime?.let { populateFeatured(it) }
        }

        viewModel.uiState.observe(this) { state ->
            binding.swipeRefreshLayout.isRefreshing = false
            
            when (state) {
                is UiState.Loading -> {
                    if (!isExploreMode) {
                        binding.shimmerView.visibility = View.VISIBLE
                        binding.shimmerView.startShimmer()
                    }
                }

                is UiState.Success -> {
                    binding.shimmerView.stopShimmer()
                    binding.shimmerView.visibility = View.GONE
                    
                    // Always update horizontal adapter with first page items when not searching
                    horizontalAdapter.submitAnimeList(state.animeList.take(20), false)
                    
                    // Always try to submit to main adapter as well so it's ready when switching
                    mainAdapter.submitAnimeList(state.animeList, state.hasNextPage)
                }
                is UiState.Error -> {
                    binding.shimmerView.stopShimmer()
                    binding.shimmerView.visibility = View.GONE
                    showToast(state.message)
                }

                else -> {}
            }
        }
    }

    private fun populateFeatured(anime: Anime) {
        binding.tvFeaturedTitle.text = anime.title
        binding.tvFeaturedStatus.text = anime.status
        
        Glide.with(this)
            .load(anime.imageUrl)
            .placeholder(R.drawable.placeholder_anime)
            .into(binding.ivFeaturedImage)
            
        binding.cvFeatured.setOnClickListener { openDetail(anime) }
    }

    private fun performSearch() {
        val query = binding.etSearch.text.toString().trim()
        if (query.isNotEmpty()) {
            switchToExploreMode()
            viewModel.searchAnime(query)
            // Hide keyboard
            binding.etSearch.clearFocus()
        } else {
            switchToHomeMode()
            viewModel.loadHomeData()
        }
    }

    private fun openDetail(anime: Anime) {
        val intent = Intent(this, AnimeDetailActivity::class.java).apply {
            putExtra("DETAIL_URL", anime.detailUrl)
        }
        startActivity(intent)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else if (isExploreMode) {
            switchToHomeMode()
            // Reset to latest data
            viewModel.loadHomeData()
        } else {
            super.onBackPressed()
        }
    }
}