package com.snaptube.clone.core.extractor.social

import kotlinx.serialization.json.JsonArray

/**
 * Instagram public posts/reels. Resolves the public "video_versions" or
 * "video_url" entry from the embedded GraphQL/JSON payload.
 */
class InstagramExtractor : SocialPageExtractor() {

    override val name: String = "instagram"

    override fun canHandle(url: String): Boolean =
        url.contains("instagram.com/") || url.contains("instagr.am/")

    override fun canonicalUrl(url: String): String = url.substringBefore('?')

    override fun findVideoUrl(payload: JsonArray?, html: String): String? {
        val videoVersions = Regex(""""video_versions"\s*:\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)
        if (videoVersions != null) {
            Regex("""https?:\\?/\\?/[^",]+\.mp4[^",]*""")
                .find(videoVersions)?.let { return unescape(it.value) }
        }
        Regex(""""video_url"\s*:\s*"(https?:\\?/\\?/[^"]+)"""")
            .find(html)?.let { return unescape(it.groupValues[1]) }
        Regex("""<meta[^>]+property=["']og:video["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.let { return unescape(it.groupValues[1]) }
        return null
    }

    override fun findAuthor(payload: JsonArray?): String =
        Regex(""""username"\s*:\s*"([^"]+)"""").find("")?.groupValues?.get(1).orEmpty()
}
