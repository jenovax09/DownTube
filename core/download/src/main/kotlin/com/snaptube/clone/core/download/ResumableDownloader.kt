package com.snaptube.clone.core.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.coroutines.coroutineContext

/** Result of a completed or resumed download pass. */
sealed interface DownloadOutcome {
    data class Completed(val file: File, val totalBytes: Long) : DownloadOutcome
    data object Paused : DownloadOutcome
}

/**
 * Pull-based, resumable HTTP downloader.
 *
 * A `.part` file is written next to the target; the byte offset already on disk
 * is sent as a Range header so a paused transfer resumes instead of restarting.
 * Progress is reported through [onProgress] on the caller's coroutine.
 */
class ResumableDownloader(
    private val chunkSize: Int = DEFAULT_CHUNK
) {

    suspend fun download(
        url: String,
        target: File,
        headers: Map<String, String> = emptyMap(),
        expectedTotal: Long = 0L,
        onProgress: suspend (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): DownloadOutcome = withContext(Dispatchers.IO) {
        val partFile = File(target.parentFile, target.name + ".part")
        var downloaded = if (partFile.exists()) partFile.length() else 0L

        var total = expectedTotal
        if (java.io.File(target.parentFile, target.name).exists()) target.delete()

        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }
        if (downloaded > 0) requestBuilder.header("Range", "bytes=$downloaded-")

        DownloadHttp.client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} for $url")
            }
            if (downloaded > 0 && response.code == 200) {
                // Server ignored our Range request: discard partial data and restart.
                partFile.delete()
                downloaded = 0L
            }
            val body = response.body ?: throw IOException("Empty body for $url")
            val contentLength = body.contentLength()
            total = when {
                response.code == 206 && expectedTotal > 0 -> expectedTotal
                downloaded + contentLength > 0 -> downloaded + contentLength
                else -> expectedTotal
            }

            val file = RandomAccessFile(partFile, "rw")
            file.seek(downloaded)
            try {
                body.byteStream().use { input ->
                    val buffer = ByteArray(chunkSize)
                    var lastReported = downloaded
                    onProgress(downloaded, total)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        file.write(buffer, 0, read)
                        downloaded += read
                        if (downloaded - lastReported >= REPORT_INTERVAL || downloaded == total) {
                            lastReported = downloaded
                            onProgress(downloaded, total)
                        }
                    }
                }
            } catch (c: CancellationException) {
                throw c
            } finally {
                file.close()
            }
        }

        if (total > 0 && downloaded < total) {
            // Connection dropped mid-transfer; partial file kept for resume.
            return@withContext DownloadOutcome.Paused
        }
        if (target.exists()) target.delete()
        if (!partFile.renameTo(target)) {
            partFile.copyTo(target, overwrite = true)
            partFile.delete()
        }
        DownloadOutcome.Completed(target, downloaded)
    }

    companion object {
        private const val DEFAULT_CHUNK = 64 * 1024
        private const val REPORT_INTERVAL = 256 * 1024L
    }
}
