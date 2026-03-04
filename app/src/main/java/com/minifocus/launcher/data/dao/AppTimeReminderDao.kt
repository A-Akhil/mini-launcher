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
