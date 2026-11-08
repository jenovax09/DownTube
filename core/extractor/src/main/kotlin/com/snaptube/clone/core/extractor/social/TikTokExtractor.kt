package com.snaptube.clone.core.extractor.social

import kotlinx.serialization.json.JsonArray

class TikTokExtractor : SocialPageExtractor() {

    override val name = "tiktok"

    override fun canHandle(url: String): Boolean {
        return runCatching {
            val host = java.net.URI(url).host?.lowercase() ?: return false
            host == "tiktok.com" ||
                host.endsWith(".tiktok.com")
        }.getOrDefault(false)
    }

    override fun canonicalUrl(url: String): String =
        url.substringBefore('?')

    override fun findVideoUrl(
        payload: JsonArray?,
        html: String
    ): String? {

        val patterns = listOf(
            Regex(""""playAddr"\s*:\s*"([^"]+)""""),
            Regex(""""downloadAddr"\s*:\s*"([^"]+)""""),
            Regex(""""contentUrl"\s*:\s*"([^"]+)""""),
            Regex(
                """<meta[^>]+property=["']og:video["'][^>]+content=["']([^"']+)["']""",
                RegexOption.IGNORE_CASE
            )
        )

        for (pattern in patterns) {
            val match = pattern.find(html) ?: continue
            val candidate = unescape(match.groupValues[1])

            if (candidate.startsWith("https://")) {
                return candidate
            }
        }

        return null
    }

    override fun findTitle(
        payload: JsonArray?,
        html: String
    ): String =
        meta(html, "og:title") ?: "TikTok video"

    override fun findAuthor(
        payload: JsonArray?,
        html: String
    ): String {
        return Regex(
            """"nickname"\s*:\s*"([^"]+)""""
        ).find(html)?.groupValues?.get(1).orEmpty()
    }

    override fun findDurationMs(
        payload: JsonArray?,
        html: String
    ): Long {
        val seconds = Regex(
            """"duration"\s*:\s*(\d+)"""
        ).find(html)?.groupValues?.get(1)?.toLongOrNull()

        return (seconds ?: 0L) * 1000L
    }
}
