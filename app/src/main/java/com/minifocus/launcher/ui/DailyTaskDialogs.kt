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

package com.minifocus.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import com.minifocus.launcher.model.DailyTaskRepeatMode
import com.minifocus.launcher.model.DailyTaskWeekdayMask
import com.minifocus.launcher.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTaskEditorDialog(
    title: String,
    initialTitle: String,
    initialStartEpochDay: Long?,
    initialEndEpochDay: Long?,
    initialEnabled: Boolean,
    initialRepeatMode: DailyTaskRepeatMode,
    @Suppress("UNUSED_PARAMETER") initialIntervalDays: Int,
    initialDaysOfWeekMask: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?, Long?, Boolean, DailyTaskRepeatMode, Int, Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialTitle) }
    var enabled by remember { mutableStateOf(initialEnabled) }
    var limitRange by remember { mutableStateOf(initialStartEpochDay != null || initialEndEpochDay != null) }
    var startDate by remember {
        mutableStateOf(initialStartEpochDay?.let { LocalDate.ofEpochDay(it) })
    }
    var endDate by remember {
        mutableStateOf(initialEndEpochDay?.let { LocalDate.ofEpochDay(it) })
    }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(initialRepeatMode) }
    var selectedDaysMask by remember {
        mutableStateOf(DailyTaskWeekdayMask.normalized(initialDaysOfWeekMask))
    }
    val zoneId = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .wrapContentHeight()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.task_name_label), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = stringResource(R.string.task_repeat_pattern), color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatOption(
                        label = stringResource(R.string.task_daily_label),
                        selected = repeatMode == DailyTaskRepeatMode.EVERY_DAY
                    ) { repeatMode = DailyTaskRepeatMode.EVERY_DAY }

                    RepeatOption(
                        label = stringResource(R.string.task_alternate_label),
                        selected = repeatMode == DailyTaskRepeatMode.EVERY_OTHER_DAY
                    ) { repeatMode = DailyTaskRepeatMode.EVERY_OTHER_DAY }

                    RepeatOption(
                        label = stringResource(R.string.task_days_label),
                        selected = repeatMode == DailyTaskRepeatMode.SPECIFIC_DAYS
                    ) { repeatMode = DailyTaskRepeatMode.SPECIFIC_DAYS }
                }

                if (repeatMode == DailyTaskRepeatMode.EVERY_OTHER_DAY) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.task_repeats_every_two_days),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                if (repeatMode == DailyTaskRepeatMode.SPECIFIC_DAYS) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.task_choose_days),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DayOfWeek.values().forEach { day ->
                            val maskForDay = DailyTaskWeekdayMask.maskFor(day)
                            val selected = DailyTaskWeekdayMask.contains(selectedDaysMask, day)
                            WeekdayToggle(
                                label = dayLabel(day),
                                selected = selected,
                                onClick = {
                                    val toggled = if (selected) {
                                        selectedDaysMask and maskForDay.inv()
                                    } else {
                                        selectedDaysMask or maskForDay
                                    }
                                    selectedDaysMask = if (toggled == 0) maskForDay else toggled
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.task_enabled),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (enabled) {
                                stringResource(R.string.task_enabled_will_show_scheduled)
                            } else {
                                stringResource(R.string.task_enabled_hidden_until_reenabled)
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                            checkedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.task_limit_date_range), color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                        Text(
                            text = if (limitRange) stringResource(R.string.task_limit_window_hint) else stringResource(R.string.task_limit_indefinite_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = limitRange,
                        onCheckedChange = { isChecked ->
                            limitRange = isChecked
                            if (isChecked && startDate == null) {
                                startDate = LocalDate.now()
                            }
                            if (!isChecked) {
                                startDate = null
                                endDate = null
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                            checkedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }

                if (limitRange) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                    ) {
                        Text(
                            text = startDate?.format(dateFormatter) ?: stringResource(R.string.task_select_start_date),
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                    ) {
                        Text(
                            text = endDate?.format(dateFormatter) ?: stringResource(R.string.task_select_end_date_optional),
                            fontSize = 14.sp
                        )
                    }

                    if (endDate != null) {
                        TextButton(onClick = { endDate = null }) {
                            Text(text = stringResource(R.string.task_clear_end_date), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) return@TextButton
                    val effectiveStart = if (limitRange) startDate ?: LocalDate.now(zoneId) else null
                    val startEpoch = effectiveStart?.toEpochDay()
                    val endEpoch = if (limitRange) endDate?.toEpochDay() else null
                    val daysMask = if (repeatMode == DailyTaskRepeatMode.SPECIFIC_DAYS) {
                        selectedDaysMask
                    } else {
                        DailyTaskWeekdayMask.ALL
                    }
                    val interval = if (repeatMode == DailyTaskRepeatMode.EVERY_OTHER_DAY) 2 else 1
                    onConfirm(trimmed, startEpoch, endEpoch, enabled, repeatMode, interval, daysMask)
                },
                enabled = name.trim().isNotEmpty()
            ) {
                Text(text = stringResource(R.string.action_save), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                onDelete?.let {
                    TextButton(onClick = it) {
                        Text(text = stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )

    if (showStartPicker) {
        val initialMillis = startDate?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli()
            ?: System.currentTimeMillis()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            startDate = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
                            if (endDate != null && startDate != null && endDate!!.isBefore(startDate)) {
                                endDate = startDate
                            }
                        }
                        showStartPicker = false
                    }
                ) {
                    Text(text = stringResource(R.string.task_set_label), color = MaterialTheme.colorScheme.onBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text(text = stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface))
        }
    }

    if (showEndPicker) {
        val initialMillis = endDate?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli()
            ?: startDate?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli()
            ?: System.currentTimeMillis()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            endDate = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
                            if (startDate != null && endDate != null && endDate!!.isBefore(startDate)) {
                                startDate = endDate
                            }
                        }
                        showEndPicker = false
                    }
                ) {
                    Text(text = stringResource(R.string.task_set_label), color = MaterialTheme.colorScheme.onBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text(text = stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface))
        }
    }
}
