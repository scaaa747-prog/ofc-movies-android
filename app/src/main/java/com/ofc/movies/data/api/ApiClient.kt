package com.ofc.movies.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    const val BASE_URL = "https://api6.aoneroom.com/"

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(MovieBoxAuthInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    private val gson: Gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val service: MovieApiService by lazy {
        retrofit.create(MovieApiService::class.java)
    }

    fun getAbsoluteUrl(url: String): String {
        if (url.isEmpty()) return ""
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url
        }
        val cleanPath = url.trimStart('/')
        return "https://pbcdn.aoneroom.com/$cleanPath"
    }

    /**
     * Efficiently resizes image on CDN and delivers lightweight WebP.
     * Reduces network consumption from ~750KB-2MB per poster down to ~15KB-22KB.
     */
    fun getThumbnailUrl(url: String, width: Int = 240): String {
        val abs = getAbsoluteUrl(url)
        if (abs.isEmpty()) return ""
        if (abs.contains("pbcdn.aoneroom.com")) {
            val separator = if (abs.contains("?")) "&" else "?"
            return "$abs${separator}x-oss-process=image/resize,w_$width/format,webp"
        }
        return abs
    }
}
