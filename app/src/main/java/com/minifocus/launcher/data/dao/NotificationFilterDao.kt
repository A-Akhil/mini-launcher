package com.minifocus.launcher.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.minifocus.launcher.data.entity.NotificationFilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationFilterDao {

    @Query("SELECT * FROM notification_filters ORDER BY app_name ASC")
    fun observeAll(): Flow<List<NotificationFilterEntity>>

    @Query("SELECT * FROM notification_filters WHERE package_name = :packageName LIMIT 1")
    suspend fun getFilter(packageName: String): NotificationFilterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(filter: NotificationFilterEntity)

    @Query("UPDATE notification_filters SET is_enabled = :enabled WHERE package_name = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean)

    @Query("UPDATE notification_filters SET is_enabled = :enabled")
    suspend fun setAllEnabled(enabled: Boolean)

    @Query("DELETE FROM notification_filters WHERE package_name = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
