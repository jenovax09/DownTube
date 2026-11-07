package com.snaptube.clone.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.snaptube.clone.core.database.entity.BookmarkEntity
import com.snaptube.clone.core.database.entity.DeleteRecordEntity
import com.snaptube.clone.core.database.entity.PlaylistEntity
import com.snaptube.clone.core.database.entity.PlaylistItemEntity
import com.snaptube.clone.core.database.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM delete_record ORDER BY deleteTime DESC")
    fun observeAll(): Flow<List<DeleteRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DeleteRecordEntity): Long

    @Query("SELECT * FROM delete_record WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): DeleteRecordEntity?

    @Query("DELETE FROM delete_record WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM delete_record")
    suspend fun clear()
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 50")
    fun observeAll(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clear()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmark ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BookmarkEntity): Long

    @Query("DELETE FROM bookmark WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlist ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlaylistEntity): Long

    @Query("DELETE FROM playlist WHERE id = :id AND isDefault = 0")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM playlist_item WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeItems(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM playlist_item WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun items(playlistId: Long): List<PlaylistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(entity: PlaylistItemEntity): Long

    @Query("DELETE FROM playlist_item WHERE id = :id")
    suspend fun deleteItem(id: Long)
}
