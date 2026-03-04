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

package com.minifocus.launcher.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_stats")
data class AppUsageStatsEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "total_score")
    val totalScore: Double,
    @ColumnInfo(name = "morning_score")
    val morningScore: Double,
    @ColumnInfo(name = "midday_score")
    val middayScore: Double,
    @ColumnInfo(name = "evening_score")
    val eveningScore: Double,
    @ColumnInfo(name = "late_score")
    val lateScore: Double,
    @ColumnInfo(name = "last_launch_epoch")
    val lastLaunchEpoch: Long,
    @ColumnInfo(name = "last_decay_day")
    val lastDecayDay: Int
)
