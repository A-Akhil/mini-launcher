package com.minifocus.launcher.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_time_reminders")
data class AppTimeReminderEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_label")
    val appLabel: String,

    @ColumnInfo(name = "default_duration_minutes")
    val defaultDurationMinutes: Int? = null,

    @ColumnInfo(name = "expiry_action")
    val expiryAction: String = "NOTIFICATION"
)
