package com.snaptube.clone.core.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * WorkManager fallback used when the app is killed: it resumes any jobs that
 * were left active, then exits. Foreground work is normally handled by
 * [DownloadService]; this worker guarantees eventual progress/retry.
 */
class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val engine = DownloadServiceLocator.engine ?: return Result.success()
        return try {
            engine.pump()
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "snaptube_download_pump"
    }
}
