package com.minifocus.launcher.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.minifocus.launcher.data.entity.AppLockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLockDao {
    @Query("SELECT * FROM app_locks")
    fun observeLocks(): Flow<List<AppLockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLock(entity: AppLockEntity)

    @Query("DELETE FROM app_locks WHERE package_name = :packageName")
    suspend fun clearLock(packageName: String)

    @Query("SELECT * FROM app_locks WHERE package_name = :packageName LIMIT 1")
    suspend fun getLock(packageName: String): AppLockEntity?

    @Query("DELETE FROM app_locks WHERE locked_until < :currentTime")
    suspend fun clearExpiredLocks(currentTime: Long)

    @Query("SELECT * FROM app_locks ORDER BY locked_until ASC LIMIT 1")
    suspend fun getEarliestLock(): AppLockEntity?
}
