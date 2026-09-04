package com.ofc.movies.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Default to the current production tunnel or your self-hosted instance
    var baseUrl: String = "https://mountains-brings-arrange-highlight.trycloudflare.com/"
        set(value) {
            val formatted = if (value.endsWith("/")) value else "$value/"
            field = formatted
            retrofit = buildRetrofit(formatted)
            service = retrofit.create(MovieApiService::class.java)
        }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    private fun buildRetrofit(url: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private var retrofit: Retrofit = buildRetrofit(baseUrl)

    var service: MovieApiService = retrofit.create(MovieApiService::class.java)
        private set

    fun getAbsoluteUrl(relativeOrAbsolute: String): String {
        if (relativeOrAbsolute.startsWith("http://") || relativeOrAbsolute.startsWith("https://")) {
            return relativeOrAbsolute
        }
        val cleanBase = baseUrl.trimEnd('/')
        val cleanPath = relativeOrAbsolute.trimStart('/')
        return "$cleanBase/$cleanPath"
    }
}
