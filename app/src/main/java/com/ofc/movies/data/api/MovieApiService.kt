package com.ofc.movies.data.api

import com.ofc.movies.data.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieApiService {

    @GET("api/home")
    suspend fun getHomeFeed(
        @Query("tabId") tabId: Int = 0
    ): HomeFeedResponse

    @GET("api/trending")
    suspend fun getTrending(): TrendingResponse

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("type") type: Int = 0
    ): SearchResponse

    @GET("api/suggest")
    suspend fun suggest(
        @Query("q") query: String
    ): SuggestResponse

    @GET("api/detail/{id}")
    suspend fun getMovieDetail(
        @Path("id") subjectId: String
    ): MovieDetailResponse

    @GET("api/resources")
    suspend fun getResources(
        @Query("subjectId") subjectId: String,
        @Query("se") season: Int = 0,
        @Query("ep") episode: Int = 0
    ): ResourcesResponse

    @GET("api/subtitles")
    suspend fun getSubtitles(
        @Query("subjectId") subjectId: String,
        @Query("se") season: Int = 0,
        @Query("ep") episode: Int = 0
    ): SubtitlesResponse

    @GET("api/related/{id}")
    suspend fun getRelated(
        @Path("id") subjectId: String
    ): List<MovieItem>
}
