package com.snaptube.clone

import android.content.Context
import com.snaptube.clone.core.database.SnapTubeDatabase
import com.snaptube.clone.core.download.DownloadEngine
import com.snaptube.clone.core.download.DownloadRepository
import com.snaptube.clone.core.download.StorageManager
import com.snaptube.clone.core.extractor.ExtractorRegistry
import com.snaptube.clone.data.MediaLibraryRepository
import com.snaptube.clone.data.SettingsRepository

/**
 * Manual dependency container.
 *
 * Kept deliberately framework-free (no Hilt) so the project builds with the
 * bare AGP/Kotlin toolchain and is easy to reason about.
 */
class AppContainer(private val context: Context) {

    val database: SnapTubeDatabase by lazy { SnapTubeDatabase.get(context) }

    val storageManager: StorageManager by lazy { StorageManager(context) }

    val extractorRegistry: ExtractorRegistry by lazy { ExtractorRegistry() }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context) }

    val mediaLibraryRepository: MediaLibraryRepository by lazy { MediaLibraryRepository(context) }

    val downloadEngine: DownloadEngine by lazy {
        DownloadEngine(
            context = context,
            database = database,
            storage = storageManager
        )
    }

    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(
            context = context,
            database = database,
            engine = downloadEngine,
            storage = storageManager
        )
    }
}
