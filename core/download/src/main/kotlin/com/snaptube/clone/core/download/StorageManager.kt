package com.snaptube.clone.core.download

import android.content.Context
import android.os.Environment
import com.snaptube.clone.core.model.MediaType
import java.io.File

/**
 * Resolves and creates output directories.
 *
 * Default layout mirrors the analysed app:
 *   .../SnapTube Video/<file>.mp4
 *   .../SnapTube Audio/<file>.mp3
 *
 * On API 29+ the app can also write to its scoped MediaStore collections so no
 * storage permission is required; on older versions it falls back to the shared
 * public directory.
 */
class StorageManager(private val context: Context) {

    data class DownloadFolder(val root: File, val videoDir: File, val audioDir: File)

    fun resolveFolder(): DownloadFolder {
        val root: File = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            File(publicDir, "SnapTube")
        } else {
            File(context.filesDir, "downloads")
        }
        val videoDir = File(root, "SnapTube Video")
        val audioDir = File(root, "SnapTube Audio")
        videoDir.mkdirs()
        audioDir.mkdirs()
        return DownloadFolder(root, videoDir, audioDir)
    }

    fun directoryFor(mediaType: MediaType): File {
        val folder = resolveFolder()
        return if (mediaType == MediaType.AUDIO) folder.audioDir else folder.videoDir
    }

    /** Builds an output file named like "Title(720p).mp4". */
    fun buildOutputFile(title: String, qualityTag: String, ext: String, mediaType: MediaType): File {
        val folder = directoryFor(mediaType)
        val safeTitle = sanitize(title).ifEmpty { "snaptube_${System.currentTimeMillis()}" }
        val suffix = if (qualityTag.isBlank()) "" else "($qualityTag)"
        var candidate = File(folder, "$safeTitle$suffix.$ext")
        var index = 1
        while (candidate.exists()) {
            candidate = File(folder, "$safeTitle$suffix ($index).$ext")
            index++
        }
        return candidate
    }

    fun tempFile(name: String): File {
        val dir = File(context.cacheDir, "downloads").apply { mkdirs() }
        return File(dir, name)
    }

    fun sanitize(input: String): String =
        input.replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(120)
}
