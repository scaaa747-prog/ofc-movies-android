package com.ofc.movies.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ofc.movies.data.api.MovieRepository
import com.ofc.movies.data.model.ContinueWatchingItem
import com.ofc.movies.data.model.HomeCategoryRow
import com.ofc.movies.data.model.MovieItem
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
    private val repository: MovieRepository = MovieRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadHomeContent()
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val result = repository.getHomeSections()
            result.onSuccess { sections ->
                val allMovies = sections.flatMap { it.items }.distinctBy { it.id }
                val heroMovies = allMovies.take(5)
                val top10 = allMovies.sortedByDescending { it.rating?.toDoubleOrNull() ?: 0.0 }.take(10)

                // Demo continue watching item
                val continueWatching = if (allMovies.isNotEmpty()) {
                    listOf(
                        ContinueWatchingItem(
                            id = allMovies.first().id,
                            title = allMovies.first().title,
                            coverUrl = allMovies.first().coverUrl,
                            progress = 0.65f,
                            durationMinutes = 112,
                            lastWatchedEpisode = null
                        )
                    )
                } else emptyList()

                _uiState.value = HomeUiState.Success(
                    heroMovies = heroMovies,
                    continueWatching = continueWatching,
                    top10Movies = if (top10.isNotEmpty()) top10 else heroMovies,
                    rows = sections
                )
            }.onFailure { err ->
                _uiState.value = HomeUiState.Error(err.localizedMessage ?: "Failed to load content")
            }

            _isRefreshing.value = false
        }
    }

    fun refresh() {
        _isRefreshing.value = true
        loadHomeContent()
    }
}
