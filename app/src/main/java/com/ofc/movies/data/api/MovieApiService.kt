package com.ofc.movies.data.api

import com.ofc.movies.data.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface MovieApiService {

    @GET("wefeed-mobile-bff/tab-operating")
    suspend fun getTabOperating(
        @Query("tabId") tabId: Int = 0,
        @Query("page") page: Int = 1,
        @Query("version") version: String = ""
    ): TabOperatingResponse

    @GET("wefeed-mobile-bff/subject-api/get")
    suspend fun getSubjectDetail(
        @Query("subjectId") subjectId: String
    ): SubjectDetailResponse

    @POST("wefeed-mobile-bff/subject-api/search")
    suspend fun search(
        @Body request: SearchRequestBody
    ): SearchResponse

    @GET("wefeed-mobile-bff/subject-api/season-info")
    suspend fun getSeasonInfo(
        @Query("subjectId") subjectId: String
    ): SeasonInfoResponse

    @POST("wefeed-mobile-bff/subject-api/play-related-rec")
    suspend fun getRecommendations(
        @Body request: RelatedRecRequestBody
    ): RelatedRecResponse

    @GET("wefeed-mobile-bff/subject-api/play-info")
    suspend fun getPlayInfo(
        @Query("subjectId") subjectId: String,
        @Query("se") se: Int = 0,
        @Query("ep") ep: Int = 0
    ): PlayInfoResponse

    @GET("wefeed-mobile-bff/subject-api/resource")
    suspend fun getResources(
        @Query("subjectId") subjectId: String,
        @Query("se") se: Int = 0,
        @Query("ep") ep: Int = 0,
        @Query("page") page: Int = 1
    ): ResourcesResponse
}
