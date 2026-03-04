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
