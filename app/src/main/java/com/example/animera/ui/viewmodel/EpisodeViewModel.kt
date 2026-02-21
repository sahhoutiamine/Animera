package com.example.animera.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.animera.data.model.EpisodePlayerInfo
import com.example.animera.data.repository.EpisodeRepository
import kotlinx.coroutines.launch

sealed class EpisodeUiState {
    object Loading : EpisodeUiState()
    data class Success(val info: EpisodePlayerInfo) : EpisodeUiState()
    data class Error(val message: String) : EpisodeUiState()
}

class EpisodeViewModel(private val repository: EpisodeRepository = EpisodeRepository()) : ViewModel() {

    private val _uiState = MutableLiveData<EpisodeUiState>()
    val uiState: LiveData<EpisodeUiState> = _uiState

    fun loadEpisode(url: String) {
        _uiState.value = EpisodeUiState.Loading
        viewModelScope.launch {
            repository.getEpisodeInfo(url)
                .onSuccess { info ->
                    _uiState.value = EpisodeUiState.Success(info)
                }
                .onFailure { error ->
                    _uiState.value = EpisodeUiState.Error(error.message ?: "Unknown error")
                }
        }
    }
}
