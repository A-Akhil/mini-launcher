package com.minifocus.launcher.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.minifocus.launcher.data.entity.AppUsageStatsEntity

@Dao
interface AppUsageStatsDao {
    @Query("SELECT * FROM app_usage_stats")
    suspend fun getAll(): List<AppUsageStatsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppUsageStatsEntity)

    @Query("DELETE FROM app_usage_stats WHERE package_name = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM app_usage_stats")
    suspend fun clearAll()
}
