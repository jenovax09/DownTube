package com.snaptube.clone.core.extractor.youtube

import com.snaptube.clone.core.extractor.HttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Thin client over YouTube's public InnerTube endpoints.
 *
 * Only the officially public API surface is used (the same requests the web and
 * Android clients make). No authentication bypass and no DRM circumvention is
 * performed here.
 */
class InnerTubeClient(
    private val client: OkHttpClient = HttpClient.default
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private val androidClient = buildClient(
        name = "ANDROID", version = "19.09.37", sdk = "30"
    )
    private val webClient = buildClient(
        name = "WEB", version = "2.20240304.00.00", sdk = null
    )

    /** Player response (streamingData + videoDetails) for a video id. */
    fun player(videoId: String, client: JsonObject = androidClient): JsonObject = post(
        "player",
        buildJsonObject {
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
            put("context", buildJsonObject { put("client", client) })
        }
    )

    fun webPlayer(videoId: String): JsonObject = player(videoId, webClient)

    /** General keyword search. */
    fun search(query: String, params: String? = null): JsonObject = post(
        "search",
        buildJsonObject {
            put("context", buildJsonObject { put("client", webClient) })
            put("query", query)
            if (params != null) put("params", params)
        }
    )

    /** Browse a channel, playlist, feed or continuation token. */
    fun browse(browseId: String? = null, continuation: String? = null, params: String? = null): JsonObject =
        post(
            "browse",
            buildJsonObject {
                put("context", buildJsonObject { put("client", webClient) })
                if (browseId != null) put("browseId", browseId)
                if (continuation != null) put("continuation", continuation)
                if (params != null) put("params", params)
            }
        )

    /** Related videos / comments via the "next" endpoint. */
    fun next(videoId: String, continuation: String? = null): JsonObject = post(
        "next",
        buildJsonObject {
            put("context", buildJsonObject { put("client", webClient) })
            put("videoId", videoId)
            if (continuation != null) put("continuation", continuation)
        }
    )

    // ---------------------------------------------------------------------

    
private fun post(endpoint: String, body: JsonObject): JsonObject {

    val contextClient = body["context"]
        ?.jsonObject
        ?.get("client")
        ?.jsonObject

    val clientName = contextClient
        ?.get("clientName")
        ?.toString()
        ?.trim('"')
        ?: "WEB"

    val clientVersion = contextClient
        ?.get("clientVersion")
        ?.toString()
        ?.trim('"')
        ?: "unknown"

    val request = Request.Builder()
        .url("$BASE_URL/$endpoint?key=$API_KEY&prettyPrint=false")
        .header("User-Agent", HttpClient.USER_AGENT)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Origin", "https://www.youtube.com")
        .header("Referer", "https://www.youtube.com/")
        .header("X-YouTube-Client-Name", clientName)
        .header("X-YouTube-Client-Version", clientVersion)
        .post(
            body.toString().toRequestBody(
                "application/json".toMediaType()
            )
        )
        .build()

    client.newCall(request).execute().use { response ->

        val responseText = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            throw InnerTubeException(
                """
                YouTube API request failed.
                Endpoint: $endpoint
                HTTP: ${response.code}
                Client: $clientName
                Version: $clientVersion
                Response: ${responseText.take(1000)}
                """.trimIndent()
            )
        }

        return try {
            json.parseToJsonElement(responseText).jsonObject
        } catch (e: Exception) {
            throw InnerTubeException(
                "Invalid YouTube JSON response: ${e.message}"
            )
        }
    }
}


    private fun buildClient(name: String, version: String, sdk: String?): JsonObject = buildJsonObject {
        put("clientName", name)
        put("clientVersion", version)
        if (sdk != null) put("androidSdkVersion", sdk)
        put("hl", "en")
        put("gl", "US")
    }

    class InnerTubeException(message: String) : Exception(message)

    companion object {
        private const val BASE_URL = "https://www.youtube.com/youtubei/v1"
        private const val API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    }
}
