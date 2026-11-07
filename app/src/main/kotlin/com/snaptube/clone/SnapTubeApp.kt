package com.snaptube.clone

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.snaptube.clone.core.download.DownloadEngine
import com.snaptube.clone.core.download.DownloadServiceLocator
import com.snaptube.clone.core.download.DownloadWorker
import com.snaptube.clone.core.download.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SnapTubeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Make the engine reachable from the foreground service.
        DownloadServiceLocator.engine = container.downloadEngine

        // On cold start, resume anything that was left queued.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { container.downloadEngine.pump() }
        }

        // Register the periodic safety net so interrupted jobs eventually retry.
        WorkManager.getInstance(this).enqueueUniqueWork(
            DownloadWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        )
    }

    fun storageManager(): StorageManager = container.storageManager
}
