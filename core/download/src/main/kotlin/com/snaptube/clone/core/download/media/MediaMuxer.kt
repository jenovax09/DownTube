package com.snaptube.clone.core.download.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device muxing/remuxing built on Media3 Transformer.
 *
 * Used to:
 *  - combine a video-only stream with an audio-only stream into one MP4, and
 *  - remux/extract audio into an M4A container.
 *
 * MP3 encoding is *not* done here: Media3 Transformer has no MP3 encoder.
 * See [AudioConverter] for the documented options.
 */
class MediaMuxer(private val context: Context) {

    /** Mux a video-only file with an audio-only file into [outputFile]. */
    suspend fun mux(videoFile: File, audioFile: File, outputFile: File): File =
        withContext(Dispatchers.IO) {
            if (outputFile.exists()) outputFile.delete()

            val videoItem = EditedMediaItem.Builder(MediaItem.fromUri(videoFile.toURI().toString())).build()
            val audioItem = EditedMediaItem.Builder(MediaItem.fromUri(audioFile.toURI().toString())).build()
            val sequence = EditedMediaItemSequence.Builder(videoItem)
                .addItem(audioItem)
                .build()
            val composition = Composition.Builder(sequence).build()

            runTransformer(Transformer.Builder(context).build(), composition, outputFile)
            outputFile
        }

    /** Extract the audio track into an M4A container (AAC passthrough). */
    suspend fun extractAudio(input: File, outputFile: File): File = withContext(Dispatchers.IO) {
        if (outputFile.exists()) outputFile.delete()
        val edited = EditedMediaItem.Builder(MediaItem.fromUri(input.toURI().toString()))
            .setRemoveVideo(true)
            .build()
        val composition = Composition.Builder(EditedMediaItemSequence(edited)).build()
        val transformer = Transformer.Builder(context)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .build()
        runTransformer(transformer, composition, outputFile)
        outputFile
    }

    private suspend fun runTransformer(
        transformer: Transformer,
        composition: Composition,
        outputFile: File
    ): Unit = suspendCancellableCoroutine { continuation ->
        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, result: ExportResult) {
                if (continuation.isActive) continuation.resume(Unit)
            }

            override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                if (continuation.isActive) continuation.resumeWithException(exception)
            }
        }
        continuation.invokeOnCancellation { transformer.cancel() }
        transformer.addListener(listener)
        try {
            transformer.start(composition, outputFile.absolutePath)
        } catch (t: Throwable) {
            if (continuation.isActive) continuation.resumeWithException(t)
        }
    }
}
