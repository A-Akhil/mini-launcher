package com.minifocus.launcher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FancyAddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (String, Long?) -> Unit,
    onAddDailyTask: (String, Long?, Long?, Boolean, DailyTaskRepeatMode, Int, Int) -> Unit
) {
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add New Task",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = taskName.value,
                    onValueChange = { taskName.value = it },
                    label = { Text("What needs to be done?", color = Color(0xFFAAAAAA)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0xFF555555)
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
                        text = "Repeat every day",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Switch(
                        checked = repeatDaily.value,
                        onCheckedChange = { isChecked ->
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
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF444444),
                            uncheckedThumbColor = Color(0xFF777777),
                            uncheckedTrackColor = Color(0xFF222222)
                        )
                    )
                }

                if (repeatDaily.value) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Repeat pattern", color = Color.White, fontSize = 16.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RepeatOption(
                            label = "Daily",
                            selected = repeatMode.value == DailyTaskRepeatMode.EVERY_DAY
                        ) { repeatMode.value = DailyTaskRepeatMode.EVERY_DAY }

                        RepeatOption(
                            label = "Alternate",
                            selected = repeatMode.value == DailyTaskRepeatMode.EVERY_OTHER_DAY
                        ) {
                            repeatMode.value = DailyTaskRepeatMode.EVERY_OTHER_DAY
                        }

                        RepeatOption(
                            label = "Days",
                            selected = repeatMode.value == DailyTaskRepeatMode.SPECIFIC_DAYS
                        ) {
                            repeatMode.value = DailyTaskRepeatMode.SPECIFIC_DAYS
                        }
                    }

                    if (repeatMode.value == DailyTaskRepeatMode.EVERY_OTHER_DAY) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Repeats every 2 days",
                            color = Color(0xFF888888),
                            fontSize = 12.sp
                        )
                    }

                    if (repeatMode.value == DailyTaskRepeatMode.SPECIFIC_DAYS) {
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
                                val selected = DailyTaskWeekdayMask.contains(selectedDaysMask.value, day)
                                WeekdayToggle(
                                    label = dayLabel(day),
                                    selected = selected,
                                    onClick = {
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
                            Text(text = "Task enabled", color = Color.White, fontSize = 16.sp)
                            Text(
                                text = if (dailyEnabled.value) "Will show on scheduled days" else "Hidden until re-enabled",
                                color = Color(0xFF888888),
                                fontSize = 13.sp
                            )
                        }
                        Switch(
                            checked = dailyEnabled.value,
                            onCheckedChange = { dailyEnabled.value = it },
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Limit to date range", color = Color.White, fontSize = 16.sp)
                            Text(
                                text = if (limitDailyRange.value) "Visible only within the window" else "Repeats indefinitely",
                                color = Color(0xFF888888),
                                fontSize = 13.sp
                            )
                        }
                        Switch(
                            checked = limitDailyRange.value,
                            onCheckedChange = { isChecked ->
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
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF444444),
                                uncheckedThumbColor = Color(0xFF666666),
                                uncheckedTrackColor = Color(0xFF222222)
                            )
                        )
                    }

                    if (limitDailyRange.value) {
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showDailyStartPicker.value = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text(
                                text = dailyStartDate.value?.format(dateFormatter) ?: "Select start date",
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showDailyEndPicker.value = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text(
                                text = dailyEndDate.value?.format(dateFormatter) ?: "Select end date (optional)",
                                fontSize = 14.sp
                            )
                        }

                        if (dailyEndDate.value != null) {
                            TextButton(onClick = { dailyEndDate.value = null }) {
                                Text(text = "Clear end date", color = Color(0xFFAAAAAA))
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
                            text = "Set Reminder",
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Switch(
                            checked = enableReminder.value,
                            onCheckedChange = { enableReminder.value = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF444444),
                                uncheckedThumbColor = Color(0xFF777777),
                                uncheckedTrackColor = Color(0xFF222222)
                            )
                        )
                    }

                    if (enableReminder.value) {
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { showDatePicker.value = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = selectedDate.value?.let {
                                    it.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))
                                } ?: "Select Date & Time",
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
                            contentColor = Color(0xFFAAAAAA)
                        )
                    ) {
                        Text("Cancel", fontSize = 16.sp)
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
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        enabled = taskName.value.trim().isNotEmpty()
                    ) {
                        Text("Add Task", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    Text(text = "Set", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDailyStartPicker.value = false }) {
                    Text(text = "Cancel", color = Color(0xFFAAAAAA))
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1A1A))
        ) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1A1A)))
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
                    Text(text = "Set", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDailyEndPicker.value = false }) {
                    Text(text = "Cancel", color = Color(0xFFAAAAAA))
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1A1A))
        ) {
            DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1A1A)))
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
                    Text("Next", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFFAAAAAA))
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    weekdayContentColor = Color(0xFFAAAAAA),
                    subheadContentColor = Color.White,
                    yearContentColor = Color.White,
                    currentYearContentColor = Color.White,
                    selectedYearContentColor = Color.Black,
                    selectedYearContainerColor = Color.White,
                    dayContentColor = Color.White,
                    selectedDayContentColor = Color.Black,
                    selectedDayContainerColor = Color.White,
                    todayContentColor = Color.White,
                    todayDateBorderColor = Color.White
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
                Text("Confirm", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFAAAAAA))
            }
        },
        text = {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Color(0xFF2A2A2A),
                    clockDialSelectedContentColor = Color.Black,
                    clockDialUnselectedContentColor = Color.White,
                    selectorColor = Color.White,
                    containerColor = Color(0xFF1A1A1A),
                    periodSelectorBorderColor = Color(0xFF444444),
                    periodSelectorSelectedContainerColor = Color.White,
                    periodSelectorUnselectedContainerColor = Color(0xFF2A2A2A),
                    periodSelectorSelectedContentColor = Color.Black,
                    periodSelectorUnselectedContentColor = Color.White,
                    timeSelectorSelectedContainerColor = Color.White,
                    timeSelectorUnselectedContainerColor = Color(0xFF2A2A2A),
                    timeSelectorSelectedContentColor = Color.Black,
                    timeSelectorUnselectedContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF1A1A1A)
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
        title = { Text("Edit Task", color = Color.White) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = inputText.value,
                    onValueChange = { inputText.value = it },
                    label = { Text("Task name", color = Color(0xFFAAAAAA)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0xFF555555)
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
                        text = "Reminder",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Switch(
                        checked = enableReminder.value,
                        onCheckedChange = { 
                            enableReminder.value = it
                            if (!it) selectedDate.value = null
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF444444),
                            uncheckedThumbColor = Color(0xFF777777),
                            uncheckedTrackColor = Color(0xFF222222)
                        )
                    )
                }
                
                if (enableReminder.value) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { showDatePicker.value = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = selectedDate.value?.let {
                                it.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))
                            } ?: "Select Date & Time",
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
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = Color(0xFFFFFFFF))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFFAAAAAA))
                }
            }
        },
        containerColor = Color(0xFF1A1A1A)
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
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        ScreenHeader(
            title = "Completed Tasks",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (historyTasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No completed tasks yet",
                    color = Color(0xFF666666),
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
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            task.completedAt?.let { timestamp ->
                                Text(
                                    text = "Completed: ${formatTimestamp(timestamp)}",
                                    color = Color(0xFF777777),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteTask(task.id) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = Color.White
                            )
                        }
                    }
                    androidx.compose.material3.HorizontalDivider(color = Color(0xFF222222), thickness = 1.dp)
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))
}

internal fun dayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
}

@Composable
internal fun RepeatOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) Color.White else Color.Transparent
    val textColor = if (selected) Color.Black else Color.White
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(BorderStroke(1.dp, Color.White), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = label, color = textColor, fontSize = 14.sp)
    }
}

@Composable
internal fun WeekdayToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) Color.White else Color.Transparent
    val textColor = if (selected) Color.Black else Color.White
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(BorderStroke(1.dp, Color.White), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = textColor, fontSize = 12.sp)
    }
}
