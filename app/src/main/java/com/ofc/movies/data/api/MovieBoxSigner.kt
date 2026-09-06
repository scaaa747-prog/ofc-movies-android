package com.ofc.movies.data.api

import android.net.Uri
import android.util.Base64
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

object MovieBoxSigner {
    const val BASE_URL = "https://api6.aoneroom.com"
    val FALLBACK_URLS = listOf("https://api5.aoneroom.com", "https://api4.aoneroom.com")

    const val SECRET_KEY_DEFAULT = "76iRl07s0xSN9jqmEWAt79EBJZulIQIsV64FZr2O"
    const val VERSION_CODE = 50020045

    val ANDROID_USER_AGENT =
        "com.community.oneroom/$VERSION_CODE (Linux; U; Android 13; en_US; 22101316G; Build/TQ2A.230405.003; Cronet/135.0.7012.3)"

    val DEVICE_ID: String = UUID.randomUUID().toString().replace("-", "")
    val GAID: String = UUID.randomUUID().toString()

    val CLIENT_INFO_JSON: String = JSONObject().apply {
        put("package_name", "com.community.oneroom")
        put("version_name", "3.0.03.0529.03")
        put("version_code", VERSION_CODE)
        put("os", "android")
        put("os_version", "13")
        put("install_ch", "ps")
        put("device_id", DEVICE_ID)
        put("install_store", "ps")
        put("gaid", GAID)
        put("brand", "Redmi")
        put("model", "22101316G")
        put("system_language", "en")
        put("net", "NETWORK_WIFI")
        put("region", "US")
        put("timezone", "America/New_York")
        put("sp_code", "40401")
        put("X-Play-Mode", "2")
    }.toString()

    fun md5Hex(data: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun generateXClientToken(ts: Long): String {
        val reversedTs = ts.toString().reversed()
        val h = md5Hex(reversedTs.toByteArray(Charsets.UTF_8))
        return "$ts,$h"
    }

    fun sortedQueryString(urlStr: String): String {
        val query = try {
            val qIdx = urlStr.indexOf('?')
            if (qIdx != -1) urlStr.substring(qIdx + 1) else null
        } catch (e: Exception) {
            null
        } ?: return ""

        val pairs = query.split("&").filter { it.isNotEmpty() }.map { param ->
            val idx = param.indexOf('=')
            if (idx >= 0) {
                val key = try { URLDecoder.decode(param.substring(0, idx), "UTF-8") } catch (e: Exception) { param.substring(0, idx) }
                val value = try { URLDecoder.decode(param.substring(idx + 1), "UTF-8") } catch (e: Exception) { param.substring(idx + 1) }
                key to value
            } else {
                val key = try { URLDecoder.decode(param, "UTF-8") } catch (e: Exception) { param }
                key to ""
            }
        }.sortedBy { it.first }

        return pairs.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
    }

    fun generateXTrSignature(
        method: String,
        accept: String?,
        contentType: String?,
        urlStr: String,
        bodyStr: String?,
        ts: Long
    ): String {
        val path = try {
            val withoutProto = if (urlStr.contains("://")) urlStr.substringAfter("://") else urlStr
            val firstSlash = withoutProto.indexOf('/')
            if (firstSlash != -1) {
                val p = withoutProto.substring(firstSlash)
                val qIdx = p.indexOf('?')
                if (qIdx != -1) p.substring(0, qIdx) else p
            } else "/"
        } catch (e: Exception) {
            "/"
        }

        val query = sortedQueryString(urlStr)
        val canonicalUrl = if (query.isNotEmpty()) "$path?$query" else path

        var bodyHash = ""
        var bodyLength = ""
        if (!bodyStr.isNullOrEmpty()) {
            val bodyBytes = bodyStr.toByteArray(Charsets.UTF_8)
            val chunkLen = if (bodyBytes.size > 4096) 4096 else bodyBytes.size
            val chunk = bodyBytes.copyOfRange(0, chunkLen)
            bodyHash = md5Hex(chunk)
            bodyLength = bodyBytes.size.toString()
        }

        val canonicalParts = listOf(
            method.uppercase(),
            accept ?: "",
            contentType ?: "",
            bodyLength,
            ts.toString(),
            bodyHash,
            canonicalUrl
        )
        val canonicalString = canonicalParts.joinToString("\n")
        val secretBytes = Base64.decode(SECRET_KEY_DEFAULT, Base64.DEFAULT)
        val mac = Mac.getInstance("HmacMD5")
        mac.init(SecretKeySpec(secretBytes, "HmacMD5"))
        val signedBytes = mac.doFinal(canonicalString.toByteArray(Charsets.UTF_8))
        val b64Mac = Base64.encodeToString(signedBytes, Base64.NO_WRAP)
        return "$ts|2|$b64Mac"
    }

    fun isFakeClipUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase()
        return lower.contains("9a0461bc39da389663bf3dbb17091d3f") ||
                lower.contains("b164fbfb4347792950bdfbfb563d39d9")
    }

    fun extractBaseDashUrl(cookie: String): String? {
        if (!cookie.contains("CloudFront-Policy=")) return null
        return try {
            val policyPart = cookie.substringAfter("CloudFront-Policy=").substringBefore(";")
                .replace("\n", "")
                .replace("\r", "")
                .trim()
            val norm = policyPart
                .replace('-', '+')
                .replace('_', '/')
                .replace('~', '=')
            val paddingNeeded = (4 - (norm.length % 4)) % 4
            val padded = norm + "=".repeat(paddingNeeded)
            val decoded = Base64.decode(padded, Base64.DEFAULT)
            val rawStr = String(decoded, Charsets.UTF_8)
            val lastBrace = rawStr.lastIndexOf('}')
            if (lastBrace != -1) {
                val cleanJson = rawStr.substring(0, lastBrace + 1)
                val json = JSONObject(cleanJson)
                val statement = json.getJSONArray("Statement").getJSONObject(0)
                val resource = statement.getString("Resource")
                resource.replace("/*", "")
            } else {
                val regex = Regex("""(https://[^\s"';]+)/\*""")
                regex.find(rawStr)?.groupValues?.get(1)
            }
        } catch (e: Exception) {
            null
        }
    }
}
