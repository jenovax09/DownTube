package com.snaptube.clone.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Completed-download history.
 *
 * Column names mirror the schema observed in the analysed app's
 * `app_database.db` (table `history`) so existing databases remain readable.
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "fileSize") val fileSize: Long = 0L,
    @ColumnInfo(name = "mediaType") val mediaType: Int = 0,
    @ColumnInfo(name = "downloadTime") val downloadTime: Long = 0L,
    @ColumnInfo(name = "downloadUrl") val downloadUrl: String = "",
    @ColumnInfo(name = "path") val path: String = "",
    @ColumnInfo(name = "referrer") val referrer: String = "",
    @ColumnInfo(name = "format") val format: String = "",
    @ColumnInfo(name = "cover") val cover: String = "",
    @ColumnInfo(name = "extra") val extra: String = "",
    @ColumnInfo(name = "is_lock") val isLock: Int = 0,
    @ColumnInfo(name = "plugin_message") val pluginMessage: String = "",
    @ColumnInfo(name = "duration") val duration: Long = 0L
)
