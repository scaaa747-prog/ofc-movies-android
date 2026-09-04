package com.ofc.movies.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ofc.movies.data.api.MovieRepository
import com.ofc.movies.data.model.ContinueWatchingItem
import com.ofc.movies.data.model.HomeCategoryRow
import com.ofc.movies.data.model.MovieItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val heroMovies: List<MovieItem>,
        val continueWatching: List<ContinueWatchingItem>,
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

            val continueWatching = repository.getContinueWatchingList()

            // Fetch Home Feed & Trending concurrently
            combine(
                repository.getHomeFeed(0),
                repository.getTrending()
            ) { homeResult, trendingResult ->
                val homeResponse = homeResult.getOrNull()
                val trendingItems = trendingResult.getOrNull() ?: emptyList()

                val rows = mutableListOf<HomeCategoryRow>()

                // 1. Trending Now row
                if (trendingItems.isNotEmpty()) {
                    rows.add(
                        HomeCategoryRow(
                            title = "🔥 Trending Now",
                            items = trendingItems
                        )
                    )
                }

                // 2. Add backend curated rows from Home Feed
                homeResponse?.items?.forEach { category ->
                    if (category.items.isNotEmpty()) {
                        rows.add(category)
                    }
                }

                // Featured Hero items (first 5 from Trending or first category)
                val heroItems = if (trendingItems.isNotEmpty()) {
                    trendingItems.take(5)
                } else {
                    homeResponse?.items?.flatMap { it.items }?.take(5) ?: emptyList()
                }

                if (rows.isNotEmpty() || heroItems.isNotEmpty()) {
                    HomeUiState.Success(
                        heroMovies = heroItems,
                        continueWatching = continueWatching,
                        rows = rows
                    )
                } else {
                    HomeUiState.Error("No content available. Please verify network connection.")
                }
            }.catch { e ->
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Failed to load content")
            }.collect { state ->
                _uiState.value = state
                _isRefreshing.value = false
            }
        }
    }

    fun refresh() {
        _isRefreshing.value = true
        loadHomeContent()
    }
}
