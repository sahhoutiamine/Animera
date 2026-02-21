package com.example.animera.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.animera.data.model.EpisodePlayerInfo
import com.example.animera.data.model.VideoServer
import com.example.animera.databinding.ActivityEpisodePlayerBinding
import com.example.animera.ui.adapter.EpisodeNavAdapter
import com.example.animera.ui.adapter.ServerAdapter
import com.example.animera.ui.viewmodel.EpisodeUiState
import com.example.animera.ui.viewmodel.EpisodeViewModel

class EpisodePlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEpisodePlayerBinding
    private val viewModel: EpisodeViewModel by viewModels()
    private lateinit var serverAdapter: ServerAdapter
    private lateinit var episodeNavAdapter: EpisodeNavAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEpisodePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val episodeUrl = intent.getStringExtra("EPISODE_URL") ?: run {
            finish()
            return
        }

        setupWebView()
        setupRecyclerViews()
        setupToolbar()
        observeViewModel()

        viewModel.loadEpisode(episodeUrl)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.mediaPlaybackRequiresUserGesture = false
            
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    // Prevent WebView from opening external browsers for the initial load
                    return false
                }
            }
            webChromeClient = WebChromeClient()
        }
    }

    private fun setupRecyclerViews() {
        serverAdapter = ServerAdapter { server ->
            loadServer(server)
        }
        binding.rvServers.apply {
            layoutManager = LinearLayoutManager(this@EpisodePlayerActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = serverAdapter
        }

        episodeNavAdapter = EpisodeNavAdapter { ep ->
            val intent = Intent(this, EpisodePlayerActivity::class.java).apply {
                putExtra("EPISODE_URL", ep.url)
            }
            startActivity(intent)
            finish() // Important to finish current to avoid deep stack
        }
        binding.rvEpisodes.apply {
            layoutManager = LinearLayoutManager(this@EpisodePlayerActivity)
            adapter = episodeNavAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is EpisodeUiState.Loading -> {
                    binding.loadingLayout.visibility = View.VISIBLE
                }
                is EpisodeUiState.Success -> {
                    binding.loadingLayout.visibility = View.GONE
                    populateUi(state.info)
                }
                is EpisodeUiState.Error -> {
                    binding.loadingLayout.visibility = View.GONE
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun populateUi(info: EpisodePlayerInfo) {
        binding.tvEpisodeTitle.text = info.title
        binding.toolbar.title = info.title

        serverAdapter.submitList(info.servers)
        episodeNavAdapter.submitList(info.episodes)

        // Load first server by default
        if (info.servers.isNotEmpty()) {
            loadServer(info.servers[0])
        }

        // Navigation buttons
        binding.btnNext.apply {
            isEnabled = info.nextEpisodeUrl != null
            setOnClickListener {
                info.nextEpisodeUrl?.let { url ->
                    val intent = Intent(this@EpisodePlayerActivity, EpisodePlayerActivity::class.java).apply {
                        putExtra("EPISODE_URL", url)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }

        binding.btnPrev.apply {
            isEnabled = info.prevEpisodeUrl != null
            setOnClickListener {
                info.prevEpisodeUrl?.let { url ->
                    val intent = Intent(this@EpisodePlayerActivity, EpisodePlayerActivity::class.java).apply {
                        putExtra("EPISODE_URL", url)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun loadServer(server: VideoServer) {
        binding.webView.loadUrl(server.url)
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
