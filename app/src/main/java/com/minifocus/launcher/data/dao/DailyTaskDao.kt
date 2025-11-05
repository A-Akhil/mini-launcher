package com.minifocus.launcher.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.minifocus.launcher.data.entity.DailyTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTaskDao {
    @Query("SELECT * FROM daily_tasks ORDER BY created_at ASC")
    fun observeDailyTasks(): Flow<List<DailyTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DailyTaskEntity): Long

    @Update
    suspend fun update(entity: DailyTaskEntity)

    @Delete
    suspend fun delete(entity: DailyTaskEntity)

    @Query("SELECT * FROM daily_tasks WHERE id = :taskId LIMIT 1")
    suspend fun get(taskId: Long): DailyTaskEntity?

    @Query("DELETE FROM daily_tasks WHERE id = :taskId")
    suspend fun delete(taskId: Long)
}
