package com.snaptube.clone.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.snaptube.clone.core.database.dao.BookmarkDao
import com.snaptube.clone.core.database.dao.DownloadTaskDao
import com.snaptube.clone.core.database.dao.HistoryDao
import com.snaptube.clone.core.database.dao.PlaylistDao
import com.snaptube.clone.core.database.dao.SearchHistoryDao
import com.snaptube.clone.core.database.dao.TrashDao
import com.snaptube.clone.core.database.entity.BookmarkEntity
import com.snaptube.clone.core.database.entity.DeleteRecordEntity
import com.snaptube.clone.core.database.entity.DownloadTaskEntity
import com.snaptube.clone.core.database.entity.HistoryEntity
import com.snaptube.clone.core.database.entity.PlaylistEntity
import com.snaptube.clone.core.database.entity.PlaylistItemEntity
import com.snaptube.clone.core.database.entity.SearchHistoryEntity

@Database(
    entities = [
        HistoryEntity::class,
        DeleteRecordEntity::class,
        DownloadTaskEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        SearchHistoryEntity::class,
        BookmarkEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SnapTubeDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun trashDao(): TrashDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        private const val DB_NAME = "clone_database.db"

        @Volatile
        private var instance: SnapTubeDatabase? = null

        fun get(context: Context): SnapTubeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SnapTubeDatabase::class.java,
                    DB_NAME
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
