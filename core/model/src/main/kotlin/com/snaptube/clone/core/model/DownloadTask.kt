package com.snaptube.clone.core.model

/** Lifecycle state for a download job. */
enum class DownloadStatus(val storageValue: Int) {
    QUEUED(0),
    PREPARING(1),
    DOWNLOADING(2),
    MUXING(3),
    PAUSED(4),
    COMPLETED(5),
    FAILED(6),
    CANCELED(7);

    val isTerminal: Boolean get() = this == COMPLETED || this == FAILED || this == CANCELED
    val isActive: Boolean get() = this == QUEUED || this == PREPARING || this == DOWNLOADING || this == MUXING

    companion object {
        fun fromStorage(value: Int): DownloadStatus =
            entries.firstOrNull { it.storageValue == value } ?: QUEUED
    }
}

/** A persisted download job as shown in the download manager UI. */
data class DownloadTask(
    val id: Long = 0L,
    val mediaId: String,
    val title: String,
    val formatId: String,
    val mediaType: MediaType,
    val source: String,
    val videoUrl: String,
    val audioUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val referrer: String,
    val coverUrl: String = "",
    val outputPath: String = "",
    val tempVideoPath: String = "",
    val tempAudioPath: String = "",
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val errorMessage: String? = null,
    val durationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isMuxed: Boolean = false
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}
