package com.snaptube.clone.core.download

import android.content.Context
import com.snaptube.clone.core.database.SnapTubeDatabase
import com.snaptube.clone.core.database.mapper.toDomain
import com.snaptube.clone.core.model.DownloadTask
import com.snaptube.clone.core.model.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Coordinating layer between the UI and [DownloadEngine].
 *
 * The UI reads flows from here and calls the suspend actions; the engine owns
 * the actual transfer + persistence.
 */
class DownloadRepository(
    private val context: Context,
    private val database: SnapTubeDatabase,
    private val engine: DownloadEngine,
    private val storage: StorageManager
) {

    val allTasks: Flow<List<DownloadTask>> =
        database.downloadTaskDao().observeAll().map { list -> list.map { it.toDomain() } }

    val activeTasks: Flow<List<DownloadTask>> =
        database.downloadTaskDao().observeActive().map { list -> list.map { it.toDomain() } }

    val completedTasks: Flow<List<DownloadTask>> =
        database.downloadTaskDao().observeCompleted().map { list -> list.map { it.toDomain() } }

    val progress = engine.progress

    /** Queue a new download and start the foreground service. */
    suspend fun enqueue(task: DownloadTask): Long {
        val id = engine.enqueue(task)
        DownloadService.start(context.applicationContext)
        return id
    }

    suspend fun pause(id: Long) = engine.pause(id)
    suspend fun resume(id: Long) = engine.resume(id)
    suspend fun cancel(id: Long) = engine.cancel(id)
    suspend fun retry(id: Long) = engine.retry(id)

    /** Delete a finished download's file and row. */
    suspend fun delete(id: Long) {
        val entity = database.downloadTaskDao().findById(id) ?: return
        File(entity.outputPath).takeIf { it.exists() }?.delete()
        database.downloadTaskDao().deleteById(id)
    }

    fun folderFor(mediaType: MediaType): File = storage.directoryFor(mediaType)
}
