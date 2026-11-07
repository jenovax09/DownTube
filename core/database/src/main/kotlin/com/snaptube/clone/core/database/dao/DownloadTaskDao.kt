package com.snaptube.clone.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.snaptube.clone.core.database.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTaskDao {

    @Query("SELECT * FROM download_task ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_task WHERE status IN (0,1,2,3) ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_task WHERE status = 5 ORDER BY completedAt DESC")
    fun observeCompleted(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_task WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): DownloadTaskEntity?

    @Query("SELECT * FROM download_task WHERE status IN (0,1,2,3) ORDER BY createdAt ASC")
    suspend fun activeTasks(): List<DownloadTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadTaskEntity): Long

    @Update
    suspend fun update(entity: DownloadTaskEntity)

    @Query("UPDATE download_task SET downloadedBytes = :bytes, totalBytes = :total WHERE id = :id")
    suspend fun updateProgress(id: Long, bytes: Long, total: Long)

    @Query("UPDATE download_task SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int, error: String? = null)

    @Query("DELETE FROM download_task WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM download_task WHERE status IN (5,6,7)")
    suspend fun clearFinished()
}
