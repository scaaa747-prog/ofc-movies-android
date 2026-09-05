package com.ofc.movies.data.model

import com.google.gson.annotations.SerializedName

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

    @SerializedName("corner", alternate = ["cornerBadge"])
    val corner: Any? = null,

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
            is String -> corner
            is Map<*, *> -> (corner["text"] as? String)
            else -> null
        }

    val displayYear: String
        get() = year?.take(4) ?: ""

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

    @SerializedName("durationSeconds", alternate = ["duration"])
    val durationSeconds: Long? = null,

    @SerializedName("releaseDate")
    val releaseDate: String? = null,

    @SerializedName("genre", alternate = ["genres"])
    val genre: String? = null,

    @SerializedName("dubs")
    val dubs: List<DubItem> = emptyList(),

    @SerializedName("staffList")
    val cast: List<CastItem> = emptyList(),

    @SerializedName("subjectType")
    val subjectType: Int = 0
) {
    val coverUrl: String
        get() = when (cover) {
            is String -> cover
            is Map<*, *> -> (cover["url"] as? String) ?: ""
            else -> ""
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

    @SerializedName("resolutions")
    val resolutions: String? = null,

    @SerializedName("size")
    val size: Long = 0L,

    @SerializedName("duration")
    val duration: Long = 0L,

    @SerializedName("codecName")
    val codecName: String? = null,

    @SerializedName("signCookie")
    val signCookie: String? = null
)

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
    val seasonNumber: Int = 1,

    @SerializedName("maxEp")
    val maxEpisode: Int = 1,

    @SerializedName("allEp")
    val allEp: String? = null
)

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
    val resolution: Int = 0,

    @SerializedName("codecName")
    val codecName: String? = null,

    @SerializedName("size")
    val size: Long = 0L,

    @SerializedName("resourceLink")
    val resourceLink: String? = null,

    @SerializedName("se")
    val se: Int = 0,

    @SerializedName("ep")
    val ep: Int = 0
)

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
