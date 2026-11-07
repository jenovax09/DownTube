package com.snaptube.clone.core.extractor.social

import kotlinx.serialization.json.JsonArray

/**
 * TikTok videos. TikTok embeds a JSON blob under the
 * "SIGI_STATE"/"__UNIVERSAL_DATA_FOR_REHYDRATION__" script tags which contains
 * playAddrs. We surface the no-watermark play address when publicly present.
 */
class TikTokExtractor : SocialPageExtractor() {

    override val name: String = "tiktok"

    override fun canHandle(url: String): Boolean =
        url.contains("tiktok.com/")

    override fun canonicalUrl(url: String): String = url.substringBefore('?')

    override fun findVideoUrl(payload: JsonArray?, html: String): String? {
        val keys = listOf("playAddr", "downloadAddr", "playApi", "PlayAddr")
        for (key in keys) {
            Regex(""""$key"\s*:\s*"(https?:[^"]+)"""").find(html)?.let {
                return unescape(it.groupValues[1])
            }
        }
        Regex("""<meta[^>]+property=["']og:video["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.let { return unescape(it.groupValues[1]) }
        Regex(""""contentUrl"\s*:\s*"(https?:[^"]+)"""").find(html)?.let {
            return unescape(it.groupValues[1])
        }
        return null
    }

    override fun findTitle(payload: JsonArray?, html: String): String =
        meta(html, "og:title") ?: "TikTok video"

    override fun findAuthor(payload: JsonArray?): String =
        Regex(""""nickname"\s*:\s*"([^"]+)"""").find("")?.groupValues?.get(1).orEmpty()

    override fun findDurationMs(payload: JsonArray?): Long =
        Regex(""""duration"\s*:\s*(\d+)""").find("")?.groupValues?.get(1)?.toLongOrNull()?.times(1000) ?: 0L
}
