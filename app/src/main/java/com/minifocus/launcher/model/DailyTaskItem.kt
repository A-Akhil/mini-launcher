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

package com.minifocus.launcher.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

enum class DailyTaskRepeatMode {
    EVERY_DAY,
    EVERY_OTHER_DAY,
    SPECIFIC_DAYS;

    companion object {
        fun fromStorage(value: String?): DailyTaskRepeatMode = values().firstOrNull { it.name == value } ?: EVERY_DAY
    }
}

object DailyTaskWeekdayMask {
    private val allDays = DayOfWeek.values()

    val ALL: Int = allDays.fold(0) { acc, day -> acc or maskFor(day) }

    fun maskFor(day: DayOfWeek): Int = 1 shl day.ordinal

    fun contains(mask: Int, day: DayOfWeek): Boolean = (mask and maskFor(day)) != 0

    fun normalized(mask: Int): Int = if (mask == 0) ALL else mask

    fun selectedDays(mask: Int): List<DayOfWeek> {
        val effective = normalized(mask)
        return allDays.filter { contains(effective, it) }
    }
}

data class DailyTaskItem(
    val id: Long,
    val title: String,
    val startEpochDay: Long?,
    val endEpochDay: Long?,
    val isEnabled: Boolean,
    val lastCompletedEpochDay: Long?,
    val createdAt: Long,
    val repeatMode: DailyTaskRepeatMode,
    val intervalDays: Int,
    val daysOfWeekMask: Int,
    val isActiveToday: Boolean,
    val isCompletedToday: Boolean
) {
    val selectedWeekdays: List<DayOfWeek>
        get() = if (repeatMode == DailyTaskRepeatMode.SPECIFIC_DAYS) {
            DailyTaskWeekdayMask.selectedDays(daysOfWeekMask)
        } else {
            emptyList()
        }

    fun createdEpochDay(zoneId: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(createdAt).atZone(zoneId).toLocalDate().toEpochDay()
}

fun DailyTaskItem.patternLabel(): String {
    return when (repeatMode) {
        DailyTaskRepeatMode.EVERY_DAY -> "Daily"
        DailyTaskRepeatMode.EVERY_OTHER_DAY -> "Alternate days"
        DailyTaskRepeatMode.SPECIFIC_DAYS -> {
            val effectiveMask = DailyTaskWeekdayMask.normalized(daysOfWeekMask)
            if (effectiveMask == DailyTaskWeekdayMask.ALL) {
                "Daily"
            } else {
                selectedWeekdays.joinToString(" ") { it.shortLabel() }
            }
        }
    }
}

private fun DayOfWeek.shortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
}
