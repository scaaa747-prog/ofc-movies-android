package com.ofc.movies.data.api

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

class MovieBoxAuthInterceptor : Interceptor {

    private val tokenRef = AtomicReference<String?>(null)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Ensure we have a token or attempt bootstrap
        val token = tokenRef.get() ?: bootstrapToken(chain)

        val signedRequest = signRequest(originalRequest, token)
        var response = chain.proceed(signedRequest)

        if (response.code == 401) {
            response.close()
            // Token expired; force refresh bootstrap
            val refreshedToken = bootstrapToken(chain)
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
    private fun bootstrapToken(chain: Interceptor.Chain): String? {
        val current = tokenRef.get()
        // If already set by another thread while waiting for lock, return it
        // (unless we are refreshing)

        val bootstrapUrl = "${MovieBoxSigner.BASE_URL}/wefeed-mobile-bff/tab-operating?page=1&tabId=0&version="
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

        return try {
            val resp = chain.proceed(bootstrapReq)
            val xUser = resp.header("x-user") ?: resp.header("X-User")
            resp.close()

            if (!xUser.isNullOrEmpty()) {
                val json = JSONObject(xUser)
                val token = json.optString("token", "")
                if (token.isNotEmpty()) {
                    tokenRef.set(token)
                    token
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
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
