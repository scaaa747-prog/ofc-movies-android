package com.ofc.movies.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class MovieBoxAuthInterceptor : Interceptor {

    private val tokenRef = AtomicReference<String?>(null)

    // Dedicated independent client for side-channel bootstrap requests.
    // This guarantees chain.proceed is NEVER called multiple times on the same chain!
    private val bootstrapClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1. Ensure token exists (bootstrap if null)
        var token = tokenRef.get()
        if (token.isNullOrEmpty()) {
            token = bootstrapToken()
        }

        // 2. Sign and send the request
        val signedRequest = signRequest(originalRequest, token)
        var response = chain.proceed(signedRequest)

        // 3. If 401 Unauthorized, refresh bootstrap token and retry once
        if (response.code == 401) {
            response.close()
            val refreshedToken = bootstrapToken()
            val retryRequest = signRequest(originalRequest, refreshedToken)
            response = chain.proceed(retryRequest)
        }

        return response
    }

    private fun signRequest(request: Request, token: String?): Request {
        val ts = System.currentTimeMillis()
        val urlStr = request.url.toString()
        val method = request.method
        val bodyStr = requestBodyToString(request.body)

        val clientToken = MovieBoxSigner.generateXClientToken(ts)
        val signature = MovieBoxSigner.generateXTrSignature(
            method = method,
            accept = "application/json",
            contentType = "application/json",
            urlStr = urlStr,
            bodyStr = bodyStr,
            ts = ts
        )

        val builder = request.newBuilder()
            .header("User-Agent", MovieBoxSigner.ANDROID_USER_AGENT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Connection", "keep-alive")
            .header("X-Client-Info", MovieBoxSigner.CLIENT_INFO_JSON)
            .header("X-Client-Status", "0")
            .header("X-Client-Token", clientToken)
            .header("x-tr-signature", signature)

        if (!token.isNullOrEmpty()) {
            builder.header("Authorization", "Bearer $token")
        }

        return builder.build()
    }

    @Synchronized
    private fun bootstrapToken(): String? {
        val hosts = listOf(MovieBoxSigner.BASE_URL) + MovieBoxSigner.FALLBACK_URLS
        for (base in hosts) {
            val bootstrapUrl = "$base/wefeed-mobile-bff/tab-operating?page=1&tabId=0&version="
            val ts = System.currentTimeMillis()
            val clientToken = MovieBoxSigner.generateXClientToken(ts)
            val signature = MovieBoxSigner.generateXTrSignature(
                method = "GET",
                accept = "application/json",
                contentType = "application/json",
                urlStr = bootstrapUrl,
                bodyStr = null,
                ts = ts
            )

            val bootstrapReq = Request.Builder()
                .url(bootstrapUrl)
                .header("User-Agent", MovieBoxSigner.ANDROID_USER_AGENT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Connection", "keep-alive")
                .header("X-Client-Info", MovieBoxSigner.CLIENT_INFO_JSON)
                .header("X-Client-Status", "0")
                .header("X-Client-Token", clientToken)
                .header("x-tr-signature", signature)
                .build()

            try {
                val resp = bootstrapClient.newCall(bootstrapReq).execute()
                val xUser = resp.header("x-user") ?: resp.header("X-User")
                resp.close()

                if (!xUser.isNullOrEmpty()) {
                    val json = JSONObject(xUser)
                    val token = json.optString("token", "")
                    if (token.isNotEmpty()) {
                        tokenRef.set(token)
                        return token
                    }
                }
            } catch (e: Exception) {
                // Try fallback host
                continue
            }
        }
        return null
    }

    private fun requestBodyToString(body: RequestBody?): String? {
        if (body == null) return null
        return try {
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: Exception) {
            null
        }
    }
}
