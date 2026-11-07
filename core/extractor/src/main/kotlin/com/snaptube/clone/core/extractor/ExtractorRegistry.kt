package com.snaptube.clone.core.extractor

import com.snaptube.clone.core.common.AppResult
import com.snaptube.clone.core.extractor.generic.GenericHtmlExtractor
import com.snaptube.clone.core.extractor.social.FacebookExtractor
import com.snaptube.clone.core.extractor.social.InstagramExtractor
import com.snaptube.clone.core.extractor.social.TikTokExtractor
import com.snaptube.clone.core.extractor.social.TwitterExtractor
import com.snaptube.clone.core.extractor.youtube.YouTubeExtractor
import com.snaptube.clone.core.model.MediaInfo

/**
 * Routes a URL to the most specific extractor that can handle it, falling back
 * to the generic HTML extractor last.
 *
 * The list mirrors the layered strategy of the analysed application: dedicated
 * handlers for the major platforms, then a catch-all for everything else.
 */
class ExtractorRegistry(
    extractors: List<MediaExtractor>? = null
) {

    private val ordered: List<MediaExtractor> = (extractors ?: defaultExtractors())
        .sortedByDescending { priority(it.name) }

    fun extractorFor(url: String): MediaExtractor =
        ordered.firstOrNull { it.name != "generic" && it.canHandle(url) }
            ?: ordered.first { it.name == "generic" }

    suspend fun extract(url: String): AppResult<MediaInfo> = extractorFor(url).extract(url)

    fun all(): List<MediaExtractor> = ordered

    fun searchable(): List<SearchableExtractor> = ordered.filterIsInstance<SearchableExtractor>()

    private fun priority(name: String): Int = when (name) {
        "youtube" -> 100
        "instagram", "tiktok", "facebook", "twitter" -> 80
        "generic" -> 0
        else -> 50
    }

    companion object {
        fun defaultExtractors(): List<MediaExtractor> = listOf(
            YouTubeExtractor(),
            InstagramExtractor(),
            TikTokExtractor(),
            FacebookExtractor(),
            TwitterExtractor(),
            GenericHtmlExtractor()
        )
    }
}
