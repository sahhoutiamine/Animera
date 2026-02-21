package com.example.animera.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animera.data.model.AnimeDetail
import com.example.animera.data.repository.AnimeDetailRepository
import kotlinx.coroutines.launch

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val detail: AnimeDetail) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

class AnimeDetailViewModel : ViewModel() {

    private val repository = AnimeDetailRepository()

    private val _uiState = MutableLiveData<DetailUiState>(DetailUiState.Loading)
    val uiState: LiveData<DetailUiState> = _uiState

    fun loadDetail(url: String) {
        _uiState.value = DetailUiState.Loading
        viewModelScope.launch {
            val result = repository.getAnimeDetail(url)
            result.onSuccess { detail ->
                _uiState.value = DetailUiState.Success(detail)
            }.onFailure { error ->
                _uiState.value = DetailUiState.Error(error.message ?: "Unknown error")
            }
        }
    }
}
