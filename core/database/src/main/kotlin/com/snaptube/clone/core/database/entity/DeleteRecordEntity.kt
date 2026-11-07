package com.snaptube.clone.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Recycle-bin entry created when a downloaded file is deleted.
 * Mirrors the `delete_record` table observed in the analysed app.
 */
@Entity(tableName = "delete_record")
data class DeleteRecordEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "deleteTime") val deleteTime: Long = 0L,
    @ColumnInfo(name = "downloadUrl") val downloadUrl: String = "",
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "fileSize") val fileSize: Long = 0L,
    @ColumnInfo(name = "duration") val duration: Long = 0L,
    @ColumnInfo(name = "cover") val cover: String = "",
    @ColumnInfo(name = "deletePath") val deletePath: String = "",
    @ColumnInfo(name = "format") val format: String = "",
    @ColumnInfo(name = "mediaType") val mediaType: Int = 0,
    @ColumnInfo(name = "delete_source", defaultValue = "0") val deleteSource: Int = 0,
    @ColumnInfo(name = "plugin_message") val pluginMessage: String = "",
    @ColumnInfo(name = "isVideo") val isVideo: Int = 0,
    @ColumnInfo(name = "isAudio") val isAudio: Int = 0,
    @ColumnInfo(name = "isImage") val isImage: Int = 0
)
