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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.minifocus.launcher.model.DailyTaskRepeatMode
import com.minifocus.launcher.model.DailyTaskWeekdayMask
import com.minifocus.launcher.model.TaskItem
import com.minifocus.launcher.ui.components.MinimalCheckbox
import com.minifocus.launcher.ui.components.ScreenHeader
import com.minifocus.launcher.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FancyAddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (String, Long?) -> Unit,
    onAddDailyTask: (String, Long?, Long?, Boolean, DailyTaskRepeatMode, Int, Int) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val defocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val taskName = remember { mutableStateOf("") }
    val enableReminder = remember { mutableStateOf(false) }
    val selectedDate = remember { mutableStateOf<LocalDateTime?>(null) }
    val showDatePicker = remember { mutableStateOf(false) }

    val repeatDaily = remember { mutableStateOf(false) }
    val dailyEnabled = remember { mutableStateOf(true) }
    val limitDailyRange = remember { mutableStateOf(false) }
    val dailyStartDate = remember { mutableStateOf<LocalDate?>(null) }
    val dailyEndDate = remember { mutableStateOf<LocalDate?>(null) }
    val showDailyStartPicker = remember { mutableStateOf(false) }
    val showDailyEndPicker = remember { mutableStateOf(false) }
    val repeatMode = remember { mutableStateOf(DailyTaskRepeatMode.EVERY_DAY) }
    val selectedDaysMask = remember { mutableStateOf(DailyTaskWeekdayMask.ALL) }
    val zoneId = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val clearFocus: () -> Unit = remember(focusManager, keyboardController, defocusRequester) {
        {
            try {
                defocusRequester.requestFocus()
            } catch (_: IllegalStateException) {
                // Ignore cases where the anchor is not yet attached
            }
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }
            .filter { it }
            .collectLatest {
                clearFocus()
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(
                    modifier = Modifier
                        .size(0.dp)
                        .focusRequester(defocusRequester)
                        .focusable()
                )
                Text(
                    text = stringResource(R.string.task_add_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = taskName.value,
                    onValueChange = { taskName.value = it },
                    label = { Text(stringResource(R.string.task_prompt_what_to_do), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.task_repeat_daily),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Switch(
                        checked = repeatDaily.value,
                        onCheckedChange = { isChecked ->
                            clearFocus()
                            repeatDaily.value = isChecked
                            if (isChecked) {
                                enableReminder.value = false
                                selectedDate.value = null
                            } else {
                                limitDailyRange.value = false
                                dailyStartDate.value = null
                                dailyEndDate.value = null
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

                if (repeatDaily.value) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = stringResource(R.string.task_repeat_pattern), color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RepeatOption(
                            label = stringResource(R.string.task_daily_label),
                            selected = repeatMode.value == DailyTaskRepeatMode.EVERY_DAY
                        ) {
                            clearFocus()
                            repeatMode.value = DailyTaskRepeatMode.EVERY_DAY
                        }

                        RepeatOption(
                            label = stringResource(R.string.task_alternate_label),
                            selected = repeatMode.value == DailyTaskRepeatMode.EVERY_OTHER_DAY
                        ) {
                            clearFocus()
                            repeatMode.value = DailyTaskRepeatMode.EVERY_OTHER_DAY
                        }

                        RepeatOption(
                            label = stringResource(R.string.task_days_label),
                            selected = repeatMode.value == DailyTaskRepeatMode.SPECIFIC_DAYS
                        ) {
                            clearFocus()
                            repeatMode.value = DailyTaskRepeatMode.SPECIFIC_DAYS
                        }
                    }

                    if (repeatMode.value == DailyTaskRepeatMode.EVERY_OTHER_DAY) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.task_repeats_every_two_days),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    if (repeatMode.value == DailyTaskRepeatMode.SPECIFIC_DAYS) {
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
                                val selected = DailyTaskWeekdayMask.contains(selectedDaysMask.value, day)
                                WeekdayToggle(
                                    label = dayLabel(day),
                                    selected = selected,
                                    onClick = {
                                        clearFocus()
                                        val toggled = if (selected) {
                                            selectedDaysMask.value and maskForDay.inv()
                                        } else {
                                            selectedDaysMask.value or maskForDay
                                        }
                                        selectedDaysMask.value = if (toggled == 0) maskForDay else toggled
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.task_enabled), color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                            Text(
                                text = if (dailyEnabled.value) stringResource(R.string.task_enabled_will_show_scheduled) else stringResource(R.string.task_enabled_hidden_until_reenabled),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                        Switch(
                            checked = dailyEnabled.value,
                            onCheckedChange = {
                                clearFocus()
                                dailyEnabled.value = it
                            },
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.task_limit_date_range), color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                            Text(
                                text = if (limitDailyRange.value) stringResource(R.string.task_limit_window_hint) else stringResource(R.string.task_limit_indefinite_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                        Switch(
                            checked = limitDailyRange.value,
                            onCheckedChange = { isChecked ->
                                clearFocus()
                                limitDailyRange.value = isChecked
                                if (isChecked && dailyStartDate.value == null) {
                                    dailyStartDate.value = LocalDate.now(zoneId)
                                }
                                if (!isChecked) {
                                    dailyStartDate.value = null
                                    dailyEndDate.value = null
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

                    if (limitDailyRange.value) {
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                clearFocus()
                                showDailyStartPicker.value = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                        ) {
                            Text(
                                text = dailyStartDate.value?.format(dateFormatter) ?: stringResource(R.string.task_select_start_date),
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                clearFocus()
                                showDailyEndPicker.value = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                        ) {
                            Text(
                                text = dailyEndDate.value?.format(dateFormatter) ?: stringResource(R.string.task_select_end_date_optional),
                                fontSize = 14.sp
                            )
                        }

                        if (dailyEndDate.value != null) {
                            TextButton(onClick = {
                                clearFocus()
                                dailyEndDate.value = null
                            }) {
                                Text(text = stringResource(R.string.task_clear_end_date), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.task_set_reminder),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Switch(
                            checked = enableReminder.value,
                            onCheckedChange = {
                                clearFocus()
                                enableReminder.value = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                                checkedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        )
                    }

                    if (enableReminder.value) {
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                clearFocus()
                                showDatePicker.value = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onBackground
                            )
                        ) {
                            Text(
                                text = selectedDate.value?.let {
                                    it.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))
                                } ?: stringResource(R.string.task_select_date_time),
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(stringResource(R.string.action_cancel), fontSize = 16.sp)
                    }

                    Button(
                        onClick = {
                            val title = taskName.value.trim()
                            if (title.isEmpty()) return@Button

                            if (repeatDaily.value) {
                                val startEpoch = if (limitDailyRange.value) {
                                    val start = dailyStartDate.value ?: LocalDate.now(zoneId)
                                    dailyStartDate.value = start
                                    start.toEpochDay()
                                } else null
                                val endEpoch = if (limitDailyRange.value) {
                                    dailyEndDate.value?.let { end ->
                                        val start = dailyStartDate.value ?: end
                                        if (end.isBefore(start)) {
                                            dailyStartDate.value = end
                                            end.toEpochDay()
                                        } else {
                                            end.toEpochDay()
                                        }
                                    }
                                } else null
                                val mode = repeatMode.value
                                val daysMask = if (mode == DailyTaskRepeatMode.SPECIFIC_DAYS) {
                                    selectedDaysMask.value
                                } else {
                                    DailyTaskWeekdayMask.ALL
                                }
                                val interval = if (mode == DailyTaskRepeatMode.EVERY_OTHER_DAY) 2 else 1
                                onAddDailyTask(
                                    title,
                                    startEpoch,
                                    endEpoch,
                                    dailyEnabled.value,
                                    mode,
                                    interval,
                                    daysMask
                                )
                            } else {
                                val scheduledTime = if (enableReminder.value && selectedDate.value != null) {
                                    selectedDate.value!!.atZone(zoneId).toInstant().toEpochMilli()
                                } else null
                                onAddTask(title, scheduledTime)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        enabled = taskName.value.trim().isNotEmpty()
                    ) {
                        Text(stringResource(R.string.task_add_icon), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Date Picker (simplified - just shows current date + time picker)
    if (showDatePicker.value) {
        DateTimePickerDialog(
            onDismiss = { showDatePicker.value = false },
            onConfirm = { dateTime ->
                selectedDate.value = dateTime
                showDatePicker.value = false
            }
        )
    }

    if (showDailyStartPicker.value) {
        val initialMillis = dailyStartDate.value?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli()
            ?: System.currentTimeMillis()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDailyStartPicker.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
                            dailyStartDate.value = date
                            if (dailyEndDate.value != null && dailyEndDate.value!!.isBefore(date)) {
                                dailyEndDate.value = date
                            }
                        }
                        showDailyStartPicker.value = false
                    }
                ) {
                    Text(text = stringResource(R.string.task_set_label), color = MaterialTheme.colorScheme.onBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDailyStartPicker.value = false }) {
                    Text(text = stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface))
        }
    }

    if (showDailyEndPicker.value) {
        val initialMillis = dailyEndDate.value?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli()
            ?: dailyStartDate.value?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli()
            ?: System.currentTimeMillis()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDailyEndPicker.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
                            dailyEndDate.value = date
                            if (dailyStartDate.value != null && date.isBefore(dailyStartDate.value)) {
                                dailyStartDate.value = date
                            }
                        }
                        showDailyEndPicker.value = false
                    }
                ) {
                    Text(text = stringResource(R.string.task_set_label), color = MaterialTheme.colorScheme.onBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDailyEndPicker.value = false }) {
                    Text(text = stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val showTimePicker = remember { mutableStateOf(false) }
    val selectedDate = remember { mutableStateOf<Long?>(null) }

    if (!showTimePicker.value) {
        // Show Date Picker first
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate.value = datePickerState.selectedDateMillis
                        if (selectedDate.value != null) {
                            showTimePicker.value = true
                        }
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text(stringResource(R.string.task_next_label), color = MaterialTheme.colorScheme.onBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    headlineContentColor = MaterialTheme.colorScheme.onBackground,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    subheadContentColor = MaterialTheme.colorScheme.onBackground,
                    yearContentColor = MaterialTheme.colorScheme.onBackground,
                    currentYearContentColor = MaterialTheme.colorScheme.onBackground,
                    selectedYearContentColor = MaterialTheme.colorScheme.background,
                    selectedYearContainerColor = MaterialTheme.colorScheme.onBackground,
                    dayContentColor = MaterialTheme.colorScheme.onBackground,
                    selectedDayContentColor = MaterialTheme.colorScheme.background,
                    selectedDayContainerColor = MaterialTheme.colorScheme.onBackground,
                    todayContentColor = MaterialTheme.colorScheme.onBackground,
                    todayDateBorderColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    } else {
        // Show Time Picker after date is selected
        TimePickerDialog(
            onDismiss = {
                showTimePicker.value = false
                selectedDate.value = null
            },
            onConfirm = { hour, minute ->
                selectedDate.value?.let { dateMillis ->
                    val localDate = Instant.ofEpochMilli(dateMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    val dateTime = localDate.atTime(hour, minute)
                    onConfirm(dateTime)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = LocalDateTime.now().hour,
        initialMinute = LocalDateTime.now().minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text(stringResource(R.string.task_confirm_label), color = MaterialTheme.colorScheme.onBackground)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                    clockDialSelectedContentColor = MaterialTheme.colorScheme.background,
                    clockDialUnselectedContentColor = MaterialTheme.colorScheme.onBackground,
                    selectorColor = MaterialTheme.colorScheme.onBackground,
                    containerColor = MaterialTheme.colorScheme.surface,
                    periodSelectorBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    periodSelectorSelectedContentColor = MaterialTheme.colorScheme.background,
                    periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onBackground,
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.onBackground,
                    timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.background,
                    timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: TaskItem,
    onDismiss: () -> Unit,
    onSave: (String, Long?) -> Unit,
    onDelete: () -> Unit
) {
    val inputText = remember { mutableStateOf(task.title) }
    val enableReminder = remember { mutableStateOf(task.scheduledFor != null) }
    val selectedDate = remember { 
        mutableStateOf<LocalDateTime?>(
            task.scheduledFor?.let { 
                LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
            }
        )
    }
    val showDatePicker = remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.task_edit_title), color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = inputText.value,
                    onValueChange = { inputText.value = it },
                    label = { Text(stringResource(R.string.task_name_label), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.task_reminder_label),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Switch(
                        checked = enableReminder.value,
                        onCheckedChange = { 
                            enableReminder.value = it
                            if (!it) selectedDate.value = null
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                            checkedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }
                
                if (enableReminder.value) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { showDatePicker.value = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Text(
                            text = selectedDate.value?.let {
                                it.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))
                            } ?: stringResource(R.string.task_select_date_time),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newTitle = inputText.value.trim()
                    if (newTitle.isNotEmpty()) {
                        val scheduledTime = if (enableReminder.value && selectedDate.value != null) {
                            selectedDate.value!!.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        } else null
                        onSave(newTitle, scheduledTime)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(stringResource(R.string.action_save), color = MaterialTheme.colorScheme.onBackground)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
    
    if (showDatePicker.value) {
        DateTimePickerDialog(
            onDismiss = { showDatePicker.value = false },
            onConfirm = { dateTime ->
                selectedDate.value = dateTime
                showDatePicker.value = false
            }
        )
    }
}

@Composable
fun HistoryScreen(
    historyTasks: List<TaskItem>,
    onBack: () -> Unit,
    onToggleTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.task_completed_tasks),
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (historyTasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.task_no_completed),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(
                    count = historyTasks.size,
                    key = { index -> historyTasks[index].id }
                ) { index ->
                    val task = historyTasks[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MinimalCheckbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggleTask(task.id) }
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                text = task.title,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp
                            )
                            task.completedAt?.let { timestamp ->
                                Text(
                                    text = stringResource(R.string.task_completed_format, formatTimestamp(timestamp)),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteTask(task.id) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer, thickness = 1.dp)
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))
}

@Composable
internal fun dayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> stringResource(R.string.weekday_mon)
    DayOfWeek.TUESDAY -> stringResource(R.string.weekday_tue)
    DayOfWeek.WEDNESDAY -> stringResource(R.string.weekday_wed)
    DayOfWeek.THURSDAY -> stringResource(R.string.weekday_thu)
    DayOfWeek.FRIDAY -> stringResource(R.string.weekday_fri)
    DayOfWeek.SATURDAY -> stringResource(R.string.weekday_sat)
    DayOfWeek.SUNDAY -> stringResource(R.string.weekday_sun)
}

@Composable
internal fun RepeatOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent
    val textColor = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = label, color = textColor, fontSize = 14.sp)
    }
}

@Composable
internal fun WeekdayToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent
    val textColor = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = textColor, fontSize = 12.sp)
    }
}
