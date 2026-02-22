package com.example.animera.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animera.data.model.Anime
import com.example.animera.data.repository.AnimeRepository
import kotlinx.coroutines.launch

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    object LoadingMore : UiState()
    data class Success(val animeList: List<Anime>, val hasNextPage: Boolean) : UiState()
    data class Error(val message: String) : UiState()
}

class AnimeListViewModel : ViewModel() {

    private val repository = AnimeRepository()

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    private val _animeList = MutableLiveData<List<Anime>>(emptyList())
    val animeList: LiveData<List<Anime>> = _animeList

    private val _featuredAnime = MutableLiveData<Anime?>()
    val featuredAnime: LiveData<Anime?> = _featuredAnime

    private var currentPage = 0
    private var hasNextPage = true
    private var isLoading = false
    private var currentSearchQuery: String? = null

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        if (isLoading) return
        currentPage = 0
        hasNextPage = true
        currentSearchQuery = null
        _animeList.value = emptyList()
        
        loadNextPage()
    }

    fun searchAnime(query: String) {
        if (isLoading) return
        currentPage = 0
        hasNextPage = true
        currentSearchQuery = query.ifEmpty { null }
        _animeList.value = emptyList()
        
        loadNextPage()
    }

    fun loadNextPage() {
        if (isLoading || !hasNextPage) return
        isLoading = true

        val nextPage = currentPage + 1
        val isFirstLoad = currentPage == 0

        _uiState.value = if (isFirstLoad) UiState.Loading else UiState.LoadingMore

        viewModelScope.launch {
            val result = if (currentSearchQuery != null) {
                repository.searchAnime(currentSearchQuery!!, nextPage)
            } else {
                repository.getAnimePage(nextPage)
            }

            result.onSuccess { animePage ->
                currentPage = animePage.currentPage
                hasNextPage = animePage.hasNextPage
                
                val currentList = _animeList.value ?: emptyList()
                val fetchedList = animePage.animeList
                
                // If it's the very first load and no search, pick a random featured anime
                if (isFirstLoad && currentSearchQuery == null && fetchedList.isNotEmpty()) {
                    _featuredAnime.value = fetchedList.shuffled().first()
                }

                val newList = currentList + fetchedList
                _animeList.value = newList
                _uiState.value = UiState.Success(newList, hasNextPage)
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.message ?: "Unknown error occurred")
            }
            isLoading = false
        }
    }

    fun canLoadMore(): Boolean = hasNextPage && !isLoading
}
