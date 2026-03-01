package com.minifocus.launcher.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.minifocus.launcher.data.entity.AppTimeReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppTimeReminderDao {

    @Query("SELECT * FROM app_time_reminders ORDER BY app_label ASC")
    fun observeAll(): Flow<List<AppTimeReminderEntity>>

    @Query("SELECT * FROM app_time_reminders WHERE package_name = :packageName")
    suspend fun getByPackage(packageName: String): AppTimeReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppTimeReminderEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entities: List<AppTimeReminderEntity>)

    @Query("DELETE FROM app_time_reminders WHERE package_name = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT COUNT(*) FROM app_time_reminders WHERE package_name = :packageName")
    suspend fun isTracked(packageName: String): Int

    @Query("SELECT COUNT(*) FROM app_time_reminders")
    suspend fun count(): Int

    @Query("UPDATE app_time_reminders SET expiry_action = :expiryAction WHERE package_name = :packageName")
    suspend fun updateExpiryAction(packageName: String, expiryAction: String)
}
