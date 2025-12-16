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
