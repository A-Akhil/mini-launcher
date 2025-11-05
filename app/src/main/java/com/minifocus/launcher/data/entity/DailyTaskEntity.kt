package com.minifocus.launcher.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.minifocus.launcher.model.DailyTaskItem

@Entity(tableName = "daily_tasks")
data class DailyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "start_epoch_day") val startEpochDay: Long? = null,
    @ColumnInfo(name = "end_epoch_day") val endEpochDay: Long? = null,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true,
    @ColumnInfo(name = "last_completed_epoch_day") val lastCompletedEpochDay: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

fun DailyTaskEntity.toItem(todayEpochDay: Long): DailyTaskItem {
    val withinStart = startEpochDay?.let { todayEpochDay >= it } ?: true
    val withinEnd = endEpochDay?.let { todayEpochDay <= it } ?: true
    val isActiveToday = isEnabled && withinStart && withinEnd
    val isCompletedToday = lastCompletedEpochDay != null && lastCompletedEpochDay == todayEpochDay
    return DailyTaskItem(
        id = id,
        title = title,
        startEpochDay = startEpochDay,
        endEpochDay = endEpochDay,
        isEnabled = isEnabled,
        lastCompletedEpochDay = lastCompletedEpochDay,
        createdAt = createdAt,
        isActiveToday = isActiveToday,
        isCompletedToday = isActiveToday && isCompletedToday
    )
}
