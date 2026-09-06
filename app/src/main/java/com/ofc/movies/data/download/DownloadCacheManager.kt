package com.ofc.movies.data.download

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.dash.offline.DashDownloader
import com.ofc.movies.data.api.MovieBoxSigner
import java.io.File
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
object DownloadCacheManager {
    @Volatile
    private var simpleCache: SimpleCache? = null

    @Volatile
    private var databaseProvider: StandaloneDatabaseProvider? = null

    fun getCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: run {
                val dbProvider = databaseProvider ?: StandaloneDatabaseProvider(context.applicationContext).also {
                    databaseProvider = it
                }
                val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
                val downloadDir = File(baseDir, "ofc_downloads_cache")
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                SimpleCache(downloadDir, NoOpCacheEvictor(), dbProvider).also {
                    simpleCache = it
                }
            }
        }
    }

    fun createHttpDataSourceFactory(signCookie: String? = null): DefaultHttpDataSource.Factory {
        val cleanCookie = signCookie?.replace("\r", "")?.replace("\n", "")?.trim()?.trimEnd(';') ?: ""
        val headers = mutableMapOf(
            "User-Agent" to MovieBoxSigner.ANDROID_USER_AGENT,
            "Referer" to "https://www.movieboxpro.app/"
        )
        if (cleanCookie.isNotEmpty()) {
            headers["Cookie"] = cleanCookie
        }

        return DefaultHttpDataSource.Factory()
            .setUserAgent(MovieBoxSigner.ANDROID_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(25000)
            .setDefaultRequestProperties(headers)
    }

    /**
     * Cache DataSource Factory for downloading. Allows writing data into the offline cache.
     */
    fun createCacheDataSourceFactory(context: Context, signCookie: String? = null): CacheDataSource.Factory {
        val cache = getCache(context)
        val httpFactory = createHttpDataSourceFactory(signCookie)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Read-only Cache DataSource Factory for playing downloaded offline content.
     * Prevents any extra data from being written to internal storage during playback.
     */
    fun createReadOnlyCacheDataSourceFactory(context: Context, signCookie: String? = null): CacheDataSource.Factory {
        val cache = getCache(context)
        val httpFactory = createHttpDataSourceFactory(signCookie)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpFactory)
            .setCacheWriteDataSinkFactory(null) // READ-ONLY: Never write to cache during playback!
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Completely removes all cached DASH segments, audio/video streams, and manifest
     * for the specified stream URL to free up device storage.
     */
    fun removeDashDownload(context: Context, streamUrl: String, signCookie: String? = null) {
        val executor = Executors.newSingleThreadExecutor()
        try {
            val mediaItem = MediaItem.Builder()
                .setUri(streamUrl)
                .setMimeType(MimeTypes.APPLICATION_MPD)
                .build()
            val cacheFactory = createCacheDataSourceFactory(context, signCookie)
            val downloader = DashDownloader(mediaItem, cacheFactory, executor)
            downloader.remove()
        } catch (e: Throwable) {
            android.util.Log.w("DownloadCacheManager", "DashDownloader.remove() encountered error: ${e.message}")
        } finally {
            executor.shutdown()
        }

        // Also purge resource from SimpleCache directly
        try {
            val cache = getCache(context)
            cache.removeResource(streamUrl)
        } catch (e: Throwable) {}
    }

    /**
     * Prunes orphaned video cache files that do not correspond to any active downloads.
     */
    fun pruneOrphanedCache(context: Context, activeStreamUrls: Set<String>) {
        try {
            val cache = getCache(context)
            val allKeys = cache.keys.toList()
            for (key in allKeys) {
                // If the key is not part of any active download, remove it
                val isNeeded = activeStreamUrls.any { activeUrl ->
                    if (activeUrl.isEmpty()) false
                    else {
                        val activePrefix = activeUrl.substringBeforeLast("/")
                        key.startsWith(activePrefix) || key == activeUrl
                    }
                }
                if (!isNeeded) {
                    try {
                        cache.removeResource(key)
                    } catch (e: Throwable) {}
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("DownloadCacheManager", "Failed to prune cache", e)
        }
    }
}
