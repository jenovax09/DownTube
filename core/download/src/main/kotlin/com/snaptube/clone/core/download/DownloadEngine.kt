package com.snaptube.clone.core.download

import com.snaptube.clone.core.common.DispatcherProvider
import com.snaptube.clone.core.common.DefaultDispatcherProvider
import com.snaptube.clone.core.database.SnapTubeDatabase
import com.snaptube.clone.core.database.entity.DownloadTaskEntity
import com.snaptube.clone.core.database.mapper.toDomain
import com.snaptube.clone.core.database.mapper.toEntity
import com.snaptube.clone.core.database.mapper.toHistoryEntity
import com.snaptube.clone.core.download.media.AudioConverter
import com.snaptube.clone.core.download.media.MediaMuxer
import com.snaptube.clone.core.download.media.NoOpAudioConverter
import com.snaptube.clone.core.model.DownloadStatus
import com.snaptube.clone.core.model.DownloadTask
import com.snaptube.clone.core.model.MediaType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Live download progress exposed to the UI. */
data class DownloadProgress(
    val taskId: Long,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val bytesPerSecond: Long,
    val status: DownloadStatus,
    val errorMessage: String? = null
) {
    val percent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
}

/**
 * Owns the download pipeline: it dequeues persisted jobs, downloads the
 * video/audio streams, muxes them when required and records the finished file in
 * history.
 *
 * Concurrency is bounded by [maxConcurrent]; the queue itself is persisted in
 * Room so jobs survive process death.
 */
class DownloadEngine(
    private val context: android.content.Context,
    private val database: SnapTubeDatabase,
    private val storage: StorageManager,
    private val downloader: ResumableDownloader = ResumableDownloader(),
    private val audioConverter: AudioConverter = NoOpAudioConverter(),
    private val dispatcher: DispatcherProvider = DefaultDispatcherProvider(),
    private val maxConcurrent: Int = 3
) {

    private val queueMutex = Mutex()
    private var running = 0

    private val _progress = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
    val progress: StateFlow<Map<Long, DownloadProgress>> = _progress.asStateFlow()

    private val muxer by lazy { MediaMuxer(context.applicationContext) }

    suspend fun enqueue(task: DownloadTask): Long = withContext(dispatcher.io) {
        database.downloadTaskDao().insert(task.toEntity())
    }

    /** Processes all QUEUED jobs, honouring the concurrency limit. */
    suspend fun pump(): Unit = withContext(dispatcher.io) {
        while (true) {
            val next = queueMutex.withLock {
                val active = database.downloadTaskDao().activeTasks()
                if (running >= maxConcurrent) return@withLock null
                val candidate = active.firstOrNull { it.status == DownloadStatus.QUEUED.storageValue }
                    ?: return@withLock null
                running++
                candidate
            } ?: break

            try {
                process(next)
            } finally {
                queueMutex.withLock { running-- }
            }
        }
    }

    suspend fun pause(taskId: Long) = withContext(dispatcher.io) {
        database.downloadTaskDao().updateStatus(taskId, DownloadStatus.PAUSED.storageValue)
        publish(taskId, DownloadStatus.PAUSED)
    }

    suspend fun resume(taskId: Long) = withContext(dispatcher.io) {
        database.downloadTaskDao().updateStatus(taskId, DownloadStatus.QUEUED.storageValue)
        pump()
    }

    suspend fun cancel(taskId: Long) = withContext(dispatcher.io) {
        val task = database.downloadTaskDao().findById(taskId) ?: return@withContext
        cleanupTemp(task)
        database.downloadTaskDao().updateStatus(taskId, DownloadStatus.CANCELED.storageValue)
        publish(taskId, DownloadStatus.CANCELED)
    }

    suspend fun retry(taskId: Long) = withContext(dispatcher.io) {
        database.downloadTaskDao().updateStatus(taskId, DownloadStatus.QUEUED.storageValue, null)
        pump()
    }

    // ---------------------------------------------------------------------

    private suspend fun process(entity: DownloadTaskEntity) {
        val task = entity.toDomain()
        database.downloadTaskDao().updateStatus(task.id, DownloadStatus.PREPARING.storageValue)
        publish(task.id, DownloadStatus.PREPARING)

        var tempVideo: File? = null
        var tempAudio: File? = null

        try {
            val needsMux = !task.audioUrl.isNullOrEmpty()
            val videoTarget = if (needsMux) {
                storage.tempFile("${task.id}_video.${task.videoUrl.extensionOr("mp4")}")
                    .also { tempVideo = it }
            } else {
                storage.buildOutputFile(task.title, task.formatId.qualityTag(), "mp4", task.mediaType)
            }

            // Persist temp paths immediately so pause/cancel/retry can clean up
            // even after the process is killed and restarted.
            if (needsMux) {
                database.downloadTaskDao().update(
                    entity.copy(tempVideoPath = videoTarget.absolutePath)
                )
            }

            var lastBytes = 0L
            var lastTime = System.currentTimeMillis()
            var speed = 0L

            val videoOutcome = downloader.download(
                url = task.videoUrl,
                target = videoTarget,
                headers = task.headers,
                expectedTotal = task.totalBytes,
                onProgress = { downloaded, total ->
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastTime
                    if (elapsed >= 500) {
                        speed = ((downloaded - lastBytes) * 1000) / elapsed
                        lastBytes = downloaded
                        lastTime = now
                    }
                    publish(task.id, DownloadStatus.DOWNLOADING, downloaded, total, speed)
                }
            )
            if (videoOutcome is DownloadOutcome.Paused) {
                publish(task.id, DownloadStatus.PAUSED)
                return
            }

            val finalVideo = if (needsMux) {
                val audioTarget = storage.tempFile("${task.id}_audio.m4a")
                    .also { tempAudio = it }
                database.downloadTaskDao().update(
                    entity.copy(
                        tempVideoPath = videoTarget.absolutePath,
                        tempAudioPath = audioTarget.absolutePath
                    )
                )
                val audioOutcome = downloader.download(
                    url = task.audioUrl!!,
                    target = audioTarget,
                    headers = task.headers,
                    onProgress = { downloaded, total ->
                        publish(task.id, DownloadStatus.DOWNLOADING, downloaded, total, 0L)
                    }
                )
                if (audioOutcome is DownloadOutcome.Paused) {
                    publish(task.id, DownloadStatus.PAUSED)
                    return
                }
                publish(task.id, DownloadStatus.MUXING)
                database.downloadTaskDao().updateStatus(task.id, DownloadStatus.MUXING.storageValue)
                val muxOutput = storage.buildOutputFile(task.title, task.formatId.qualityTag(), "mp4", MediaType.VIDEO)
                muxer.mux(videoTarget, audioTarget, muxOutput)
                muxOutput
            } else {
                videoTarget
            }

            val finalFile = maybeConvertAudio(finalVideo, task)

            val completed = task.copy(
                outputPath = finalFile.absolutePath,
                totalBytes = finalFile.length(),
                downloadedBytes = finalFile.length(),
                status = DownloadStatus.COMPLETED,
                completedAt = System.currentTimeMillis(),
                isMuxed = needsMux,
                tempVideoPath = "",
                tempAudioPath = ""
            )
            database.downloadTaskDao().update(completed.toEntity())
            database.historyDao().insert(completed.toHistoryEntity())
            publish(task.id, DownloadStatus.COMPLETED, finalFile.length(), finalFile.length(), 0L)
        } catch (c: CancellationException) {
            publish(task.id, DownloadStatus.PAUSED)
            throw c
        } catch (t: Throwable) {
            database.downloadTaskDao()
                .updateStatus(task.id, DownloadStatus.FAILED.storageValue, t.message ?: t.javaClass.simpleName)
            publish(task.id, DownloadStatus.FAILED, errorMessage = t.message)
        } finally {
            // Intermediate files are only needed while muxing; remove them once
            // the job reaches a terminal state (completed or failed).
            val status = database.downloadTaskDao().findById(task.id)?.status
            if (status == DownloadStatus.COMPLETED.storageValue ||
                status == DownloadStatus.FAILED.storageValue
            ) {
                tempVideo?.let { runCatching { it.delete() } }
                tempAudio?.let { runCatching { it.delete() } }
                runCatching {
                    tempVideo?.let { File(it.parentFile, it.name + ".part").delete() }
                    tempAudio?.let { File(it.parentFile, it.name + ".part").delete() }
                }
            }
        }
    }

    private suspend fun maybeConvertAudio(input: File, task: DownloadTask): File {
        if (!audioConverter.supportsMp3) return input
        val bitrate = task.formatId.substringAfterLast('_').removeSuffix("K").toIntOrNull() ?: 192
        val mp3Target = storage.buildOutputFile(task.title, "${bitrate}K", "mp3", MediaType.AUDIO)
        return audioConverter.toMp3(input, mp3Target, bitrate)
    }

    private fun cleanupTemp(entity: DownloadTaskEntity) {
        listOf(entity.tempVideoPath, entity.tempAudioPath).forEach { path ->
            if (path.isNotBlank()) {
                runCatching { File(path).delete() }
                runCatching { File("$path.part").delete() }
            }
        }
    }

    private fun publish(
        taskId: Long,
        status: DownloadStatus,
        downloadedBytes: Long = 0L,
        totalBytes: Long = 0L,
        bytesPerSecond: Long = 0L,
        errorMessage: String? = null
    ) {
        _progress.update { current ->
            val previous = current[taskId]
            current + (taskId to DownloadProgress(
                taskId = taskId,
                downloadedBytes = if (downloadedBytes > 0) downloadedBytes else previous?.downloadedBytes ?: 0L,
                totalBytes = if (totalBytes > 0) totalBytes else previous?.totalBytes ?: 0L,
                bytesPerSecond = bytesPerSecond,
                status = status,
                errorMessage = errorMessage
            ))
        }
    }

    private fun String.extensionOr(fallback: String): String {
        val ext = substringBefore('?').substringAfterLast('.', "")
        return if (ext.length in 2..4) ext else fallback
    }

    private fun String.qualityTag(): String =
        removePrefix("MP4_").removePrefix("WEBM_").removeSuffix("P").takeIf { it.isNotBlank() } ?: "video"
}
