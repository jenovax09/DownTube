package com.snaptube.clone.core.extractor.generic

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
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URI

/**
 * Extractor for pages that expose HTML5 <video>/<source> tags, Open Graph video
 * metadata, or direct links to a media file.
 *
 * This is the catch-all backend: it never bypasses paywalls or DRM and simply
 * surfaces whatever the page already serves publicly.
 */
class GenericHtmlExtractor : MediaExtractor {

    override val name: String = "generic"

    override fun canHandle(url: String): Boolean = url.startsWith("http")

    override suspend fun extract(url: String): AppResult<MediaInfo> = withContext(Dispatchers.IO) {
        runCatchingResult {
            val direct = extractDirect(url)
            if (direct != null) return@runCatchingResult direct

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", HttpClient.USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val body = HttpClient.default.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}")
                }
                response.body?.string().orEmpty()
            }
            val document = Jsoup.parse(body, url)
            val baseUri = document.baseUri().ifEmpty { url }

            val formats = mutableListOf<DownloadFormat>()

            document.select("video[src]").forEach { video ->
                video.attr("src").takeIf { it.isNotBlank() }?.let { src ->
                    formats += format(absolute(baseUri, src), "Direct video")
                }
            }
            document.select("video source[src]").forEach { source ->
                source.attr("src").takeIf { it.isNotBlank() }?.let { src ->
                    formats += format(absolute(baseUri, src), "Source")
                }
            }
            document.select("meta[property=og:video], meta[property=og:video:secure_url], meta[name=twitter:player:stream]")
                .forEach { meta ->
                    meta.attr("content").takeIf { it.isNotBlank() }?.let { src ->
                        formats += format(absolute(baseUri, src), "OpenGraph")
                    }
                }

            val title = document.selectFirst("meta[property=og:title]")?.attr("content")
                ?: document.title().ifEmpty { "Media" }
            val thumbnail = document.selectFirst("meta[property=og:image]")?.attr("content").orEmpty()
            val description = document.selectFirst("meta[property=og:description]")?.attr("content").orEmpty()

            if (formats.isEmpty()) {
                val fallbackTitle = document.title().ifEmpty { "Unsupported link" }
                return@runCatchingResult MediaInfo(
                    id = url,
                    title = fallbackTitle,
                    description = description,
                    thumbnailUrl = thumbnail,
                    webpageUrl = url,
                    source = name,
                    formats = emptyList(),
                    isRestricted = true,
                    restrictionReason = "No downloadable media found on this page."
                )
            }

            MediaInfo(
                id = url,
                title = title,
                description = description,
                thumbnailUrl = thumbnail,
                webpageUrl = url,
                source = name,
                formats = formats.distinctBy { it.url }.mapIndexed { i, f -> f.copy(id = "FMT_${i + 1}_${f.ext.uppercase()}") }
            )
        }
    }

    private fun extractDirect(url: String): MediaInfo? {
        val path = runCatching { URI(url).path ?: "" }.getOrDefault("")
        val ext = path.substringAfterLast('.', "").lowercase()
        if (ext !in MEDIA_EXTENSIONS) return null
        val isAudio = ext in AUDIO_EXTENSIONS
        val title = path.substringAfterLast('/').ifEmpty { "media.$ext" }
        val container = when (ext) {
            "mp4", "m4v", "mov" -> ContainerType.MP4
            "webm" -> ContainerType.WEBM
            "mp3" -> ContainerType.MP3
            "m4a", "aac" -> ContainerType.M4A
            "3gp" -> ContainerType.THREE_GP
            else -> ContainerType.OTHER
        }
        val fmt = DownloadFormat(
            id = "DIRECT_${ext.uppercase()}",
            label = title,
            container = container,
            mediaType = if (isAudio) MediaType.AUDIO else MediaType.VIDEO,
            url = url,
            ext = ext,
            hasVideo = !isAudio,
            hasAudio = true
        )
        return MediaInfo(
            id = url,
            title = title,
            webpageUrl = url,
            source = name,
            formats = listOf(fmt)
        )
    }

    private fun format(url: String, label: String): DownloadFormat {
        val ext = url.substringBefore('?').substringAfterLast('.', "mp4").lowercase()
        val container = when (ext) {
            "webm" -> ContainerType.WEBM
            "mp4", "m4v", "mov" -> ContainerType.MP4
            "mp3" -> ContainerType.MP3
            "m4a" -> ContainerType.M4A
            "m3u8", "mpd" -> if (ext == "m3u8") ContainerType.HLS else ContainerType.DASH
            else -> ContainerType.MP4
        }
        val isAudio = container == ContainerType.MP3 || container == ContainerType.M4A
        return DownloadFormat(
            id = ext.uppercase(),
            label = label,
            container = container,
            mediaType = if (isAudio) MediaType.AUDIO else MediaType.VIDEO,
            url = url,
            ext = ext,
            hasVideo = !isAudio,
            hasAudio = true
        )
    }

    private fun absolute(base: String, ref: String): String =
        runCatching { URI(base).resolve(ref).toString() }.getOrDefault(ref)

    companion object {
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg")
        private val MEDIA_EXTENSIONS = AUDIO_EXTENSIONS + setOf(
            "mp4", "webm", "m4v", "mov", "mkv", "3gp", "m3u8", "mpd"
        )
    }
}
