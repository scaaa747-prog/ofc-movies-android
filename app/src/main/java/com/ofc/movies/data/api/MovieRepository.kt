package com.ofc.movies.data.api

import com.ofc.movies.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class MovieRepository(
    private val apiService: MovieApiService = ApiClient.service
) {

    fun getHomeFeed(tabId: Int = 0): Flow<Result<HomeFeedResponse>> = flow {
        try {
            val response = apiService.getHomeFeed(tabId)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getTrending(): Flow<Result<List<MovieItem>>> = flow {
        try {
            val response = apiService.getTrending()
            val items = response.data?.items ?: emptyList()
            emit(Result.success(items))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun searchMovies(query: String, page: Int = 1, type: Int = 0): Flow<Result<List<MovieItem>>> = flow {
        try {
            val response = apiService.search(query, page, type)
            emit(Result.success(response.items))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getMovieDetail(subjectId: String): Flow<Result<MovieDetailResponse>> = flow {
        try {
            val response = apiService.getMovieDetail(subjectId)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getStreamResources(subjectId: String, season: Int = 0, episode: Int = 0): Flow<Result<List<StreamResource>>> = flow {
        try {
            val response = apiService.getResources(subjectId, season, episode)
            val list = response.data?.list ?: emptyList()
            emit(Result.success(list))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // Demo/Cached Continue Watching list
    fun getContinueWatchingList(): List<ContinueWatchingItem> {
        return listOf(
            ContinueWatchingItem(
                id = "8282836313385190960",
                title = "JoJo's Bizarre Adventure [Hindi]",
                coverUrl = "/img/v/BRYrAwdISk4dPRVWMV0AAxo6QF9dW0MBMB5bGwgACjpZAG9BV0NEbh0CBRkJU24QEBNcAl5sRwc7EgRZF2dQCVZTCVA7QkcUUlNfalhYLxQ.webp?w=180&q=45",
                progress = 0.65f,
                durationMinutes = 24,
                lastWatchedEpisode = "S1 E5"
            ),
            ContinueWatchingItem(
                id = "5349301616099452064",
                title = "Gandhari [Hindi]",
                coverUrl = "/img/v/BRYrAwdISk4dPRVWMV0AAxo6QF9dW0MBMB5bGwgACjpZAG9BV0NEZh0AABkOBjkWEEdcAlQ-FAVmEFlfTGdTVgIBWAM5FUQXUAcLO1hYLxQ.webp?w=180&q=45",
                progress = 0.35f,
                durationMinutes = 112
            ),
            ContinueWatchingItem(
                id = "7826893701690839800",
                title = "The Runner [Hindi]",
                coverUrl = "/img/v/BRYrAwdISk4dPRVWMV0AAxo6QF9dW0MBMB5bGwgACjpZAG9BV0NEZh0AABkOBjkWEEdcAlQ-FAVmEFlfTGdTVgIBWAM5FUQXUAcLO1hYLxQ.webp?w=180&q=45",
                progress = 0.82f,
                durationMinutes = 98
            )
        )
    }
}
