package com.ofc.movies.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ofc.movies.data.api.MovieRepository
import com.ofc.movies.data.local.StorageManager
import com.ofc.movies.data.model.ContinueWatchingItem
import com.ofc.movies.data.model.HomeCategoryRow
import com.ofc.movies.data.model.MovieItem
import com.ofc.movies.data.model.formatUserFriendlyError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val heroMovies: List<MovieItem>,
        val continueWatching: List<ContinueWatchingItem>,
        val top10Movies: List<MovieItem>,
        val rows: List<HomeCategoryRow>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val storageManager = StorageManager.getInstance(application)
    private val repository = MovieRepository(storageManager = storageManager)

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadHomeContent()
    }

    fun refreshContinueWatching() {
        val current = _uiState.value
        if (current is HomeUiState.Success) {
            val cw = storageManager.getContinueWatching()
            _uiState.value = current.copy(continueWatching = cw)
        }
    }

    fun loadHomeContent(force: Boolean = false) {
        if (!force && _uiState.value is HomeUiState.Success) {
            refreshContinueWatching()
            return
        }

        viewModelScope.launch {
            if (_uiState.value !is HomeUiState.Success) {
                _uiState.value = HomeUiState.Loading
            }

            val result = repository.getHomeSections(force = force)
            result.onSuccess { sections ->
                val allMovies = sections.flatMap { it.items }.distinctBy { it.id }
                val heroMovies = allMovies.take(5)
                val top10 = allMovies.sortedByDescending { it.rating?.toDoubleOrNull() ?: 0.0 }.take(10)

                // Real Continue Watching from persistent storage
                val continueWatching = storageManager.getContinueWatching()

                _uiState.value = HomeUiState.Success(
                    heroMovies = heroMovies,
                    continueWatching = continueWatching,
                    top10Movies = if (top10.isNotEmpty()) top10 else heroMovies,
                    rows = sections
                )
            }.onFailure { err ->
                if (_uiState.value !is HomeUiState.Success) {
                    _uiState.value = HomeUiState.Error(formatUserFriendlyError(err, "Failed to load content"))
                }
            }

            _isRefreshing.value = false
        }
    }

    fun refresh() {
        _isRefreshing.value = true
        loadHomeContent(force = true)
    }
}
