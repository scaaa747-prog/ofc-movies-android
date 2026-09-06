package com.ofc.movies.data.model

import com.google.gson.annotations.SerializedName

private val TITLE_LANG_REGEX = Regex("""\[([^\]]+)\]|\(([^\)]+)\)""")

private val KNOWN_LANGUAGES = listOf(
    "Hindi", "Tamil", "Telugu", "Kannada", "Malayalam", "Bengali", "Marathi",
    "Punjabi", "Gujarati", "Bhojpuri", "Urdu", "English", "Spanish", "French",
    "German", "Italian", "Korean", "Japanese", "Chinese", "Turkish", "Arabic",
    "Thai", "Vietnamese", "Indonesian", "Dual Audio", "Multi Audio", "Dual", "Multi"
)

private fun isIgnoredCornerTag(tag: String?): Boolean {
    if (tag.isNullOrBlank()) return true
    val lower = tag.trim().lowercase()
    return lower in listOf("4k", "4k uhd", "uhd", "free", "vip", "new", "hot")
}

private fun matchKnownLanguage(text: String?): String? {
    if (text.isNullOrBlank()) return null
    val clean = text.trim()
    val lower = clean.lowercase()
    if (lower.contains("dual")) return "Dual Audio"
    if (lower.contains("multi")) return "Multi Audio"
    for (lang in KNOWN_LANGUAGES) {
        if (lower.contains(lang.lowercase())) {
            return lang
        }
    }
    return null
}

data class MovieItem(
    @SerializedName("subjectId", alternate = ["id"])
    val id: String = "",

    @SerializedName("title", alternate = ["subjectName"])
    val title: String = "",

    @SerializedName("cover", alternate = ["img", "image"])
    private val rawCover: Any? = null,

    @SerializedName("imdbRatingValue", alternate = ["rating", "imdbRating", "score"])
    val rating: String? = null,

    @SerializedName("releaseDate", alternate = ["year", "releaseYear"])
    val year: String? = null,

    @SerializedName("genre", alternate = ["genres"])
    val genre: String? = null,

    @SerializedName("description", alternate = ["desc", "synopsis", "introduction"])
    val description: String? = null,

    @SerializedName("corner")
    val corner: Any? = null,

    @SerializedName("cornerBadge")
    val cornerBadge: Any? = null,

    @SerializedName("language")
    val language: String? = null,

    @SerializedName("dubs")
    val dubs: List<DubItem> = emptyList(),

    @SerializedName("isCam")
    val isCam: Boolean? = null,

    @SerializedName("duration", alternate = ["durationSeconds"])
    val duration: Any? = null,

    @SerializedName("subjectType")
    val subjectType: Int? = null,

    @SerializedName("restrictKid")
    val restrictKid: Int? = null,

    @SerializedName("contentRating")
    val contentRating: String? = null
) {
    val coverUrl: String
        get() = extractCoverUrl(rawCover)

    val cornerText: String?
        get() = when (corner) {
            is String -> corner.takeIf { it.isNotBlank() }
            is Map<*, *> -> (corner["text"] as? String)?.takeIf { it.isNotBlank() }
            else -> null
        }

    val displayYear: String
        get() = year?.take(4) ?: ""

    val isCamFilm: Boolean
        get() {
            if (isCam == true) return true
            val t = title.lowercase()
            return t.contains("[cam]") || t.contains("(cam)") || t.contains("cam-rip") || t.contains("hdcam")
        }

    val audioLanguage: String?
        get() {
            // 1. Title bracket match (e.g. [Hindi], [Tamil], [English], [Dual Audio])
            val bracketMatch = TITLE_LANG_REGEX.find(title)
            if (bracketMatch != null) {
                val candidate = (bracketMatch.groupValues[1].ifEmpty { bracketMatch.groupValues.getOrNull(2) ?: "" }).trim()
                val matched = matchKnownLanguage(candidate)
                if (matched != null) return matched
            }

            // 2. Corner text from API (excluding fake 4K and promotional tags)
            val c = cornerText?.trim()
            if (!c.isNullOrBlank() && !isIgnoredCornerTag(c)) {
                val matched = matchKnownLanguage(c)
                if (matched != null) return matched
                if (c.length <= 12) return c
            }

            // 3. Dubs list
            val firstDub = dubs.firstOrNull()?.lanName?.trim()
            if (!firstDub.isNullOrBlank()) {
                val matched = matchKnownLanguage(firstDub)
                if (matched != null) return matched
                if (firstDub.length <= 12) return firstDub
            }

            // 4. API language field
            val lang = language?.trim()
            if (!lang.isNullOrBlank()) {
                val firstLang = lang.split(',').firstOrNull()?.trim()
                if (!firstLang.isNullOrBlank()) {
                    return matchKnownLanguage(firstLang) ?: firstLang.take(12)
                }
            }

            return null
        }

    val displayTitle: String
        get() {
            return title
                .replace(Regex("""\[(Hindi|Tamil|Telugu|Kannada|Malayalam|English|Dual|Multi|CAM|TS|HD-TC|HDCAM)[^\]]*\]""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\s+"""), " ")
                .trim()
                .ifEmpty { title }
        }

    val isExplicitAdult: Boolean
        get() {
            val c = (cornerText ?: "").lowercase()
            val t = title.lowercase()
            val g = (genre ?: "").lowercase()
            if (t.contains("erotic") || t.contains("porn") || t.contains("xxx") || t.contains("hentai") || t.contains("jav")) return true
            if (c.contains("erotic") || c.contains("porn") || c.contains("xxx") || c.contains("hentai")) return true
            if (g.contains("erotic") || g.contains("porn") || g.contains("adult movie")) return true
            return false
        }

    val isFamilySafe: Boolean
        get() {
            if (isExplicitAdult) return false
            if (restrictKid == 1) return false
            val cr = (contentRating ?: "").uppercase()
            if (cr in listOf("R", "NC-17", "18+", "TV-MA", "XXX")) return false
            val c = (cornerText ?: "").lowercase()
            if (c.contains("18+") || c.contains("adult")) return false
            return true
        }

    private fun extractCoverUrl(cover: Any?): String {
        return when (cover) {
            is String -> cover
            is Map<*, *> -> (cover["url"] as? String) ?: ""
            is List<*> -> {
                val first = cover.firstOrNull()
                when (first) {
                    is String -> first
                    is Map<*, *> -> (first["url"] as? String) ?: ""
                    else -> ""
                }
            }
            else -> ""
        }
    }
}

data class HomeCategoryRow(
    val title: String = "",
    val type: String = "",
    val items: List<MovieItem> = emptyList()
)

data class TabOperatingResponse(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: TabOperatingData? = null
)

data class TabOperatingData(
    @SerializedName("tabId")
    val tabId: Int = 0,
    @SerializedName("items")
    val items: List<TabOperatingSection> = emptyList()
)

data class TabOperatingSection(
    @SerializedName("title")
    val title: String = "",
    @SerializedName("type")
    val type: String = "",
    @SerializedName("subjects", alternate = ["items"])
    val subjects: List<MovieItem> = emptyList()
)

data class SubjectDetailResponse(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("data")
    val data: MovieDetailData? = null
)

data class MovieDetailData(
    @SerializedName("subjectId")
    val subjectId: String = "",

    @SerializedName("title")
    val title: String = "",

    @SerializedName("description", alternate = ["desc", "synopsis", "introduction"])
    val description: String? = null,

    @SerializedName("cover")
    val cover: Any? = null,

    @SerializedName("imdbRatingValue", alternate = ["rating"])
    val rating: String? = null,

    @SerializedName("durationSeconds")
    val rawDurationSeconds: Any? = null,

    @SerializedName("duration")
    val rawDuration: Any? = null,

    @SerializedName("releaseDate")
    val releaseDate: String? = null,

    @SerializedName("genre", alternate = ["genres"])
    val genre: String? = null,

    @SerializedName("dubs")
    val dubs: List<DubItem> = emptyList(),

    @SerializedName("staffList")
    val cast: List<CastItem> = emptyList(),

    @SerializedName("language")
    val language: String? = null,

    @SerializedName("corner")
    val corner: Any? = null,

    @SerializedName("isCam")
    val isCam: Boolean? = null,

    @SerializedName("resourceDetectors")
    val resourceDetectors: List<ResourceDetector> = emptyList(),

    @SerializedName("subjectType")
    val rawSubjectType: Any? = null
) {
    val coverUrl: String
        get() = when (cover) {
            is String -> cover
            is Map<*, *> -> (cover["url"] as? String) ?: ""
            else -> ""
        }

    val durationSeconds: Long
        get() = when (val d = rawDurationSeconds ?: rawDuration) {
            is Number -> d.toLong()
            is String -> d.toLongOrNull() ?: 0L
            else -> 0L
        }

    val subjectType: Int
        get() = when (rawSubjectType) {
            is Number -> rawSubjectType.toInt()
            is String -> rawSubjectType.toIntOrNull() ?: 0
            else -> 0
        }

    val isCamFilm: Boolean
        get() {
            if (isCam == true) return true
            val t = title.lowercase()
            return t.contains("[cam]") || t.contains("(cam)") || t.contains("cam-rip") || t.contains("hdcam")
        }

    val audioLanguage: String?
        get() {
            val bracketMatch = TITLE_LANG_REGEX.find(title)
            if (bracketMatch != null) {
                val candidate = (bracketMatch.groupValues[1].ifEmpty { bracketMatch.groupValues.getOrNull(2) ?: "" }).trim()
                val matched = matchKnownLanguage(candidate)
                if (matched != null) return matched
            }

            val c = when (corner) {
                is String -> corner.takeIf { it.isNotBlank() }
                is Map<*, *> -> (corner["text"] as? String)?.takeIf { it.isNotBlank() }
                else -> null
            }
            if (!c.isNullOrBlank() && !isIgnoredCornerTag(c)) {
                val matched = matchKnownLanguage(c)
                if (matched != null) return matched
                if (c.length <= 12) return c
            }

            val firstDub = dubs.firstOrNull()?.lanName?.trim()
            if (!firstDub.isNullOrBlank()) {
                val matched = matchKnownLanguage(firstDub)
                if (matched != null) return matched
                if (firstDub.length <= 12) return firstDub
            }

            val lang = language?.trim()
            if (!lang.isNullOrBlank()) {
                val firstLang = lang.split(',').firstOrNull()?.trim()
                if (!firstLang.isNullOrBlank()) {
                    return matchKnownLanguage(firstLang) ?: firstLang.take(12)
                }
            }

            return null
        }
}

data class DubItem(
    @SerializedName("subjectId")
    val subjectId: String = "",

    @SerializedName("lanName")
    val lanName: String = "",

    @SerializedName("original")
    val isOriginal: Boolean = false
)

data class CastItem(
    @SerializedName("name")
    val name: String = "",

    @SerializedName("character")
    val role: String? = null,

    @SerializedName("avatarUrl", alternate = ["img"])
    val avatarUrl: String? = null
)

data class PlayInfoResponse(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("data")
    val data: PlayInfoData? = null
)

data class PlayInfoData(
    @SerializedName("streams")
    val streams: List<PlayInfoStream> = emptyList()
)

data class PlayInfoStream(
    @SerializedName("format")
    val format: String? = null,

    @SerializedName("id")
    val id: String? = null,

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("dashUrl")
    val dashUrl: String? = null,

    @SerializedName("resourceLink")
    val resourceLink: String? = null,

    @SerializedName("resolutions")
    val resolutions: String? = null,

    @SerializedName("resolution")
    val rawResolution: Any? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("size")
    val rawSize: Any? = null,

    @SerializedName("duration")
    val rawDuration: Any? = null,

    @SerializedName("codecName")
    val codecName: String? = null,

    @SerializedName("signCookie")
    val signCookie: String? = null
) {
    val size: Long
        get() = when (rawSize) {
            is Number -> rawSize.toLong()
            is String -> rawSize.toLongOrNull() ?: 0L
            else -> 0L
        }

    val duration: Long
        get() = when (rawDuration) {
            is Number -> rawDuration.toLong()
            is String -> rawDuration.toLongOrNull() ?: 0L
            else -> 0L
        }

    val resolution: Int
        get() = when (rawResolution) {
            is Number -> rawResolution.toInt()
            is String -> rawResolution.toIntOrNull() ?: 0
            else -> resolutions?.split(",")?.firstOrNull()?.trim()?.toIntOrNull() ?: 0
        }
}

data class SeasonInfoResponse(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("data")
    val data: SeasonInfoData? = null
)

data class SeasonInfoData(
    @SerializedName("subjectId")
    val subjectId: String = "",

    @SerializedName("subjectType")
    val subjectType: Int = 0,

    @SerializedName("seasons")
    val seasons: List<SeasonItem> = emptyList()
)

data class SeasonItem(
    @SerializedName("se")
    val rawSeasonNumber: Any? = null,

    @SerializedName("maxEp")
    val rawMaxEpisode: Any? = null,

    @SerializedName("allEp")
    val allEp: String? = null
) {
    val seasonNumber: Int
        get() = when (rawSeasonNumber) {
            is Number -> rawSeasonNumber.toInt()
            is String -> rawSeasonNumber.toIntOrNull() ?: 1
            else -> 1
        }

    val maxEpisode: Int
        get() = when (rawMaxEpisode) {
            is Number -> rawMaxEpisode.toInt()
            is String -> rawMaxEpisode.toIntOrNull() ?: 1
            else -> 1
        }
}

data class SearchRequestBody(
    @SerializedName("keyword")
    val keyword: String,
    @SerializedName("page")
    val page: Int = 1,
    @SerializedName("perPage")
    val perPage: Int = 20,
    @SerializedName("subjectType")
    val subjectType: Int = 0
)

data class SearchResponse(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("data")
    val data: SearchData? = null
)

data class SearchData(
    @SerializedName("items", alternate = ["list"])
    val items: List<MovieItem> = emptyList()
)

data class RelatedRecRequestBody(
    @SerializedName("subjectId")
    val subjectId: String
)

data class RelatedRecResponse(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("data")
    val data: RelatedRecData? = null
)

data class RelatedRecData(
    @SerializedName("items")
    val items: List<MovieItem> = emptyList()
)

data class ResourcesResponse(
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("data")
    val data: ResourcesData? = null
)

data class ResourcesData(
    @SerializedName("list")
    val list: List<StreamResource> = emptyList()
)

data class StreamResource(
    @SerializedName("resolution")
    val rawResolution: Any? = null,

    @SerializedName("codecName")
    val codecName: String? = null,

    @SerializedName("size")
    val rawSize: Any? = null,

    @SerializedName("resourceLink")
    val resourceLink: String? = null,

    @SerializedName("dashUrl")
    val dashUrl: String? = null,

    @SerializedName("signCookie")
    val signCookie: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("se")
    val se: Int = 0,

    @SerializedName("ep")
    val ep: Int = 0
) {
    val resolution: Int
        get() = when (rawResolution) {
            is Number -> rawResolution.toInt()
            is String -> rawResolution.toIntOrNull() ?: 0
            else -> 0
        }

    val size: Long
        get() = when (rawSize) {
            is Number -> rawSize.toLong()
            is String -> rawSize.toLongOrNull() ?: 0L
            else -> 0L
        }
}

data class PlayableStream(
    val title: String,
    val resolution: Int,
    val codecName: String,
    val size: Long,
    val duration: Long,
    val streamUrl: String,
    val isDash: Boolean,
    val signCookie: String? = null,
    val season: Int = 0,
    val episode: Int = 0
)

data class ContinueWatchingItem(
    val id: String,
    val title: String,
    val coverUrl: String,
    val progress: Float,
    val durationMinutes: Int,
    val lastWatchedEpisode: String? = null
)

data class ResourceDetector(
    @SerializedName("type")
    val type: Int = 0,
    @SerializedName("totalEpisode")
    val totalEpisode: Int = 0,
    @SerializedName("totalSize")
    val totalSize: String? = null,
    @SerializedName("downloadUrl")
    val downloadUrl: String? = null,
    @SerializedName("resolutionList")
    val resolutionList: List<ResolutionListItem> = emptyList()
)

data class ResolutionListItem(
    @SerializedName("episode")
    val episode: Int = 0,
    @SerializedName("se")
    val se: Int = 0,
    @SerializedName("ep")
    val ep: Int = 0,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("resourceLink")
    val resourceLink: String? = null,
    @SerializedName("size")
    val rawSize: Any? = null,
    @SerializedName("resolution")
    val rawResolution: Any? = null,
    @SerializedName("duration")
    val rawDuration: Any? = null,
    @SerializedName("codecName")
    val codecName: String? = null
) {
    val resolution: Int
        get() = when (rawResolution) {
            is Number -> rawResolution.toInt()
            is String -> rawResolution.toIntOrNull() ?: 0
            else -> 0
        }

    val size: Long
        get() = when (rawSize) {
            is Number -> rawSize.toLong()
            is String -> rawSize.toLongOrNull() ?: 0L
            else -> 0L
        }
}

data class DownloadQualityOption(
    val title: String,
    val resolution: Int,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val streamUrl: String,
    val signCookie: String? = null,
    val codec: String = "h264",
    val season: Int = 0,
    val episode: Int = 0
)

fun formatDownloadSize(sizeBytes: Long, resolution: Int): String {
    return if (sizeBytes > 0L) {
        if (sizeBytes >= 1_000_000_000L) {
            "%.1f GB".format(sizeBytes.toDouble() / (1024 * 1024 * 1024))
        } else {
            "${sizeBytes / (1024 * 1024)} MB"
        }
    } else {
        when {
            resolution >= 1080 -> "~1.2 GB"
            resolution >= 720 -> "~650 MB"
            resolution >= 480 -> "~300 MB"
            else -> "~200 MB"
        }
    }
}
