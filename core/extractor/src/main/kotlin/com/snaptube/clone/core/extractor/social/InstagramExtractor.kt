package com.snaptube.clone.core.extractor.social

import kotlinx.serialization.json.JsonArray

class InstagramExtractor : SocialPageExtractor() {

    override val name = "instagram"

    override fun canHandle(url: String): Boolean {
        return runCatching {
            val host = java.net.URI(url).host?.lowercase() ?: return false
            host == "instagram.com" ||
                host.endsWith(".instagram.com") ||
                host == "instagr.am" ||
                host.endsWith(".instagr.am")
        }.getOrDefault(false)
    }

    override fun canonicalUrl(url: String): String =
        url.substringBefore('?')

    override fun findVideoUrl(
        payload: JsonArray?,
        html: String
    ): String? {

        val patterns = listOf(
            Regex(""""video_url"\s*:\s*"([^"]+)""""),
            Regex(""""contentUrl"\s*:\s*"([^"]+)""""),
            Regex(
                """<meta[^>]+property=["']og:video["'][^>]+content=["']([^"']+)["']""",
                RegexOption.IGNORE_CASE
            )
        )

        for (pattern in patterns) {
            val match = pattern.find(html) ?: continue
            val candidate = unescape(match.groupValues[1])

            if (candidate.startsWith("https://") &&
                candidate.contains(".mp4")
            ) {
                return candidate
            }
        }

        return null
    }

    override fun findAuthor(
        payload: JsonArray?,
        html: String
    ): String {
        return Regex(
            """"username"\s*:\s*"([^"]+)""""
        ).find(html)?.groupValues?.get(1).orEmpty()
    }

    override fun findTitle(
        payload: JsonArray?,
        html: String
    ): String =
        meta(html, "og:title") ?: "Instagram video"
}
