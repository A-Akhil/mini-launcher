/*
 * Minimalist Focus Launcher
 * Copyright (C) 2025 A-Akhil
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.minifocus.launcher.model

/**
 * Represents a calendar event fetched from the device's CalendarContract.
 */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String = "",
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean = false,
    val calendarId: Long,
    val calendarDisplayName: String = "",
    val calendarColor: Int = 0,
    val eventColor: Int = 0,
    val location: String = ""
)

/**
 * Represents a device calendar discovered via CalendarContract.Calendars.
 */
data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val isPrimary: Boolean,
    val isWritable: Boolean
)
