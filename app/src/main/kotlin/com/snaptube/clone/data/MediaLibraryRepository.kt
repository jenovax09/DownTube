package com.snaptube.clone.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.snaptube.clone.core.model.LocalMedia
import com.snaptube.clone.core.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans the device MediaStore for the videos/audio the app has downloaded (or
 * that the user already has), powering the "Videos" / "Audio" library tabs.
 */
class MediaLibraryRepository(private val context: Context) {

    suspend fun videos(): List<LocalMedia> = query(
        collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        mediaType = MediaType.VIDEO
    )

    suspend fun audio(): List<LocalMedia> = query(
        collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        mediaType = MediaType.AUDIO
    )

    private suspend fun query(
        collection: android.net.Uri,
        mediaType: MediaType
    ): List<LocalMedia> = withContext(Dispatchers.IO) {
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.MediaColumns.MIME_TYPE)
            add(MediaStore.MediaColumns.DATA)
            if (mediaType == MediaType.VIDEO) add(MediaStore.Video.Media.DURATION)
            else {
                add(MediaStore.Audio.Media.DURATION)
                add(MediaStore.Audio.Media.ALBUM)
                add(MediaStore.Audio.Media.ARTIST)
            }
        }.toTypedArray()

        val results = mutableListOf<LocalMedia>()
        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                results += LocalMedia(
                    id = id,
                    path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else "",
                    title = cursor.getString(nameCol).orEmpty(),
                    mediaType = mediaType,
                    durationMs = if (durationCol >= 0) cursor.getLong(durationCol) else 0L,
                    sizeBytes = cursor.getLong(sizeCol),
                    mimeType = if (mimeCol >= 0) cursor.getString(mimeCol).orEmpty() else "",
                    dateAdded = cursor.getLong(dateCol) * 1000L,
                    album = if (albumCol >= 0) cursor.getString(albumCol).orEmpty() else "",
                    artist = if (artistCol >= 0) cursor.getString(artistCol).orEmpty() else ""
                )
            }
        }
        results
    }

    fun contentUriFor(id: Long, mediaType: MediaType): android.net.Uri =
        if (mediaType == MediaType.VIDEO) {
            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
        } else {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        }
}
