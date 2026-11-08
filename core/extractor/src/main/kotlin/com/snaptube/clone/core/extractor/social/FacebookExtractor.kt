package com.snaptube.clone.core.extractor.social

import kotlinx.serialization.json.JsonArray

class FacebookExtractor : SocialPageExtractor() {

    override val name = "facebook"

    override fun canHandle(url: String): Boolean {
        return runCatching {
            val host = java.net.URI(url).host?.lowercase() ?: return false

            host == "facebook.com" ||
                host.endsWith(".facebook.com") ||
                host == "fb.watch" ||
                host == "fb.com" ||
                host.endsWith(".fb.com")
        }.getOrDefault(false)
    }

    override fun canonicalUrl(url: String): String = url

    override fun findVideoUrl(
        payload: JsonArray?,
        html: String
    ): String? {

        val keys = listOf(
            "browser_native_hd_url",
            "browser_native_sd_url",
            "playable_url_quality_hd",
            "playable_url",
            "hd_src",
            "sd_src"
        )

        for (key in keys) {
            val pattern = Regex(
                """"${Regex.escape(key)}"\s*:\s*"([^"]+)""""
            )

            val match = pattern.find(html) ?: continue
            val candidate = unescape(match.groupValues[1])

            if (candidate.startsWith("https://")) {
                return candidate
            }
        }

        return meta(html, "og:video")
            ?: meta(html, "og:video:url")
    }

    override fun findTitle(
        payload: JsonArray?,
        html: String
    ): String =
        meta(html, "og:title") ?: "Facebook video"
}
