package com.snaptube.clone.core.extractor.social

import kotlinx.serialization.json.JsonArray

/**
 * Twitter/X public videos. Reads the "variants" array of the public
 * tweet JSON, preferring the highest-bitrate MP4.
 */
class TwitterExtractor : SocialPageExtractor() {

    override val name: String = "twitter"

    override fun canHandle(url: String): Boolean =
        url.contains("twitter.com/") || url.contains("//x.com/")

    override fun findVideoUrl(payload: JsonArray?, html: String): String? {
        val variants = Regex(""""variants"\s*:\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1) ?: return ogVideo(html)
        val mp4s = Regex("""https?:[^"]+\.mp4""").findAll(variants).map { it.value }.toList()
        if (mp4s.isEmpty()) return ogVideo(html)
        // Highest bitrate is usually last because Twitter orders variants ascending.
        return unescape(mp4s.last())
    }

    private fun ogVideo(html: String): String? =
        Regex("""<meta[^>]+property=["']og:video(?::url)?["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.let { unescape(it.groupValues[1]) }

    override fun findTitle(payload: JsonArray?, html: String): String =
        meta(html, "og:title") ?: "X video"
}
