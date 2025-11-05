package com.minifocus.launcher.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.minifocus.launcher.model.DailyTaskItem
import com.minifocus.launcher.model.DailyTaskRepeatMode
import com.minifocus.launcher.model.DailyTaskWeekdayMask
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Entity(tableName = "daily_tasks")
data class DailyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "start_epoch_day") val startEpochDay: Long? = null,
    @ColumnInfo(name = "end_epoch_day") val endEpochDay: Long? = null,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true,
    @ColumnInfo(name = "last_completed_epoch_day") val lastCompletedEpochDay: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "repeat_mode") val repeatMode: String = DailyTaskRepeatMode.EVERY_DAY.name,
    @ColumnInfo(name = "interval_days") val intervalDays: Int = 1,
    @ColumnInfo(name = "days_of_week_mask") val daysOfWeekMask: Int = 0
)

fun DailyTaskEntity.toItem(todayEpochDay: Long): DailyTaskItem {
    val withinStart = startEpochDay?.let { todayEpochDay >= it } ?: true
    val withinEnd = endEpochDay?.let { todayEpochDay <= it } ?: true
    val repeatModeEnum = DailyTaskRepeatMode.fromStorage(repeatMode)
    val zoneId = ZoneId.systemDefault()
    val referenceEpochDay = startEpochDay ?: Instant.ofEpochMilli(createdAt)
        .atZone(zoneId).toLocalDate().toEpochDay()
    val effectiveInterval = intervalDays.coerceAtLeast(1)
    val effectiveMask = DailyTaskWeekdayMask.normalized(daysOfWeekMask)
    val matchesRecurrence = when (repeatModeEnum) {
        DailyTaskRepeatMode.EVERY_DAY -> true
        DailyTaskRepeatMode.EVERY_OTHER_DAY -> {
            val delta = todayEpochDay - referenceEpochDay
            delta >= 0 && delta % effectiveInterval == 0L
        }
        DailyTaskRepeatMode.SPECIFIC_DAYS -> {
            if (effectiveMask == DailyTaskWeekdayMask.ALL) {
                true
            } else {
                val dayOfWeek = LocalDate.ofEpochDay(todayEpochDay).dayOfWeek
                DailyTaskWeekdayMask.contains(effectiveMask, dayOfWeek)
            }
        }
    }
    val isActiveToday = isEnabled && withinStart && withinEnd && matchesRecurrence
    val isCompletedToday = lastCompletedEpochDay != null && lastCompletedEpochDay == todayEpochDay
    return DailyTaskItem(
        id = id,
        title = title,
        startEpochDay = startEpochDay,
        endEpochDay = endEpochDay,
        isEnabled = isEnabled,
        lastCompletedEpochDay = lastCompletedEpochDay,
        createdAt = createdAt,
        repeatMode = repeatModeEnum,
        intervalDays = effectiveInterval,
        daysOfWeekMask = effectiveMask,
        isActiveToday = isActiveToday,
        isCompletedToday = isActiveToday && isCompletedToday
    )
}
