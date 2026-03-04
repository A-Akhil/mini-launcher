/*
 * Minimalist Focus Launcher
 * Copyright (C) 2025 A-Akhil
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
