package com.ofc.movies.data.update

import com.google.gson.annotations.SerializedName

data class GitHubRelease(
    @SerializedName("id") val id: Long,
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("prerelease") val prerelease: Boolean,
    @SerializedName("assets") val assets: List<GitHubAsset>?
)

data class GitHubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("size") val size: Long,
    @SerializedName("browser_download_url") val browserDownloadUrl: String
)

data class AppUpdateInfo(
    val versionName: String,
    val releaseTitle: String,
    val changelog: String,
    val apkUrl: String,
    val apkSize: Long,
    val isPrerelease: Boolean
)
