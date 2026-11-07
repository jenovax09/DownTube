package com.snaptube.clone.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlist")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long = 0L,
    @ColumnInfo(name = "isDefault") val isDefault: Int = 0
)

@Entity(
    tableName = "playlist_item",
    indices = [Index(value = ["playlistId"]), Index(value = ["path"])],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "playlistId") val playlistId: Long,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "durationMs") val durationMs: Long = 0L,
    @ColumnInfo(name = "position") val position: Int = 0
)

/** A search term the user typed before. */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey @ColumnInfo(name = "query") val query: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = 0L
)

/** A bookmarked / speed-dial site. */
@Entity(tableName = "bookmark")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "iconUrl") val iconUrl: String = "",
    @ColumnInfo(name = "createdAt") val createdAt: Long = 0L
)
