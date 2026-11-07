package com.snaptube.clone.core.model

/** A locally stored media file surfaced by the MediaStore scanner. */
data class LocalMedia(
    val id: Long,
    val path: String,
    val title: String,
    val mediaType: MediaType,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val mimeType: String = "",
    val dateAdded: Long = 0L,
    val thumbnailPath: String = "",
    val album: String = "",
    val artist: String = ""
)

/** A user-defined or default playlist. */
data class Playlist(
    val id: Long = 0L,
    val name: String,
    val itemCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)

/** A search history entry. */
data class SearchHistoryItem(
    val query: String,
    val timestamp: Long
)

/** How the app should react when a URL is shared/pasted into it. */
enum class QualityPreference {
    ALWAYS_ASK,
    BEST,
    MEDIUM,
    LOW
}
