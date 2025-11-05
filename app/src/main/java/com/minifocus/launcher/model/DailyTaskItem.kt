package com.minifocus.launcher.model

data class DailyTaskItem(
    val id: Long,
    val title: String,
    val startEpochDay: Long?,
    val endEpochDay: Long?,
    val isEnabled: Boolean,
    val lastCompletedEpochDay: Long?,
    val createdAt: Long,
    val isActiveToday: Boolean,
    val isCompletedToday: Boolean
)
