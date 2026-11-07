package com.snaptube.clone.core.extractor.social

import kotlinx.serialization.json.JsonArray

/**
 * Facebook public video pages. Prefers the "browser_native_hd_url" /
 * "browser_native_sd_url" / "playable_url" entries from the embedded page JSON.
 */
class FacebookExtractor : SocialPageExtractor() {

    override val name: String = "facebook"

    override fun canHandle(url: String): Boolean =
        url.contains("facebook.com/") || url.contains("fb.watch/") || url.contains("fb.com/")

    override fun canonicalUrl(url: String): String = url.substringBefore('?')

    override fun findVideoUrl(payload: JsonArray?, html: String): String? {
        val hdKeys = listOf("browser_native_hd_url", "playable_url_quality_hd", "hd_src")
        val sdKeys = listOf("browser_native_sd_url", "playable_url", "sd_src")
        for (key in hdKeys + sdKeys) {
            Regex(""""$key"\s*:\s*"(https?:(?:\\/|[^"])+)"""").find(html)?.let {
                return unescape(it.groupValues[1])
            }
        }
        Regex("""<meta[^>]+property=["']og:video(?::url)?["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.let { return unescape(it.groupValues[1]) }
        return null
    }

    override fun findTitle(payload: JsonArray?, html: String): String =
        meta(html, "og:title") ?: "Facebook video"
}
