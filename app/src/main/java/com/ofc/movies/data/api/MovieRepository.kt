package com.ofc.movies.data.api

import com.ofc.movies.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MovieRepository(
    private val api: MovieApiService = ApiClient.service
) {

    suspend fun getHomeSections(): Result<List<HomeCategoryRow>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTabOperating(tabId = 0, page = 1)
            val rawItems = response.data?.items ?: emptyList()

            val filteredSections = mutableListOf<HomeCategoryRow>()
            val bannedKeywords = listOf("short tv", "shorts", "18+", "adult", "erotic", "fight zone", "banner", "update")

            for (sec in rawItems) {
                val title = sec.title.trim()
                val titleLower = title.lowercase()

                if (bannedKeywords.any { titleLower.contains(it) }) continue

                val safeSubjects = sec.subjects.filter { it.isFamilySafe }
                if (safeSubjects.isNotEmpty()) {
                    filteredSections.add(
                        HomeCategoryRow(
                            title = title.ifEmpty { "Featured" },
                            type = sec.type,
                            items = safeSubjects
                        )
                    )
                }
            }

            Result.success(filteredSections)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieDetail(subjectId: String): Result<MovieDetailData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSubjectDetail(subjectId)
            val data = response.data ?: throw IllegalStateException("Empty subject detail")
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSeasonInfo(subjectId: String): Result<List<SeasonItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSeasonInfo(subjectId)
            val seasons = response.data?.seasons ?: emptyList()
            Result.success(seasons)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecommendations(subjectId: String): Result<List<MovieItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getRecommendations(RelatedRecRequestBody(subjectId))
            val items = response.data?.items?.filter { it.isFamilySafe } ?: emptyList()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchMovies(query: String, page: Int = 1): Result<List<MovieItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.search(
                SearchRequestBody(
                    keyword = query,
                    page = page,
                    perPage = 20,
                    subjectType = 0
                )
            )
            val items = response.data?.items?.filter { it.isFamilySafe } ?: emptyList()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlayableStreams(subjectId: String, se: Int = 0, ep: Int = 0): Result<List<PlayableStream>> =
        withContext(Dispatchers.IO) {
            try {
                val playInfoResp = api.getPlayInfo(subjectId = subjectId, se = se, ep = ep)
                val streams = playInfoResp.data?.streams ?: emptyList()
                val playable = mutableListOf<PlayableStream>()

                for (s in streams) {
                    val cookie = s.signCookie ?: ""
                    val resString = s.resolutions ?: "1080"
                    val codec = s.codecName ?: "h265"
                    val size = s.size
                    val duration = s.duration

                    if (cookie.contains("CloudFront-Policy=")) {
                        val baseDashUrl = MovieBoxSigner.extractBaseDashUrl(cookie)
                        if (baseDashUrl != null) {
                            val mpdUrl = "$baseDashUrl/index.mpd"
                            val resList = resString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val effectiveList = if (resList.isEmpty()) listOf("1080") else resList

                            for (rVal in effectiveList) {
                                val rInt = rVal.toIntOrNull() ?: 1080
                                playable.add(
                                    PlayableStream(
                                        title = "${rInt}P HD",
                                        resolution = rInt,
                                        codecName = codec,
                                        size = size,
                                        duration = duration,
                                        streamUrl = mpdUrl,
                                        isDash = true,
                                        signCookie = cookie,
                                        season = se,
                                        episode = ep
                                    )
                                )
                            }
                        }
                    } else {
                        val url = s.url ?: ""
                        if (url.isNotEmpty() && !url.contains("9a0461bc39da389663bf3dbb17091d3f")) {
                            val rInt = resString.split(",").firstOrNull()?.trim()?.toIntOrNull() ?: 720
                            playable.add(
                                PlayableStream(
                                    title = "${rInt}P",
                                    resolution = rInt,
                                    codecName = codec,
                                    size = size,
                                    duration = duration,
                                    streamUrl = url,
                                    isDash = false,
                                    season = se,
                                    episode = ep
                                )
                            )
                        }
                    }
                }

                // If playInfo returned nothing, check resources endpoint as fallback
                if (playable.isEmpty()) {
                    val resResp = api.getResources(subjectId = subjectId, se = se, ep = ep, page = 1)
                    val rawList = resResp.data?.list ?: emptyList()
                    for (r in rawList) {
                        val link = r.resourceLink ?: ""
                        if (link.isNotEmpty() && !link.contains("9a0461bc39da389663bf3dbb17091d3f") && !link.contains("/other/2026/09/01/")) {
                            playable.add(
                                PlayableStream(
                                    title = "${r.resolution}P",
                                    resolution = r.resolution,
                                    codecName = r.codecName ?: "h264",
                                    size = r.size,
                                    duration = 0L,
                                    streamUrl = link,
                                    isDash = false,
                                    season = se,
                                    episode = ep
                                )
                            )
                        }
                    }
                }

                Result.success(playable.sortedByDescending { it.resolution })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
