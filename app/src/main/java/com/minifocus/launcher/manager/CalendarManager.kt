/*
 * Minimalist Focus Launcher
 * Copyright (C) 2025 A-Akhil
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.minifocus.launcher.manager

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.minifocus.launcher.model.CalendarEvent
import com.minifocus.launcher.model.DeviceCalendar
import java.util.TimeZone

/**
 * Reusable calendar service that wraps Android's CalendarContract Content Provider.
 * All operations go through ContentResolver so changes sync automatically via
 * Android's built-in sync adapters (Google Calendar sync, etc.).
 */
class CalendarManager(private val context: Context) {

    private val resolver: ContentResolver
        get() = context.contentResolver

    /**
     * Returns all calendars visible to the user, marking writable ones.
     * Prefers the user's primary Google calendar when available.
     */
    fun listCalendars(): List<DeviceCalendar> {
        val calendars = mutableListOf<DeviceCalendar>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )

        try {
            resolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = 1",
                null,
                "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val accessLevel = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL))
                    calendars.add(
                        DeviceCalendar(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)),
                            displayName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)) ?: "",
                            accountName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)) ?: "",
                            accountType = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)) ?: "",
                            color = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)),
                            isPrimary = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)) == 1,
                            isWritable = accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
        return calendars
    }

    /**
     * Returns the first writable calendar, preferring the primary Google calendar.
     */
    fun getDefaultWritableCalendar(): DeviceCalendar? {
        val calendars = listCalendars().filter { it.isWritable }
        // Prefer primary Google calendar
        return calendars.firstOrNull { it.isPrimary && it.accountType == "com.google" }
            ?: calendars.firstOrNull { it.accountType == "com.google" }
            ?: calendars.firstOrNull()
    }

    /**
     * Returns the writable calendar for the given ID, or falls back to default auto-detection.
     * A preferredId of -1 or 0 means auto-detect.
     */
    fun getWritableCalendar(preferredId: Long): DeviceCalendar? {
        if (preferredId > 0) {
            val calendars = listCalendars().filter { it.isWritable }
            val preferred = calendars.firstOrNull { it.id == preferredId }
            if (preferred != null) return preferred
        }
        return getDefaultWritableCalendar()
    }

    /**
     * Returns events for a specific day (midnight to midnight in the device timezone).
     */
    fun getEventsForDate(year: Int, month: Int, dayOfMonth: Int): List<CalendarEvent> {
        val tz = TimeZone.getDefault()
        val startCal = java.util.Calendar.getInstance(tz).apply {
            set(year, month - 1, dayOfMonth, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val endCal = java.util.Calendar.getInstance(tz).apply {
            set(year, month - 1, dayOfMonth, 23, 59, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }
        return queryEvents(startCal.timeInMillis, endCal.timeInMillis)
    }

    /**
     * Returns a set of day-of-month numbers that have events in the given month.
     * Used to render dot indicators on the calendar grid.
     */
    fun getDaysWithEvents(year: Int, month: Int): Set<Int> {
        val tz = TimeZone.getDefault()
        val startCal = java.util.Calendar.getInstance(tz).apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val endCal = java.util.Calendar.getInstance(tz).apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            add(java.util.Calendar.MONTH, 1)
        }
        val events = queryEvents(startCal.timeInMillis, endCal.timeInMillis)
        val daysSet = mutableSetOf<Int>()
        val cal = java.util.Calendar.getInstance(tz)
        for (event in events) {
            cal.timeInMillis = event.startMillis
            daysSet.add(cal.get(java.util.Calendar.DAY_OF_MONTH))
        }
        return daysSet
    }

    /**
     * Returns upcoming events from now to the specified number of days ahead.
     */
    fun getUpcomingEvents(daysAhead: Int = 7): List<CalendarEvent> {
        val now = System.currentTimeMillis()
        val end = now + daysAhead.toLong() * 24 * 60 * 60 * 1000
        return queryEvents(now, end)
    }

    /**
     * Creates a new calendar event and returns the event ID, or -1 on failure.
     */
    fun createEvent(
        title: String,
        startMillis: Long,
        endMillis: Long,
        calendarId: Long,
        description: String = "",
        allDay: Boolean = false,
        location: String = ""
    ): Long {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.ALL_DAY, if (allDay) 1 else 0)
            put(CalendarContract.Events.EVENT_LOCATION, location)
        }
        return try {
            val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull() ?: -1L
        } catch (e: SecurityException) {
            -1L
        }
    }

    /**
     * Updates an existing calendar event. Returns true on success.
     */
    fun updateEvent(
        eventId: Long,
        title: String,
        startMillis: Long,
        endMillis: Long,
        description: String = "",
        location: String = ""
    ): Boolean {
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_LOCATION, location)
        }
        return try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            resolver.update(uri, values, null, null) > 0
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Deletes a calendar event. Returns true on success.
     */
    fun deleteEvent(eventId: Long): Boolean {
        return try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            resolver.delete(uri, null, null) > 0
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Internal: queries CalendarContract.Instances for events in a time range.
     * Uses Instances table for correct handling of recurring events.
     */
    private fun queryEvents(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.EVENT_LOCATION
        )

        try {
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)

            resolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    events.add(
                        CalendarEvent(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)),
                            title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)) ?: "",
                            description = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)) ?: "",
                            startMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)),
                            endMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)),
                            allDay = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)) == 1,
                            calendarId = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)),
                            calendarDisplayName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)) ?: "",
                            calendarColor = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_COLOR)),
                            eventColor = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Instances.DISPLAY_COLOR)),
                            location = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)) ?: ""
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
        return events
    }
}
