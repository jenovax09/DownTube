package com.snaptube.clone.core.database.mapper

import com.snaptube.clone.core.database.entity.DownloadTaskEntity
import com.snaptube.clone.core.database.entity.HistoryEntity
import com.snaptube.clone.core.model.DownloadStatus
import com.snaptube.clone.core.model.DownloadTask
import com.snaptube.clone.core.model.MediaType

/** Converts between persisted entities and the domain layer. */

fun DownloadTaskEntity.toDomain(): DownloadTask = DownloadTask(
    id = id,
    mediaId = mediaId,
    title = title,
    formatId = formatId,
    mediaType = MediaType.fromStorage(mediaType),
    source = source,
    videoUrl = videoUrl,
    audioUrl = audioUrl,
    headers = decodeHeaders(headers),
    referrer = referrer,
    coverUrl = coverUrl,
    outputPath = outputPath,
    tempVideoPath = tempVideoPath,
    tempAudioPath = tempAudioPath,
    totalBytes = totalBytes,
    downloadedBytes = downloadedBytes,
    status = DownloadStatus.fromStorage(status),
    errorMessage = errorMessage,
    durationMs = durationMs,
    createdAt = createdAt,
    completedAt = completedAt,
    isMuxed = isMuxed == 1
)

fun DownloadTask.toEntity(): DownloadTaskEntity = DownloadTaskEntity(
    id = id,
    mediaId = mediaId,
    title = title,
    formatId = formatId,
    mediaType = mediaType.storageValue,
    source = source,
    videoUrl = videoUrl,
    audioUrl = audioUrl,
    headers = encodeHeaders(headers),
    referrer = referrer,
    coverUrl = coverUrl,
    outputPath = outputPath,
    tempVideoPath = tempVideoPath,
    tempAudioPath = tempAudioPath,
    totalBytes = totalBytes,
    downloadedBytes = downloadedBytes,
    status = status.storageValue,
    errorMessage = errorMessage,
    durationMs = durationMs,
    createdAt = createdAt,
    completedAt = completedAt,
    isMuxed = if (isMuxed) 1 else 0
)

/**
 * History rows reuse the task primary key so a completed task is mirrored into
 * `history` exactly once.
 */
fun DownloadTask.toHistoryEntity(): HistoryEntity = HistoryEntity(
    id = mediaId.ifEmpty { id.toString() },
    title = title,
    fileSize = totalBytes,
    mediaType = mediaType.storageValue,
    downloadTime = completedAt ?: System.currentTimeMillis(),
    downloadUrl = videoUrl,
    path = outputPath,
    referrer = referrer,
    format = formatId,
    cover = coverUrl,
    extra = "",
    isLock = 0,
    pluginMessage = "",
    duration = durationMs
)

private fun encodeHeaders(headers: Map<String, String>): String =
    headers.entries.joinToString(";") { "${it.key}=${it.value}" }

private fun decodeHeaders(raw: String): Map<String, String> {
    if (raw.isBlank()) return emptyMap()
    return raw.split(';').mapNotNull { pair ->
        val idx = pair.indexOf('=')
        if (idx <= 0) null else pair.substring(0, idx) to pair.substring(idx + 1)
    }.toMap()
}
