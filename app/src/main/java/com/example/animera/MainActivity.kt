package com.example.animera

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animera.databinding.ActivityMainBinding
import com.example.animera.ui.adapter.AnimeAdapter
import com.example.animera.ui.viewmodel.AnimeListViewModel
import com.example.animera.ui.viewmodel.UiState

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AnimeListViewModel by viewModels()
    private lateinit var animeAdapter: AnimeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupRecyclerView() {
        animeAdapter = AnimeAdapter { anime ->
            // Future: open detail screen
            Toast.makeText(this, anime.title, Toast.LENGTH_SHORT).show()
        }

        val spanCount = 2
        val gridLayoutManager = GridLayoutManager(this, spanCount)

        // Loading item spans full width
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (animeAdapter.getItemViewType(position) == 1) spanCount else 1
            }
        }

        binding.recyclerView.apply {
            layoutManager = gridLayoutManager
            adapter = animeAdapter
            setHasFixedSize(false)

            // Infinite scroll
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) { // Scrolling down
                        val visibleItemCount = gridLayoutManager.childCount
                        val totalItemCount = gridLayoutManager.itemCount
                        val pastVisibleItems = gridLayoutManager.findFirstVisibleItemPosition()

                        if (viewModel.canLoadMore()) {
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 4) {
                                viewModel.loadNextPage()
                            }
                        }
                    }
                }
            })
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.apply {
            setColorSchemeResources(R.color.accent_purple, R.color.accent_blue)
            setOnRefreshListener {
                viewModel.loadFirstPage()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Idle -> {
                    // Do nothing
                }
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.errorLayout.visibility = View.GONE
                    binding.recyclerView.visibility = View.GONE
                }
                is UiState.LoadingMore -> {
                    binding.progressBar.visibility = View.GONE
                    binding.errorLayout.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    // Show loading item at bottom
                    val list = viewModel.animeList.value ?: emptyList()
                    animeAdapter.submitAnimeList(list, showLoading = true)
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.errorLayout.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    animeAdapter.submitAnimeList(state.animeList, showLoading = false)
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    val currentList = viewModel.animeList.value ?: emptyList()
                    if (currentList.isEmpty()) {
                        binding.errorLayout.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                        binding.tvErrorMessage.text = state.message
                    } else {
                        binding.errorLayout.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                        animeAdapter.submitAnimeList(currentList, showLoading = false)
                        Toast.makeText(this, "Failed to load more: ${state.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadFirstPage()
        }
    }
}