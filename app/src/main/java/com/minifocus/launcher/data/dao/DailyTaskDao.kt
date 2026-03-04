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
