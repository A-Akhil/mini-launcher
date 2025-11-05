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
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import com.minifocus.launcher.model.DailyTaskRepeatMode
import com.minifocus.launcher.model.DailyTaskWeekdayMask

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
        title = { Text(text = title, color = Color.White) },
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
                    label = { Text("Title", color = Color(0xFFAAAAAA)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0xFF555555),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Repeat pattern", color = Color.White, fontSize = 16.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatOption(
                        label = "Daily",
                        selected = repeatMode == DailyTaskRepeatMode.EVERY_DAY
                    ) { repeatMode = DailyTaskRepeatMode.EVERY_DAY }

                    RepeatOption(
                        label = "Alternate",
                        selected = repeatMode == DailyTaskRepeatMode.EVERY_OTHER_DAY
                    ) { repeatMode = DailyTaskRepeatMode.EVERY_OTHER_DAY }

                    RepeatOption(
                        label = "Days",
                        selected = repeatMode == DailyTaskRepeatMode.SPECIFIC_DAYS
                    ) { repeatMode = DailyTaskRepeatMode.SPECIFIC_DAYS }
                }

                if (repeatMode == DailyTaskRepeatMode.EVERY_OTHER_DAY) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Repeats every 2 days",
                        color = Color(0xFF888888),
                        fontSize = 12.sp
                    )
                }

                if (repeatMode == DailyTaskRepeatMode.EVERY_OTHER_DAY) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Repeats every 2 days",
                        color = Color(0xFF888888),
                        fontSize = 12.sp
                    )
                }

                if (repeatMode == DailyTaskRepeatMode.SPECIFIC_DAYS) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Choose days",
                        color = Color(0xFF888888),
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
                        Text(text = "Task enabled", color = Color.White, fontSize = 16.sp)
                        Text(
                            text = if (enabled) "Will appear on eligible days" else "Will be hidden everywhere",
                            color = Color(0xFF888888),
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF444444),
                            uncheckedThumbColor = Color(0xFF666666),
                            uncheckedTrackColor = Color(0xFF222222)
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
                        Text(text = "Limit to date range", color = Color.White, fontSize = 16.sp)
                        Text(
                            text = if (limitRange) "Task only appears within the window" else "Follows repeat pattern indefinitely",
                            color = Color(0xFF888888),
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
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF444444),
                            uncheckedThumbColor = Color(0xFF666666),
                            uncheckedTrackColor = Color(0xFF222222)
                        )
                    )
                }

                if (limitRange) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(
                            text = startDate?.format(dateFormatter) ?: "Select start date",
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(
                            text = endDate?.format(dateFormatter) ?: "Select end date (optional)",
                            fontSize = 14.sp
                        )
                    }

                    if (endDate != null) {
                        TextButton(onClick = { endDate = null }) {
                            Text(text = "Clear end date", color = Color(0xFFAAAAAA))
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
                Text(text = "Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                onDelete?.let {
                    TextButton(onClick = it) {
                        Text(text = "Delete", color = Color(0xFFFF6666))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel", color = Color(0xFFAAAAAA))
                }
            }
        },
        containerColor = Color(0xFF1A1A1A)
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
                    Text(text = "Set", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) {
                    Text(text = "Cancel", color = Color(0xFFAAAAAA))
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1A1A))
        ) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1A1A)))
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
                    Text(text = "Set", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) {
                    Text(text = "Cancel", color = Color(0xFFAAAAAA))
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1A1A))
        ) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1A1A)))
        }
    }
}
