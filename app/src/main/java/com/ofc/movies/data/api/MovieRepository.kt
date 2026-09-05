package com.ofc.movies.data.api

import com.ofc.movies.data.local.StorageManager
import com.ofc.movies.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MovieRepository(
    private val api: MovieApiService = ApiClient.service,
    private val storageManager: StorageManager? = null
) {

    private fun isAllowed(item: MovieItem): Boolean {
        if (item.isExplicitAdult) return false
        if (storageManager?.isFamilyModeEnabled() == true) {
            return item.isFamilySafe
        }
        return true
    }

    suspend fun getHomeSections(): Result<List<HomeCategoryRow>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getTabOperating(tabId = 0, page = 1)
            val rawItems = response.data?.items ?: emptyList()

            val filteredSections = mutableListOf<HomeCategoryRow>()
            val bannedKeywords = listOf("short tv", "shorts", "fight zone", "banner", "update")

            for (sec in rawItems) {
                val title = sec.title.trim()
                val titleLower = title.lowercase()

                if (bannedKeywords.any { titleLower.contains(it) }) continue

                val safeSubjects = sec.subjects.filter { isAllowed(it) }
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
            val items = response.data?.items?.filter { isAllowed(it) } ?: emptyList()
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
            val items = response.data?.items?.filter { isAllowed(it) } ?: emptyList()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlayableStreams(subjectId: String, se: Int = 0, ep: Int = 0): Result<List<PlayableStream>> =
        withContext(Dispatchers.IO) {
            try {
                var playInfoResp = api.getPlayInfo(subjectId = subjectId, se = se, ep = ep)
                var streams = playInfoResp.data?.streams ?: emptyList()

                // Fallback 1: If series queried without episode or episode not found, try ep=1, se=1
                if (streams.isEmpty() && se == 0 && ep == 0) {
                    val seriesResp = api.getPlayInfo(subjectId = subjectId, se = 1, ep = 1)
                    val sStreams = seriesResp.data?.streams ?: emptyList()
                    if (sStreams.isNotEmpty()) {
                        streams = sStreams
                    }
                }

                // Fallback 2: If standalone movie mistakenly queried with ep>0, try se=0, ep=0
                if (streams.isEmpty() && (se > 0 || ep > 0)) {
                    val movieResp = api.getPlayInfo(subjectId = subjectId, se = 0, ep = 0)
                    val mStreams = movieResp.data?.streams ?: emptyList()
                    if (mStreams.isNotEmpty()) {
                        streams = mStreams
                    }
                }

                val playable = mutableListOf<PlayableStream>()

                for (s in streams) {
                    val cookie = s.signCookie ?: ""
                    val codec = s.codecName ?: "hevc"
                    val size = s.size
                    val duration = s.duration
                    val rInt = if (s.resolution > 0) s.resolution else 1080

                    if (cookie.contains("CloudFront-Policy=")) {
                        val baseDashUrl = MovieBoxSigner.extractBaseDashUrl(cookie)
                        val mpdUrl = s.dashUrl ?: if (baseDashUrl != null) "$baseDashUrl/index.mpd" else null
                        if (mpdUrl != null) {
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
                    } else {
                        val url = s.url ?: s.resourceLink ?: ""
                        if (url.isNotEmpty() && !url.contains("9a0461bc39da389663bf3dbb17091d3f")) {
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
                    var resResp = api.getResources(subjectId = subjectId, se = se, ep = ep, page = 1)
                    var rawList = resResp.data?.list ?: emptyList()

                    if (rawList.isEmpty() && (se > 0 || ep > 0)) {
                        resResp = api.getResources(subjectId = subjectId, se = 0, ep = 0, page = 1)
                        rawList = resResp.data?.list ?: emptyList()
                    }
                    if (rawList.isEmpty() && se == 0 && ep == 0) {
                        resResp = api.getResources(subjectId = subjectId, se = 1, ep = 1, page = 1)
                        rawList = resResp.data?.list ?: emptyList()
                    }

                    for (r in rawList) {
                        val cookie = r.signCookie ?: ""
                        val link = r.resourceLink ?: ""
                        val res = if (r.resolution > 0) r.resolution else 720

                        if (cookie.contains("CloudFront-Policy=")) {
                            val baseDashUrl = MovieBoxSigner.extractBaseDashUrl(cookie)
                            val mpdUrl = r.dashUrl ?: if (baseDashUrl != null) "$baseDashUrl/index.mpd" else null
                            if (mpdUrl != null) {
                                playable.add(
                                    PlayableStream(
                                        title = "${res}P HD",
                                        resolution = res,
                                        codecName = r.codecName ?: "hevc",
                                        size = r.size,
                                        duration = 0L,
                                        streamUrl = mpdUrl,
                                        isDash = true,
                                        signCookie = cookie,
                                        season = se,
                                        episode = ep
                                    )
                                )
                            }
                        } else if (link.isNotEmpty() && !link.contains("9a0461bc39da389663bf3dbb17091d3f") && !link.contains("/other/2026/09/01/")) {
                            playable.add(
                                PlayableStream(
                                    title = "${res}P",
                                    resolution = res,
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

                val distinct = playable.distinctBy { "${it.resolution}_${it.streamUrl}" }
                Result.success(distinct.sortedByDescending { it.resolution })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
