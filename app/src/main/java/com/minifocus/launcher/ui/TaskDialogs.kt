package com.minifocus.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.minifocus.launcher.model.TaskItem
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FancyAddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Long?) -> Unit
) {
    val taskName = remember { mutableStateOf("") }
    val enableReminder = remember { mutableStateOf(false) }
    val selectedDate = remember { mutableStateOf<LocalDateTime?>(null) }
    val showDatePicker = remember { mutableStateOf(false) }

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
                            if (title.isNotEmpty()) {
                                val scheduledTime = if (enableReminder.value && selectedDate.value != null) {
                                    selectedDate.value!!.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                } else null
                                onAdd(title, scheduledTime)
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
            Column {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Completed Tasks",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

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
            androidx.compose.foundation.lazy.LazyColumn {
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
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggleTask(task.id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.White,
                                uncheckedColor = Color(0xFF555555),
                                checkmarkColor = Color.Black
                            )
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
                    Divider(color = Color(0xFF222222), thickness = 1.dp)
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))
}
