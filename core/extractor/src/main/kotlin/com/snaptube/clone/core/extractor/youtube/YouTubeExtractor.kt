package com.snaptube.clone.core.extractor.youtube

import com.snaptube.clone.core.common.AppResult
import com.snaptube.clone.core.common.runCatchingResult
import com.snaptube.clone.core.extractor.HttpClient
import com.snaptube.clone.core.extractor.MediaExtractor
import com.snaptube.clone.core.extractor.SearchableExtractor
import com.snaptube.clone.core.model.ContainerType
import com.snaptube.clone.core.model.DownloadFormat
import com.snaptube.clone.core.model.MediaInfo
import com.snaptube.clone.core.model.MediaType
import com.snaptube.clone.core.model.SearchPage
import com.snaptube.clone.core.model.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.net.URLDecoder

/**
 * Extracts downloadable formats and metadata from YouTube URLs.
 *
 * Strategy (matching the layered fallback used by the reference app):
 *  1. Fetch the ANDROID player response.
 *  2. If it lacks adaptive/progressive formats, fall back to the WEB client.
 *  3. Decipher signatures / throttling params from the player JS when required.
 */
class YouTubeExtractor(
    private val client: InnerTubeClient = InnerTubeClient()
) : MediaExtractor, SearchableExtractor {

    override val name: String = "youtube"

    @Volatile
    private var decipherer: SignatureDecipherer? = null

    override fun canHandle(url: String): Boolean = videoId(url) != null

    override suspend fun extract(url: String): AppResult<MediaInfo> = withContext(Dispatchers.IO) {
        runCatchingResult {
            val videoId = videoId(url) ?: throw ExtractionError("Not a recognised YouTube URL")

            val androidResult = runCatching {
    client.player(videoId)
}

val androidPlayer = androidResult.getOrNull()

val player = if (
    androidPlayer != null &&
    (
        !formatsOf(androidPlayer).isNullOrEmpty() ||
        !adaptiveOf(androidPlayer).isNullOrEmpty()
    )
) {
    androidPlayer
} else {
    val webResult = runCatching {
        client.webPlayer(videoId)
    }

    webResult.getOrElse { error ->
        throw ExtractionError(
            buildString {
                append("YouTube extraction failed.")
                append("\nAndroid: ")
                append(androidResult.exceptionOrNull()?.message ?: "No formats returned")
                append("\nWeb: ")
                append(error.message ?: "Unknown error")
            }
        )
    }
}
            if (playabilityBlocked(player)) {
                return@runCatchingResult restricted(player)
            }

            val details = player["videoDetails"]?.jsonObject
                ?: throw ExtractionError("Missing videoDetails")

            val thumbnail = details["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content.orEmpty()

            MediaInfo(
                id = videoId,
                title = details["title"]?.jsonPrimitive?.content.orEmpty(),
                uploader = details["author"]?.jsonPrimitive?.content.orEmpty(),
                description = (details["shortDescription"]?.jsonPrimitive?.content.orEmpty()).take(5000),
                durationMs = (details["lengthSeconds"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L) * 1000,
                thumbnailUrl = thumbnail,
                webpageUrl = "https://www.youtube.com/watch?v=$videoId",
                source = name,
                viewCount = details["viewCount"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                formats = buildFormats(player, videoId),
                subtitles = buildSubtitles(player),
                isLive = details["isLiveContent"]?.jsonPrimitive?.booleanOrNull == true
            )
        }
    }

    override suspend fun search(query: String, pageToken: String?): AppResult<SearchPage> =
        withContext(Dispatchers.IO) {
            runCatchingResult {
                val response = if (pageToken == null) client.search(query) else client.browse(continuation = pageToken)
                SearchPage(items = parseSearchResults(response), nextPageToken = findContinuation(response))
            }
        }

    // ---------------------------------------------------------------------

    private fun formatsOf(player: JsonObject): JsonArray? =
        player["streamingData"]?.jsonObject?.get("formats")?.jsonArray

    private fun adaptiveOf(player: JsonObject): JsonArray? =
        player["streamingData"]?.jsonObject?.get("adaptiveFormats")?.jsonArray

    private fun playabilityBlocked(player: JsonObject): Boolean {
        val status = player["playabilityStatus"]?.jsonObject?.get("status")?.jsonPrimitive?.content
        return status != null && status != "OK"
    }

    private fun restricted(player: JsonObject): MediaInfo {
        val reason = player["playabilityStatus"]?.jsonObject?.get("reason")
            ?.jsonPrimitive?.content ?: "Video unavailable"
        return MediaInfo(
            id = "",
            title = "",
            webpageUrl = "",
            source = name,
            isRestricted = true,
            restrictionReason = reason
        )
    }

    private fun buildFormats(player: JsonObject, videoId: String): List<DownloadFormat> {
        val streaming = player["streamingData"]?.jsonObject ?: return emptyList()
        val all: List<JsonElement> =
            (streaming["formats"]?.jsonArray?.toList().orEmpty()) +
                (streaming["adaptiveFormats"]?.jsonArray?.toList().orEmpty())

        val result = mutableListOf<DownloadFormat>()
        for (element in all) {
            val format = element.jsonObject
            val itag = format["itag"]?.jsonPrimitive?.content?.toIntOrNull() ?: -1
            val mime = format["mimeType"]?.jsonPrimitive?.content.orEmpty()
            val info = ItagTable.info(itag)
            val container = containerOf(mime, info)
            val mediaType = if (mime.startsWith("audio") || info?.mediaType == MediaType.AUDIO) {
                MediaType.AUDIO
            } else {
                MediaType.VIDEO
            }

            var resolvedUrl = format["url"]?.jsonPrimitive?.content
            if (resolvedUrl.isNullOrEmpty()) {
                val cipher = format["signatureCipher"]?.jsonPrimitive?.content
                    ?: format["cipher"]?.jsonPrimitive?.content
                    ?: continue
                val params = parseCipherParams(cipher)
                val plainUrl = params["url"] ?: continue
                val signature = params["s"]?.let { sig ->
                    val d = resolveDecipherer(videoId) ?: return@let null
                    d.decipher(sig)
                }
                resolvedUrl = if (signature != null) "$plainUrl&sig=$signature" else plainUrl
            }
            val url = resolvedUrl ?: continue

            val fixedUrl = applyNTransform(url, videoId)

            val width = format["width"]?.jsonPrimitive?.intOrNull ?: info?.width ?: 0
            val height = format["height"]?.jsonPrimitive?.intOrNull ?: info?.height ?: 0
            val fps = format["fps"]?.jsonPrimitive?.intOrNull ?: info?.fps ?: 0
            val bitrate = format["bitrate"]?.jsonPrimitive?.intOrNull?.div(1000) ?: info?.bitrate ?: 0
            val contentLength = format["contentLength"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            val codecs = codecsOf(mime)

            result += DownloadFormat(
                id = formatId(itag, container, mediaType, height, bitrate),
                label = labelFor(container, mediaType, height, bitrate),
                container = container,
                mediaType = mediaType,
                url = fixedUrl,
                ext = extFor(container),
                width = width,
                height = height,
                fps = fps,
                bitrate = bitrate,
                codec = if (mediaType == MediaType.VIDEO) codecs else "",
                audioCodec = if (mediaType == MediaType.AUDIO) codecs else "",
                contentLength = contentLength,
                hasVideo = mediaType == MediaType.VIDEO,
                hasAudio = mediaType == MediaType.AUDIO || mime.contains("audio"),
                itag = itag,
                headers = headersFor()
            )
        }
        return result.distinctBy { it.id }
    }

    private fun applyNTransform(url: String, videoId: String): String {
        val n = Regex("[?&]n=([^&]+)").find(url)?.groupValues?.get(1) ?: return url
        val d = resolveDecipherer(videoId) ?: return url
        if (!d.hasNTransform) return url
        return url.replace("&n=$n", "&n=${d.transformN(n)}")
    }

    private fun resolveDecipherer(videoId: String): SignatureDecipherer? {
        decipherer?.let { return it }
        return runCatching {
            val request = Request.Builder()
                .url("https://www.youtube.com/watch?v=$videoId")
                .header("User-Agent", HttpClient.USER_AGENT)
                .build()
            val html = HttpClient.default.newCall(request).execute().use { it.body?.string().orEmpty() }
            val jsPath = Regex("\"jsUrl\"\\s*:\\s*\"([^\"]+)\"").find(html)?.groupValues?.get(1)
                ?: Regex("/s/player/[^\"]+/player_ias[^\"]*base\\.js").find(html)?.value
                ?: return null
            val jsUrl = if (jsPath.startsWith("http")) jsPath else "https://www.youtube.com$jsPath"
            val js = HttpClient.default.newCall(
                Request.Builder().url(jsUrl).header("User-Agent", HttpClient.USER_AGENT).build()
            ).execute().use { it.body?.string().orEmpty() }
            SignatureDecipherer.from(js)?.also { decipherer = it }
        }.getOrNull()
    }

    private fun buildSubtitles(player: JsonObject): List<SubtitleTrack> {
        val tracks = mutableListOf<SubtitleTrack>()
        val captionTracks = player["captions"]?.jsonObject
            ?.get("playerCaptionsTracklistRenderer")?.jsonObject
            ?.get("captionTracks")?.jsonArray ?: return emptyList()
        for (element in captionTracks) {
            val track = element.jsonObject
            val url = track["baseUrl"]?.jsonPrimitive?.content ?: continue
            val lang = track["languageCode"]?.jsonPrimitive?.content ?: "und"
            val label = track["name"]?.jsonObject?.get("simpleText")?.jsonPrimitive?.content
                ?: track["name"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("text")?.jsonPrimitive?.content ?: lang
            val auto = track["kind"]?.jsonPrimitive?.content == "asr"
            tracks += SubtitleTrack(language = lang, label = label, url = url, isAutoGenerated = auto)
        }
        return tracks
    }

    private fun parseSearchResults(response: JsonObject): List<MediaInfo> {
        val items = mutableListOf<MediaInfo>()
        val sections = response["contents"]?.jsonObject
            ?.get("twoColumnSearchResultsRenderer")?.jsonObject
            ?.get("primaryContents")?.jsonObject
            ?.get("sectionListRenderer")?.jsonObject
            ?.get("contents")?.jsonArray ?: return emptyList()

        for (section in sections) {
            val contents = section.jsonObject["itemSectionRenderer"]?.jsonObject
                ?.get("contents")?.jsonArray ?: continue
            for (content in contents) {
                videoRendererToInfo(content.jsonObject)?.let { items += it }
            }
        }
        return items
    }

    private fun videoRendererToInfo(content: JsonObject): MediaInfo? {
        val video = content["videoRenderer"]?.jsonObject
            ?: content["compactVideoRenderer"]?.jsonObject
            ?: return null
        val id = video["videoId"]?.jsonPrimitive?.content ?: return null
        return MediaInfo(
            id = id,
            title = textOf(video["title"]).orEmpty(),
            uploader = textOf(video["ownerText"]) ?: textOf(video["shortBylineText"]) ?: "",
            durationMs = parseDuration(textOf(video["lengthText"]) ?: ""),
            thumbnailUrl = video["thumbnail"]?.jsonObject?.get("thumbnails")?.jsonArray
                ?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content.orEmpty(),
            webpageUrl = "https://www.youtube.com/watch?v=$id",
            source = name
        )
    }

    private fun textOf(element: JsonElement?): String? {
        val obj = element?.jsonObject ?: return null
        obj["simpleText"]?.jsonPrimitive?.content?.let { return it }
        val runs = obj["runs"]?.jsonArray ?: return null
        return runs.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content.orEmpty() }
            .ifEmpty { null }
    }

    private fun parseDuration(text: String): Long {
        val parts = text.split(":").mapNotNull { it.trim().toLongOrNull() }
        if (parts.isEmpty()) return 0L
        var seconds = 0L
        for (part in parts) seconds = seconds * 60 + part
        return seconds * 1000
    }

    private fun findContinuation(response: JsonObject): String? =
        Regex("\"continuationCommand\"\\s*:\\s*\\{\\s*\"token\"\\s*:\\s*\"([^\"]+)\"")
            .find(response.toString())?.groupValues?.get(1)

    private fun parseCipherParams(cipher: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (pair in cipher.split('&')) {
            val idx = pair.indexOf('=')
            if (idx <= 0) continue
            map[pair.substring(0, idx)] = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
        }
        return map
    }

    private fun codecsOf(mime: String): String =
        mime.substringAfter("codecs=\"", "").substringBefore("\"")

    private fun containerOf(mime: String, info: ItagTable.Info?): ContainerType = when {
        mime.contains("webm") -> ContainerType.WEBM
        mime.contains("mp4a") || mime.startsWith("audio/mp4") -> ContainerType.M4A
        mime.contains("audio/mpeg") -> ContainerType.MP3
        mime.contains("3gpp") -> ContainerType.THREE_GP
        mime.contains("mp4") -> ContainerType.MP4
        else -> info?.container ?: ContainerType.OTHER
    }

    private fun extFor(container: ContainerType): String = when (container) {
        ContainerType.MP4 -> "mp4"
        ContainerType.WEBM -> "webm"
        ContainerType.MP3 -> "mp3"
        ContainerType.M4A -> "m4a"
        ContainerType.THREE_GP -> "3gp"
        ContainerType.HLS -> "m3u8"
        ContainerType.DASH -> "mpd"
        ContainerType.OTHER -> "bin"
    }

    private fun labelFor(container: ContainerType, mediaType: MediaType, height: Int, bitrate: Int): String =
        if (mediaType == MediaType.AUDIO) "${container.name} $bitrate Kbps"
        else "${container.name} ${height}p"

    private fun formatId(itag: Int, container: ContainerType, mediaType: MediaType, height: Int, bitrate: Int): String =
        if (mediaType == MediaType.AUDIO) "${container.name}_${bitrate}K"
        else "${container.name}_${height}P" + if (itag > 0) "_$itag" else ""

    private fun headersFor(): Map<String, String> = mapOf("User-Agent" to HttpClient.USER_AGENT)

    fun videoId(url: String): String? {
        val patterns = listOf(
            Regex("""[?&]v=([A-Za-z0-9_-]{11})"""),
            Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),
            Regex("""/shorts/([A-Za-z0-9_-]{11})"""),
            Regex("""/embed/([A-Za-z0-9_-]{11})"""),
            Regex("""/live/([A-Za-z0-9_-]{11})"""),
            Regex("""^([A-Za-z0-9_-]{11})$""")
        )
        for (p in patterns) {
            val m = p.find(url) ?: continue
            return m.groupValues[1]
        }
        return null
    }
}

class ExtractionError(message: String) : Exception(message)
