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
    val subjectType: Int? = null
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
    @SerializedName("title")
    val title: String = "",
    
    @SerializedName("type")
    val type: String = "",
    
    @SerializedName("subjects", alternate = ["items", "list"])
    val items: List<MovieItem> = emptyList()
)

data class HomeFeedResponse(
    @SerializedName("tabId")
    val tabId: Int = 0,
    
    @SerializedName("items")
    val items: List<HomeCategoryRow> = emptyList()
)

data class TrendingResponse(
    @SerializedName("data")
    val data: TrendingData? = null
)

data class TrendingData(
    @SerializedName("items")
    val items: List<MovieItem> = emptyList()
)

data class SearchResponse(
    @SerializedName("items")
    val items: List<MovieItem> = emptyList()
)

data class SuggestResponse(
    @SerializedName("data")
    val data: List<MovieItem> = emptyList()
)

data class MovieDetailResponse(
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
    val durationSeconds: Long? = null,
    
    @SerializedName("releaseDate")
    val releaseDate: String? = null,
    
    @SerializedName("dubs")
    val dubs: List<DubItem> = emptyList(),
    
    @SerializedName("staffList")
    val cast: List<CastItem> = emptyList()
)

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

data class ResourcesResponse(
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
    
    @SerializedName("token")
    val token: String? = null,
    
    @SerializedName("streamUrl")
    val streamUrl: String? = null,
    
    @SerializedName("isDash")
    val isDash: Boolean = false
)

data class SubtitlesResponse(
    @SerializedName("subtitles")
    val subtitles: List<SubtitleItem> = emptyList()
)

data class SubtitleItem(
    @SerializedName("language")
    val language: String = "",
    
    @SerializedName("url")
    val url: String = ""
)

data class ContinueWatchingItem(
    val id: String,
    val title: String,
    val coverUrl: String,
    val progress: Float, // 0.0f to 1.0f
    val durationMinutes: Int,
    val lastWatchedEpisode: String? = null
)
