package com.ofc.movies.data.download

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.ofc.movies.data.api.MovieBoxSigner
import java.io.File

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

    fun createCacheDataSourceFactory(context: Context, signCookie: String? = null): CacheDataSource.Factory {
        val cache = getCache(context)
        val cleanCookie = signCookie?.replace("\r", "")?.replace("\n", "")?.trim()?.trimEnd(';') ?: ""
        val headers = mutableMapOf(
            "User-Agent" to MovieBoxSigner.ANDROID_USER_AGENT,
            "Referer" to "https://www.movieboxpro.app/"
        )
        if (cleanCookie.isNotEmpty()) {
            headers["Cookie"] = cleanCookie
        }

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(MovieBoxSigner.ANDROID_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(25000)
            .setDefaultRequestProperties(headers)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
