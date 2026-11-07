package com.snaptube.clone.core.extractor.social

import com.snaptube.clone.core.common.AppResult
import com.snaptube.clone.core.common.runCatchingResult
import com.snaptube.clone.core.extractor.HttpClient
import com.snaptube.clone.core.extractor.MediaExtractor
import com.snaptube.clone.core.model.ContainerType
import com.snaptube.clone.core.model.DownloadFormat
import com.snaptube.clone.core.model.MediaInfo
import com.snaptube.clone.core.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request

/**
 * Base class for social platforms whose public pages embed a JSON payload or
 * Open Graph video metadata.
 *
 * Subclasses only describe how to find the embedded JSON / canonical URL; the
 * generic extraction, format building and metadata handling live here.
 *
 * These extractors operate strictly on publicly accessible content. Private or
 * authenticated media is intentionally not supported.
 */
abstract class SocialPageExtractor : MediaExtractor {

    protected val json = Json { ignoreUnknownKeys = true; isLenient = true }

    protected open val mobileUserAgent: String = HttpClient.USER_AGENT

    protected open fun canonicalUrl(url: String): String = url

    override suspend fun extract(url: String): AppResult<MediaInfo> = withContext(Dispatchers.IO) {
        runCatchingResult {
            val target = canonicalUrl(url)
            val html = fetch(target)
            val payload = extractPayload(html)
            val videoUrl = findVideoUrl(payload, html)
                ?: throw IllegalStateException("No public video stream found for this $name link")

            val title = findTitle(payload, html).ifEmpty { "$name video" }
            val thumbnail = findThumbnail(payload, html)
            val durationMs = findDurationMs(payload)
            val author = findAuthor(payload)

            val format = DownloadFormat(
                id = "MP4_${name.uppercase()}",
                label = "MP4",
                container = ContainerType.MP4,
                mediaType = MediaType.VIDEO,
                url = videoUrl,
                ext = "mp4",
                hasVideo = true,
                hasAudio = true,
                headers = streamHeaders(target)
            )

            val formats = mutableListOf(format)
            findExtraAudio(payload)?.let { audioUrl ->
                formats += DownloadFormat(
                    id = "M4A_${name.uppercase()}",
                    label = "M4A audio",
                    container = ContainerType.M4A,
                    mediaType = MediaType.AUDIO,
                    url = audioUrl,
                    ext = "m4a",
                    hasVideo = false,
                    hasAudio = true,
                    headers = streamHeaders(target)
                )
            }

            MediaInfo(
                id = target,
                title = title,
                uploader = author,
                durationMs = durationMs,
                thumbnailUrl = thumbnail,
                webpageUrl = target,
                source = name,
                formats = formats
            )
        }
    }

    protected fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", mobileUserAgent)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        return HttpClient.default.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code} for $url")
            response.body?.string().orEmpty()
        }
    }

    protected open fun streamHeaders(referer: String): Map<String, String> =
        mapOf("User-Agent" to mobileUserAgent, "Referer" to referer)

    /** Locate and decode the embedded JSON payload, if the platform uses one. */
    protected open fun extractPayload(html: String): JsonArray? = null

    protected abstract fun findVideoUrl(payload: JsonArray?, html: String): String?

    protected open fun findExtraAudio(payload: JsonArray?): String? = null

    protected open fun findTitle(payload: JsonArray?, html: String): String =
        meta(html, "og:title") ?: meta(html, "twitter:title") ?: ""

    protected open fun findThumbnail(payload: JsonArray?, html: String): String =
        meta(html, "og:image") ?: meta(html, "twitter:image") ?: ""

    protected open fun findAuthor(payload: JsonArray?): String = ""

    protected open fun findDurationMs(payload: JsonArray?): Long = 0L

    protected fun meta(html: String, property: String): String? {
        val regex = Regex(
            """<meta[^>]+(?:property|name)=["']${Regex.escape(property)}["'][^>]+content=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE
        )
        regex.find(html)?.let { return it.groupValues[1] }
        val reversed = Regex(
            """<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["']${Regex.escape(property)}["']""",
            RegexOption.IGNORE_CASE
        )
        return reversed.find(html)?.groupValues?.get(1)
    }

    protected fun unescape(raw: String): String =
        raw.replace("\\u002F", "/")
            .replace("\\/", "/")
            .replace("\\u0026", "&")
            .replace("\\\"", "\"")
            .replace("&amp;", "&")
}
