package com.snaptube.clone.core.extractor.social

import kotlinx.serialization.json.JsonArray

class TwitterExtractor : SocialPageExtractor() {

    override val name = "twitter"

    override fun canHandle(url: String): Boolean {
        return runCatching {
            val host = java.net.URI(url).host?.lowercase() ?: return false

            host == "x.com" ||
                host.endsWith(".x.com") ||
                host == "twitter.com" ||
                host.endsWith(".twitter.com")
        }.getOrDefault(false)
    }

    override fun findVideoUrl(
        payload: JsonArray?,
        html: String
    ): String? {

        val variants = Regex(
            """"variants"\s*:\s*\[(.*?)]""",
            RegexOption.DOT_MATCHES_ALL
        ).find(html)?.groupValues?.get(1)

        if (variants != null) {
            val mp4s = Regex(
                """"url"\s*:\s*"([^"]+\.mp4[^"]*)""""
            ).findAll(variants)
                .map { unescape(it.groupValues[1]) }
                .toList()

            if (mp4s.isNotEmpty()) {
                return mp4s.last()
            }
        }

        return meta(html, "og:video")
            ?: meta(html, "og:video:url")
    }

    override fun findTitle(
        payload: JsonArray?,
        html: String
    ): String =
        meta(html, "og:title") ?: "X video"
}
