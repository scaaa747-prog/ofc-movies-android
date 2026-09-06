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

    companion object {
        // LRU memory cache for searches to avoid consuming internet on repeated queries or backspaces
        private val searchCache = android.util.LruCache<String, List<MovieItem>>(100)
    }

    suspend fun searchMovies(query: String, page: Int = 1): Result<List<MovieItem>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim().lowercase()
        val cacheKey = "${trimmed}_p$page"
        if (page == 1) {
            val cached = searchCache.get(cacheKey)
            if (cached != null) {
                return@withContext Result.success(cached)
            }
        }
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
            if (items.isNotEmpty()) {
                searchCache.put(cacheKey, items)
            }
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

                    val resList = (s.resolutions ?: "1080,720,480")
                        .split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .filter { it > 0 }
                        .ifEmpty { listOf(if (s.resolution > 0) s.resolution else 1080) }

                    if (cookie.contains("CloudFront-Policy=")) {
                        val baseDashUrl = MovieBoxSigner.extractBaseDashUrl(cookie)
                        val mpdUrl = s.dashUrl ?: if (baseDashUrl != null) "$baseDashUrl/index.mpd" else null
                        if (mpdUrl != null) {
                            for (rInt in resList) {
                                val scaledSize = when {
                                    rInt <= 480 -> (size * 0.35).toLong()
                                    rInt <= 720 -> (size * 0.60).toLong()
                                    else -> size
                                }
                                playable.add(
                                    PlayableStream(
                                        title = "${rInt}P HD",
                                        resolution = rInt,
                                        codecName = codec,
                                        size = scaledSize,
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
                        // Direct MP4 stream ONLY if not a CloudFront DASH stream and not a fake promo video
                        val url = s.url ?: s.resourceLink ?: ""
                        if (url.isNotEmpty() && !MovieBoxSigner.isFakeClipUrl(url)) {
                            for (rInt in resList) {
                                playable.add(
                                    PlayableStream(
                                        title = "${rInt}P",
                                        resolution = rInt,
                                        codecName = codec,
                                        size = size,
                                        duration = duration,
                                        streamUrl = url,
                                        isDash = false,
                                        signCookie = if (cookie.isNotEmpty()) cookie else null,
                                        season = se,
                                        episode = ep
                                    )
                                )
                            }
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
                        } else if (link.isNotEmpty() && !MovieBoxSigner.isFakeClipUrl(link)) {
                            playable.add(
                                PlayableStream(
                                    title = "${res}P",
                                    resolution = res,
                                    codecName = r.codecName ?: "h264",
                                    size = r.size,
                                    duration = 0L,
                                    streamUrl = link,
                                    isDash = false,
                                    signCookie = if (cookie.isNotEmpty()) cookie else null,
                                    season = se,
                                    episode = ep
                                )
                            )
                        }
                    }
                }

                val distinct = playable.distinctBy { "${it.resolution}_${it.isDash}_${it.streamUrl}" }
                Result.success(distinct.sortedByDescending { it.resolution })
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getDownloadOptions(
        subjectId: String,
        se: Int = 0,
        ep: Int = 0,
        preloadedDetail: MovieDetailData? = null
    ): List<DownloadQualityOption> = withContext(Dispatchers.IO) {
        val options = mutableListOf<DownloadQualityOption>()

        // 1. First priority: Real direct MP4 files from resourceDetectors
        val detail = preloadedDetail ?: getMovieDetail(subjectId).getOrNull()
        val detectors = detail?.resourceDetectors ?: emptyList()
        for (rd in detectors) {
            val list = rd.resolutionList
            for (rl in list) {
                val matchesEpisode = if (detail?.subjectType == 2) {
                    (rl.se == se || (se == 0 && rl.se <= 1)) && (rl.ep == ep || rl.episode == ep)
                } else {
                    true
                }
                if (matchesEpisode && !rl.resourceLink.isNullOrBlank()) {
                    val res = rl.resolution
                    val size = rl.size
                    val title = when {
                        res >= 1080 -> "1080p Full HD"
                        res >= 720 -> "720p HD"
                        res >= 480 -> "480p SD"
                        res > 0 -> "${res}p"
                        else -> "Standard Quality"
                    }
                    options.add(
                        DownloadQualityOption(
                            title = title,
                            resolution = res,
                            sizeBytes = size,
                            sizeFormatted = formatDownloadSize(size, res),
                            streamUrl = rl.resourceLink,
                            codec = rl.codecName ?: "h264",
                            season = se,
                            episode = ep
                        )
                    )
                }
            }
            if (options.isEmpty() && !rd.downloadUrl.isNullOrBlank()) {
                val sBytes = rd.totalSize?.toLongOrNull() ?: 0L
                options.add(
                    DownloadQualityOption(
                        title = "720p HD",
                        resolution = 720,
                        sizeBytes = sBytes,
                        sizeFormatted = formatDownloadSize(sBytes, 720),
                        streamUrl = rd.downloadUrl,
                        codec = "h264",
                        season = se,
                        episode = ep
                    )
                )
            }
        }

        // 2. Secondary fallback: resources API endpoint
        if (options.isEmpty()) {
            try {
                val resResp = api.getResources(subjectId = subjectId, se = se, ep = ep, page = 1)
                val list = resResp.data?.list ?: emptyList()
                for (r in list) {
                    val link = r.resourceLink ?: ""
                    if (link.isNotEmpty()) {
                        val res = if (r.resolution > 0) r.resolution else 720
                        val size = r.size
                        val title = when {
                            res >= 1080 -> "1080p Full HD"
                            res >= 720 -> "720p HD"
                            res >= 480 -> "480p SD"
                            else -> "${res}p"
                        }
                        options.add(
                            DownloadQualityOption(
                                title = title,
                                resolution = res,
                                sizeBytes = size,
                                sizeFormatted = formatDownloadSize(size, res),
                                streamUrl = link,
                                signCookie = r.signCookie,
                                codec = r.codecName ?: "h264",
                                season = se,
                                episode = ep
                            )
                        )
                    }
                }
            } catch (e: Exception) {}
        }

        // 3. Third fallback: playable streams
        if (options.isEmpty()) {
            try {
                val streams = getPlayableStreams(subjectId, se, ep).getOrNull() ?: emptyList()
                for (s in streams) {
                    if (s.streamUrl.isNotEmpty()) {
                        val res = s.resolution
                        val title = when {
                            res >= 1080 -> "1080p Full HD"
                            res >= 720 -> "720p HD"
                            res >= 480 -> "480p SD"
                            else -> s.title
                        }
                        options.add(
                            DownloadQualityOption(
                                title = title,
                                resolution = res,
                                sizeBytes = s.size,
                                sizeFormatted = formatDownloadSize(s.size, res),
                                streamUrl = s.streamUrl,
                                signCookie = s.signCookie,
                                codec = s.codecName,
                                season = se,
                                episode = ep
                            )
                        )
                    }
                }
            } catch (e: Exception) {}
        }

        val distinct = options.distinctBy { it.resolution }.sortedByDescending { it.resolution }
        return@withContext distinct
    }

    suspend fun getDownloadStream(subjectId: String, se: Int = 0, ep: Int = 0): PlayableStream? =
        withContext(Dispatchers.IO) {
            val options = getDownloadOptions(subjectId, se, ep)
            val best = options.firstOrNull() ?: return@withContext null
            PlayableStream(
                title = best.title,
                resolution = best.resolution,
                codecName = best.codec,
                size = best.sizeBytes,
                duration = 0L,
                streamUrl = best.streamUrl,
                isDash = false,
                signCookie = best.signCookie,
                season = se,
                episode = ep
            )
        }
}
