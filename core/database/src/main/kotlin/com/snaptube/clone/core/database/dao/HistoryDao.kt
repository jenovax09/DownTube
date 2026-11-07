package com.snaptube.clone.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.snaptube.clone.core.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY downloadTime DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE mediaType = :mediaType ORDER BY downloadTime DESC")
    fun observeByMediaType(mediaType: Int): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): HistoryEntity?

    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' ORDER BY downloadTime DESC")
    fun search(query: String): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<HistoryEntity>)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM history WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM history")
    suspend fun clear()
}
