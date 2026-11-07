package com.snaptube.clone.core.extractor

import com.snaptube.clone.core.common.AppResult
import com.snaptube.clone.core.model.MediaInfo
import com.snaptube.clone.core.model.SearchPage

/**
 * Contract implemented by every site-specific extraction backend.
 *
 * Implementations are pure network/parse code: no Android UI, no persistence.
 * They must never attempt to bypass DRM or authentication on protected content.
 */
interface MediaExtractor {

    /** Stable identifier, e.g. "youtube", "instagram", "generic". */
    val name: String

    /** True when [url] is handled by this extractor. */
    fun canHandle(url: String): Boolean

    /** Resolve full metadata and the list of downloadable formats for [url]. */
    suspend fun extract(url: String): AppResult<MediaInfo>
}

/**
 * Optional capability for extractors that can also drive search / browsing.
 * Not every site supports this (e.g. the generic HTML extractor cannot).
 */
interface SearchableExtractor {
    suspend fun search(query: String, pageToken: String? = null): AppResult<SearchPage>
}

/**
 * Optional capability for extractors that expose trending/feed content used by
 * the home screen.
 */
interface BrowsableExtractor {
    suspend fun trending(region: String = "US", pageToken: String? = null): AppResult<SearchPage>
    suspend fun related(videoId: String, pageToken: String? = null): AppResult<SearchPage>
}
