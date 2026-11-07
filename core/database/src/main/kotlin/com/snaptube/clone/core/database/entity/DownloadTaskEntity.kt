package com.snaptube.clone.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A queued / in-progress / finished download job.
 *
 * Unlike `history` (which only records completed items), this table keeps the
 * live state needed to resume, pause, retry and mux downloads.
 */
@Entity(
    tableName = "download_task",
    indices = [Index(value = ["status"]), Index(value = ["mediaId"])]
)
data class DownloadTaskEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "mediaId") val mediaId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "formatId") val formatId: String,
    @ColumnInfo(name = "mediaType") val mediaType: Int,
    @ColumnInfo(name = "source") val source: String = "",
    @ColumnInfo(name = "videoUrl") val videoUrl: String,
    @ColumnInfo(name = "audioUrl") val audioUrl: String? = null,
    @ColumnInfo(name = "headers") val headers: String = "",
    @ColumnInfo(name = "referrer") val referrer: String = "",
    @ColumnInfo(name = "coverUrl") val coverUrl: String = "",
    @ColumnInfo(name = "outputPath") val outputPath: String = "",
    @ColumnInfo(name = "tempVideoPath") val tempVideoPath: String = "",
    @ColumnInfo(name = "tempAudioPath") val tempAudioPath: String = "",
    @ColumnInfo(name = "totalBytes") val totalBytes: Long = 0L,
    @ColumnInfo(name = "downloadedBytes") val downloadedBytes: Long = 0L,
    @ColumnInfo(name = "status") val status: Int = 0,
    @ColumnInfo(name = "errorMessage") val errorMessage: String? = null,
    @ColumnInfo(name = "durationMs") val durationMs: Long = 0L,
    @ColumnInfo(name = "createdAt") val createdAt: Long = 0L,
    @ColumnInfo(name = "completedAt") val completedAt: Long? = null,
    @ColumnInfo(name = "isMuxed") val isMuxed: Int = 0
)
