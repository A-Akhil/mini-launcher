/*
 * Minimalist Focus Launcher
 * Copyright (C) 2025 A-Akhil
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.minifocus.launcher.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.minifocus.launcher.manager.CalendarManager
import com.minifocus.launcher.model.DeviceCalendar
import com.minifocus.launcher.ui.components.ScreenHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CalendarSettingsScreen(
    selectedCalendarId: Long,
    showTasksInCalendar: Boolean,
    syncTasksWithCalendar: Boolean,
    syncTasksWithDate: Boolean,
    syncDailyReminders: Boolean,
    onSelectCalendar: (calendarId: Long, accountName: String) -> Unit,
    onToggleShowTasksInCalendar: (Boolean) -> Unit,
    onToggleSyncTasksWithCalendar: (Boolean) -> Unit,
    onToggleSyncTasksWithDate: (Boolean) -> Unit,
    onToggleSyncDailyReminders: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var writableCalendars by remember { mutableStateOf<List<DeviceCalendar>>(emptyList()) }
    val calendarManager = remember { CalendarManager(context) }

    val hasPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            writableCalendars = withContext(Dispatchers.IO) {
                calendarManager.listCalendars().filter { it.isWritable }
            }
        }
    }

    var currentScreen by remember { mutableStateOf("Main") }

    val handleBack = {
        if (currentScreen != "Main") {
            currentScreen = "Main"
        } else {
            onBack()
        }
    }

    androidx.activity.compose.BackHandler(enabled = currentScreen != "Main") {
        currentScreen = "Main"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        when (currentScreen) {
            "Main" -> {
                ScreenHeader(
                    title = "Calendar",
                    onBack = onBack
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                SettingClickableRow(
                    title = "Accounts",
                    description = "Choose which calendar to use for creating events",
                    onClick = { currentScreen = "Accounts" }
                )
                SettingClickableRow(
                    title = "Sync",
                    description = "Configure how tasks and reminders sync with your calendar",
                    onClick = { currentScreen = "Sync" }
                )
            }
            "Accounts" -> {
                ScreenHeader(
                    title = "Accounts",
                    onBack = handleBack
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Default calendar",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Events you create from the launcher will be added to this calendar. Changes sync automatically via your Google account.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!hasPermission) {
                    Text(
                        text = "Calendar permission not granted. Open the Calendar page to grant permission first.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                } else if (writableCalendars.isEmpty()) {
                    Text(
                        text = "No writable calendars found on this device.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                } else {
                    // Auto-select option (let the app pick)
                    val autoSelected = selectedCalendarId <= 0
                    CalendarPickerRow(
                        displayName = "Auto-detect",
                        accountName = "Let the launcher pick the best calendar",
                        isSelected = autoSelected,
                        onClick = { onSelectCalendar(-1L, "") }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    writableCalendars.forEach { calendar ->
                        CalendarPickerRow(
                            displayName = calendar.displayName,
                            accountName = calendar.accountName,
                            isSelected = calendar.id == selectedCalendarId,
                            onClick = { onSelectCalendar(calendar.id, calendar.accountName) }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
            "Sync" -> {
                ScreenHeader(
                    title = "Sync Settings",
                    onBack = handleBack
                )

                Spacer(modifier = Modifier.height(24.dp))

                SettingToggleRow(
                    title = "Show tasks in calendar",
                    description = "See your tasks directly inside the launcher's calendar view",
                    isChecked = showTasksInCalendar,
                    onCheckedChange = onToggleShowTasksInCalendar
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingToggleRow(
                    title = "Add tasks to device calendar",
                    description = "Sync your tasks so they appear in Google Calendar and across your devices",
                    isChecked = syncTasksWithCalendar,
                    onCheckedChange = onToggleSyncTasksWithCalendar
                )

                if (syncTasksWithCalendar) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        SettingToggleRow(
                            title = "Sync one-time tasks",
                            description = "Includes tasks that have a specific date set",
                            isChecked = syncTasksWithDate,
                            onCheckedChange = onToggleSyncTasksWithDate
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SettingToggleRow(
                            title = "Sync repeating tasks",
                            description = "Includes tasks that repeat daily or on specific days",
                            isChecked = syncDailyReminders,
                            onCheckedChange = onToggleSyncDailyReminders
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingClickableRow(
    title: String,
    description: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )
            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp
            )
            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
        androidx.compose.material3.Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                checkedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
    }
}

@Composable
private fun CalendarPickerRow(
    displayName: String,
    accountName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (accountName.isNotEmpty()) {
                Text(
                    text = accountName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
