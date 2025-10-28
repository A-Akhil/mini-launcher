@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.minifocus.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minifocus.launcher.model.AppEntry
import com.minifocus.launcher.model.BottomIconSlot
import com.minifocus.launcher.model.ClockFormat
import com.minifocus.launcher.model.SearchResult
import com.minifocus.launcher.model.TaskItem
import com.minifocus.launcher.viewmodel.LauncherUiState
import com.minifocus.launcher.viewmodel.NotificationFilterViewModel.FilterUiState
import com.minifocus.launcher.viewmodel.NotificationFilterViewModel.NotificationFilterItem
import com.minifocus.launcher.viewmodel.NotificationInboxViewModel.NotificationInboxUiState
import com.minifocus.launcher.ui.components.MinimalCheckbox
import com.minifocus.launcher.ui.screens.NotificationFilterScreen
import com.minifocus.launcher.ui.screens.NotificationInboxScreen
import com.minifocus.launcher.ui.screens.NotificationSettingsScreen
import kotlinx.coroutines.launch

@Composable
fun LauncherApp(
    state: LauncherUiState,
    notificationInboxState: NotificationInboxUiState,
    notificationFilterState: FilterUiState,
    onToggleTask: (Long) -> Unit,
    onAddTask: (String, Long?) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onPinApp: (String) -> Unit,
    onUnpinApp: (String) -> Unit,
    onHideApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit,
    onLockApp: (String, Long) -> Unit,
    onUnlockApp: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchVisibilityChange: (Boolean) -> Unit,
    onBottomIconChange: (BottomIconSlot, String) -> Unit,
    onSettingsVisibilityChange: (Boolean) -> Unit,
    onHistoryVisibilityChange: (Boolean) -> Unit,
    onClockFormatChange: (ClockFormat) -> Unit,
    onKeyboardSearchOnSwipeChange: (Boolean) -> Unit,
    onShowSecondsChange: (Boolean) -> Unit,
    onNotificationInboxVisibilityChange: (Boolean) -> Unit,
    onNotificationSettingsVisibilityChange: (Boolean) -> Unit,
    onNotificationFilterVisibilityChange: (Boolean) -> Unit,
    onNotificationRetentionSelected: (Int) -> Unit,
    onLogRetentionSelected: (Int) -> Unit,
    onNotificationDelete: (Long) -> Unit,
    onNotificationMarkAllRead: () -> Unit,
    onNotificationUndoDelete: () -> Unit,
    onNotificationUndoConsumed: () -> Unit,
    onNotificationFilterQueryChange: (String) -> Unit,
    onNotificationFilterToggle: (NotificationFilterItem) -> Unit,
    onConsumeMessage: () -> Unit,
    canLaunch: suspend (String) -> Boolean,
    onLaunchApp: (String) -> Unit,
    onOpenClock: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val bottomIconPickerSlot = remember { mutableStateOf<BottomIconSlot?>(null) }
    val searchVisible = state.isSearchVisible
    val shouldShowInlineSearch = state.isKeyboardSearchOnSwipe
    val shouldFocusInlineSearch = shouldShowInlineSearch && pagerState.currentPage == 2
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val showNotificationRetentionDialog = remember { mutableStateOf(false) }
    val showLogRetentionDialog = remember { mutableStateOf(false) }
    val filterBackTarget = remember { mutableStateOf(FilterBackTarget.None) }
    val inboxBackTarget = remember { mutableStateOf(InboxBackTarget.None) }
    val notifSettingsBackTarget = remember { mutableStateOf(NotifSettingsBackTarget.None) }

    fun openInbox(from: InboxBackTarget) {
        inboxBackTarget.value = from
        if (from == InboxBackTarget.Settings) {
            onSettingsVisibilityChange(false)
        }
        onNotificationInboxVisibilityChange(true)
    }

    fun closeInbox() {
        onNotificationInboxVisibilityChange(false)
        when (inboxBackTarget.value) {
            InboxBackTarget.Settings -> onSettingsVisibilityChange(true)
            InboxBackTarget.None -> Unit
        }
        inboxBackTarget.value = InboxBackTarget.None
    }

    fun openNotifSettings(from: NotifSettingsBackTarget) {
        notifSettingsBackTarget.value = from
        if (from == NotifSettingsBackTarget.Settings) {
            onSettingsVisibilityChange(false)
        }
        onNotificationSettingsVisibilityChange(true)
    }

    fun closeNotifSettings() {
        onNotificationSettingsVisibilityChange(false)
        when (notifSettingsBackTarget.value) {
            NotifSettingsBackTarget.Settings -> onSettingsVisibilityChange(true)
            NotifSettingsBackTarget.None -> Unit
        }
        notifSettingsBackTarget.value = NotifSettingsBackTarget.None
    }

    fun openFilters(from: FilterBackTarget) {
        filterBackTarget.value = from
        when (from) {
            FilterBackTarget.Settings -> onSettingsVisibilityChange(false)
            FilterBackTarget.Inbox -> onNotificationInboxVisibilityChange(false)
            FilterBackTarget.NotifSettings -> onNotificationSettingsVisibilityChange(false)
            FilterBackTarget.None -> Unit
        }
        onNotificationFilterVisibilityChange(true)
    }

    fun closeFilters() {
        onNotificationFilterVisibilityChange(false)
        when (filterBackTarget.value) {
            FilterBackTarget.Settings -> onSettingsVisibilityChange(true)
            FilterBackTarget.Inbox -> onNotificationInboxVisibilityChange(true)
            FilterBackTarget.NotifSettings -> onNotificationSettingsVisibilityChange(true)
            FilterBackTarget.None -> Unit
        }
        filterBackTarget.value = FilterBackTarget.None
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 2) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            if (state.searchQuery.isNotEmpty()) {
                onSearchQueryChange("")
            }
        }
    }

    LaunchedEffect(shouldShowInlineSearch, searchVisible, pagerState.currentPage) {
        if (shouldShowInlineSearch && pagerState.currentPage == 2 && searchVisible) {
            onSearchVisibilityChange(false)
        }
    }

    LaunchedEffect(shouldShowInlineSearch) {
        if (!shouldShowInlineSearch && state.searchQuery.isNotEmpty()) {
            onSearchQueryChange("")
        }
    }

    LaunchedEffect(pagerState.currentPage, shouldShowInlineSearch) {
        if (pagerState.currentPage != 2 && shouldShowInlineSearch && state.searchQuery.isNotEmpty()) {
            onSearchQueryChange("")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isNotificationFilterVisible -> {
                    NotificationFilterScreen(
                        state = notificationFilterState,
                        onBack = { closeFilters() },
                        onQueryChange = onNotificationFilterQueryChange,
                        onToggle = onNotificationFilterToggle
                    )
                }
                state.isSettingsVisible -> {
                    SettingsScreen(
                        clockFormat = state.clockFormat,
                        keyboardOnSwipe = state.isKeyboardSearchOnSwipe,
                        showSeconds = state.showSeconds,
                        notificationRetentionDays = state.notificationRetentionDays,
                        logRetentionDays = state.logRetentionDays,
                        bottomLeftApp = state.bottomLeft,
                        bottomRightApp = state.bottomRight,
                        onKeyboardToggle = onKeyboardSearchOnSwipeChange,
                        onClockFormatChange = onClockFormatChange,
                        onShowSecondsToggle = onShowSecondsChange,
                        onBottomIconClick = { slot -> bottomIconPickerSlot.value = slot },
                        onOpenNotificationSettings = { openNotifSettings(NotifSettingsBackTarget.Settings) },
                        onBack = { onSettingsVisibilityChange(false) }
                    )
                }
                state.isNotificationSettingsVisible -> {
                    NotificationSettingsScreen(
                        notificationRetentionDays = state.notificationRetentionDays,
                        logRetentionDays = state.logRetentionDays,
                        onBack = { closeNotifSettings() },
                        onOpenAppFilters = { openFilters(FilterBackTarget.NotifSettings) },
                        onOpenNotificationRetention = { showNotificationRetentionDialog.value = true },
                        onOpenLogRetention = { showLogRetentionDialog.value = true }
                    )
                }
                state.isNotificationInboxVisible -> {
                    NotificationInboxScreen(
                        state = notificationInboxState,
                        onBack = { closeInbox() },
                        onMarkAllRead = onNotificationMarkAllRead,
                        onDelete = onNotificationDelete
                    )
                }
                state.isHistoryVisible -> {
                    HistoryScreen(
                        historyTasks = state.historyTasks,
                        onBack = { onHistoryVisibilityChange(false) },
                        onToggleTask = onToggleTask,
                        onDeleteTask = onDeleteTask
                    )
                }
                else -> {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        when (page) {
                            0 -> TasksScreen(
                                tasks = state.tasks,
                                onToggleTask = onToggleTask,
                                onAddTask = onAddTask,
                                onDeleteTask = onDeleteTask,
                                onOpenHistory = { onHistoryVisibilityChange(true) }
                            )
                            1 -> HomeScreen(
                                state = state,
                                onLaunchApp = { entry ->
                                    handleAppLaunch(
                                        entry,
                                        coroutineScope,
                                        canLaunch,
                                        onLaunchApp,
                                        navigateToHome = { /* Already on home */ }
                                    )
                                },
                                bottomIconPickerSlot = bottomIconPickerSlot,
                                onUnpinApp = onUnpinApp,
                                onHideApp = onHideApp,
                                onLockApp = onLockApp,
                                onUnlockApp = onUnlockApp,
                                onOpenClock = onOpenClock
                            )
                            else -> AllAppsScreen(
                                apps = state.allApps,
                                hiddenApps = state.hiddenApps,
                                keyboardOnSwipe = shouldShowInlineSearch,
                                searchQuery = state.searchQuery,
                                shouldFocusSearch = shouldFocusInlineSearch,
                                unreadCount = notificationInboxState.unreadCount,
                                onQueryChange = onSearchQueryChange,
                                onLaunchApp = { entry ->
                                    handleAppLaunch(
                                        entry,
                                        coroutineScope,
                                        canLaunch,
                                        onLaunchApp,
                                        navigateToHome = {
                                            coroutineScope.launch {
                                                pagerState.scrollToPage(1)
                                            }
                                        }
                                    )
                                },
                                onPinApp = onPinApp,
                                onUnpinApp = onUnpinApp,
                                onHideApp = onHideApp,
                                onUnhideApp = onUnhideApp,
                                onLockApp = onLockApp,
                                onUnlockApp = onUnlockApp,
                                onOpenNotificationInbox = { openInbox(InboxBackTarget.None) },
                                onOpenSettings = { onSettingsVisibilityChange(true) }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = searchVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SearchOverlay(
                    query = state.searchQuery,
                    results = state.searchResults,
                    onQueryChange = onSearchQueryChange,
                    autoFocus = state.isSearchVisible,
                    onResultClick = { entry ->
                        coroutineScope.launch {
                            if (canLaunch(entry.packageName)) {
                                onSearchVisibilityChange(false)
                                onLaunchApp(entry.packageName)
                            }
                            // If app is locked, simply don't launch (no snackbar message)
                        }
                    }
                )
            }

            if (showNotificationRetentionDialog.value) {
                RetentionPickerDialog(
                    title = "Notification retention",
                    options = listOf(1, 2, 7, 14, 30),
                    selected = state.notificationRetentionDays,
                    onSelect = { days ->
                        onNotificationRetentionSelected(days)
                        showNotificationRetentionDialog.value = false
                    },
                    onDismiss = { showNotificationRetentionDialog.value = false }
                )
            }

            if (showLogRetentionDialog.value) {
                RetentionPickerDialog(
                    title = "Log retention",
                    options = listOf(7, 30, 60, 90),
                    selected = state.logRetentionDays,
                    onSelect = { days ->
                        onLogRetentionSelected(days)
                        showLogRetentionDialog.value = false
                    },
                    onDismiss = { showLogRetentionDialog.value = false }
                )
            }

            BottomIconPickerDialog(
                slot = bottomIconPickerSlot.value,
                apps = state.allApps,
                onDismiss = { bottomIconPickerSlot.value = null },
                onSelect = { slot, packageName ->
                    onBottomIconChange(slot, packageName)
                    bottomIconPickerSlot.value = null
                }
            )
        }
    }
}

private fun handleAppLaunch(
    entry: AppEntry,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    canLaunch: suspend (String) -> Boolean,
    onLaunchApp: (String) -> Unit,
    navigateToHome: () -> Unit
) {
    coroutineScope.launch {
        if (canLaunch(entry.packageName)) {
            onLaunchApp(entry.packageName)
            // Small delay to allow UI to settle before navigation
            kotlinx.coroutines.delay(50)
            navigateToHome()  // Navigate back to home after launching app
        }
        // If app is locked, simply don't launch (no snackbar message)
    }
}

private fun unreadCountLabel(count: Int): String = if (count > 99) "99+" else count.toString()

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp
                )
            }
        }
        Text(
            text = "→",
            color = Color(0xFFAAAAAA),
            fontSize = 20.sp
        )
    }
}

private fun pluralizeDays(days: Int): String = if (days == 1) "1 day" else "$days days"

private enum class FilterBackTarget { None, Settings, Inbox, NotifSettings }
private enum class InboxBackTarget { None, Settings }
private enum class NotifSettingsBackTarget { None, Settings }

@Composable
private fun RetentionPickerDialog(
    title: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        title = {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { option ->
                    val isSelected = option == selected
                    TextButton(
                        onClick = { onSelect(option) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isSelected) Color.Black else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color.White else Color(0x22FFFFFF))
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = pluralizeDays(option),
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = Color(0xFFAAAAAA))
            }
        }
    )
}

@Composable
private fun HomeScreen(
    state: LauncherUiState,
    onLaunchApp: (AppEntry) -> Unit,
    bottomIconPickerSlot: MutableState<BottomIconSlot?>,
    onUnpinApp: (String) -> Unit,
    onHideApp: (String) -> Unit,
    onLockApp: (String, Long) -> Unit,
    onUnlockApp: (String) -> Unit,
    onOpenClock: () -> Unit
) {
    val expandedPinned = remember { mutableStateOf<String?>(null) }
    val lockDialogApp = remember { mutableStateOf<AppEntry?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 32.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.timeFormatted,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.combinedClickable(
                    onClick = { onOpenClock() }
                )
            )
            Text(
                text = state.dateFormatted,
                fontSize = 18.sp,
                color = Color(0xFFAAAAAA),
                textAlign = TextAlign.Center
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(state.pinnedApps) { app ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = app.label,
                        fontSize = 22.sp,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .combinedClickable(
                                onClick = { onLaunchApp(app) },
                                onLongClick = { expandedPinned.value = app.packageName }
                            ),
                        textAlign = TextAlign.Center
                    )
                    DropdownMenu(
                        expanded = expandedPinned.value == app.packageName,
                        onDismissRequest = { expandedPinned.value = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Unpin") },
                            onClick = {
                                onUnpinApp(app.packageName)
                                expandedPinned.value = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hide") },
                            onClick = {
                                onHideApp(app.packageName)
                                expandedPinned.value = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Lock") },
                            onClick = {
                                lockDialogApp.value = app
                                expandedPinned.value = null
                            }
                        )
                        if (app.isLocked) {
                            DropdownMenuItem(
                                text = { Text("Unlock") },
                                onClick = {
                                    onUnlockApp(app.packageName)
                                    expandedPinned.value = null
                                }
                            )
                        }
                    }
                }
            }
        }

        BottomIconRow(
            left = state.bottomLeft,
            right = state.bottomRight,
            onLaunch = onLaunchApp,
            onLongPress = { slot -> bottomIconPickerSlot.value = slot }
        )
    }
    
    // Lock duration dialog
    lockDialogApp.value?.let { app ->
        LockDurationDialog(
            appName = app.label,
            onDismiss = { lockDialogApp.value = null },
            onLock = { minutes ->
                onLockApp(app.packageName, minutes)
                lockDialogApp.value = null
            }
        )
    }
}

@Composable
private fun BottomIconRow(
    left: AppEntry?,
    right: AppEntry?,
    onLaunch: (AppEntry) -> Unit,
    onLongPress: (BottomIconSlot) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BottomIconButton(
            label = left?.label ?: "Phone",
            onClick = { left?.let(onLaunch) },
            onLongPress = { onLongPress(BottomIconSlot.LEFT) }
        )
        BottomIconButton(
            label = right?.label ?: "Camera",
            onClick = { right?.let(onLaunch) },
            onLongPress = { onLongPress(BottomIconSlot.RIGHT) }
        )
    }
}

@Composable
private fun BottomIconButton(
    label: String,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Text(
        text = label.uppercase(),
        color = Color.White,
        modifier = Modifier
            .padding(16.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    )
}

@Composable
private fun TasksScreen(
    tasks: List<TaskItem>,
    onToggleTask: (Long) -> Unit,
    onAddTask: (String, Long?) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onOpenHistory: () -> Unit
) {
    val showAddDialog = remember { mutableStateOf(false) }
    val showEditDialog = remember { mutableStateOf<TaskItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tasks",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onOpenHistory) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "History",
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tasks, key = { it.id }) { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .combinedClickable(
                                onClick = { onToggleTask(task.id) },
                                onLongClick = {
                                    showEditDialog.value = task
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MinimalCheckbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggleTask(task.id) }
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        ) {
                            Text(
                                text = task.title,
                                color = if (task.isCompleted) Color(0xFF777777) else Color.White,
                                fontSize = 16.sp
                            )
                            task.scheduledFor?.let { timestamp ->
                                Text(
                                    text = formatScheduledTime(timestamp),
                                    color = Color(0xFF888888),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog.value = true },
            containerColor = Color.White,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Task")
        }
    }

    // Fancy Add Task Dialog
    if (showAddDialog.value) {
        FancyAddTaskDialog(
            onDismiss = { showAddDialog.value = false },
            onAdd = { title, scheduledTime ->
                onAddTask(title, scheduledTime)
                showAddDialog.value = false
            }
        )
    }

    // Edit/Delete Task Dialog
    showEditDialog.value?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { showEditDialog.value = null },
            onSave = { newTitle, scheduledTime ->
                onDeleteTask(task.id)
                onAddTask(newTitle, scheduledTime)
                showEditDialog.value = null
            },
            onDelete = {
                onDeleteTask(task.id)
                showEditDialog.value = null
            }
        )
    }
}

private fun formatScheduledTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val dateTime = java.time.LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(timestamp), 
        java.time.ZoneId.systemDefault()
    )
    val today = java.time.LocalDate.now()
    val taskDate = dateTime.toLocalDate()
    
    return when {
        taskDate == today -> "Today at ${dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}"
        taskDate == today.plusDays(1) -> "Tomorrow at ${dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}"
        timestamp < now -> "⚠ ${dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd 'at' HH:mm"))}"
        else -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd 'at' HH:mm"))
    }
}

// Smart search: prioritizes better matches
// 1. Starts with query (highest priority)
// 2. Word boundary match (query starts a word)
// 3. Contains as substring
// 4. Fuzzy match (scattered chars)
private fun fuzzyMatch(label: String, query: String): Boolean {
    if (query.isEmpty()) return true
    val lowerLabel = label.lowercase()
    val lowerQuery = query.lowercase().filter { it.isLetterOrDigit() }
    
    if (lowerQuery.isEmpty()) return true
    
    // 1. Exact prefix match
    if (lowerLabel.startsWith(lowerQuery)) return true
    
    // 2. Word boundary match (query starts a word)
    val words = lowerLabel.split(" ", "-", "_", ".", "&")
    if (words.any { it.startsWith(lowerQuery) }) return true
    
    // 3. Substring match
    if (lowerLabel.contains(lowerQuery)) return true
    
    // 4. Fuzzy scattered match (last resort)
    var queryIndex = 0
    for (char in lowerLabel) {
        if (queryIndex < lowerQuery.length && char == lowerQuery[queryIndex]) {
            queryIndex++
        }
    }
    
    return queryIndex == lowerQuery.length
}

// Calculate match score (lower is better)
// Used to sort search results by relevance
private fun matchScore(label: String, query: String): Int {
    val lowerLabel = label.lowercase()
    val lowerQuery = query.lowercase().filter { it.isLetterOrDigit() }
    
    if (lowerQuery.isEmpty()) return 999
    
    // 1. Exact match (score 0)
    if (lowerLabel == lowerQuery) return 0
    
    // 2. Starts with query (score 1)
    if (lowerLabel.startsWith(lowerQuery)) return 1
    
    // 3. Word boundary match (score 2)
    val words = lowerLabel.split(" ", "-", "_", ".", "&")
    if (words.any { it.startsWith(lowerQuery) }) return 2
    
    // 4. Contains as substring (score 3)
    if (lowerLabel.contains(lowerQuery)) return 3
    
    // 5. Fuzzy match (score 4)
    return 4
}

@Composable
private fun AllAppsScreen(
    apps: List<AppEntry>,
    hiddenApps: List<AppEntry>,
    keyboardOnSwipe: Boolean,
    searchQuery: String,
    shouldFocusSearch: Boolean,
    unreadCount: Int,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onPinApp: (String) -> Unit,
    onUnpinApp: (String) -> Unit,
    onHideApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit,
    onLockApp: (String, Long) -> Unit,
    onUnlockApp: (String) -> Unit,
    onOpenNotificationInbox: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val expandedApp = remember { mutableStateOf<String?>(null) }
    val lockDialogApp = remember { mutableStateOf<AppEntry?>(null) }
    val blinkingApp = remember { mutableStateOf<String?>(null) }
    val appToLaunch = remember { mutableStateOf<AppEntry?>(null) }
    val searchActive = searchQuery.isNotBlank()
    val filteredApps = remember(apps, searchQuery) {
        if (searchActive) {
            apps.filter { fuzzyMatch(it.label, searchQuery) }
                .sortedWith(compareBy(
                    { matchScore(it.label, searchQuery) },
                    { it.label.lowercase() }
                ))
        } else {
            apps
        }
    }
    val filteredHiddenApps = remember(hiddenApps, searchQuery) {
        if (searchActive) {
            hiddenApps.filter { fuzzyMatch(it.label, searchQuery) }
                .sortedWith(compareBy(
                    { matchScore(it.label, searchQuery) },
                    { it.label.lowercase() }
                ))
        } else {
            hiddenApps
        }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(keyboardOnSwipe) {
        if (!keyboardOnSwipe) {
            keyboardController?.hide()
        }
    }

    LaunchedEffect(shouldFocusSearch, keyboardOnSwipe) {
        if (keyboardOnSwipe && shouldFocusSearch) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else if (!shouldFocusSearch) {
            keyboardController?.hide()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "All Apps",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenNotificationInbox) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notification inbox",
                            tint = Color.White
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 3.dp, end = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.Red, CircleShape)
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unreadCountLabel(unreadCount),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (searchQuery.isBlank()) {
                    Text(
                        text = "Search apps",
                        color = Color(0x88FFFFFF)
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val firstApp = filteredApps.firstOrNull()
                            if (firstApp != null) {
                                appToLaunch.value = firstApp
                                blinkingApp.value = firstApp.packageName
                                keyboardController?.hide()
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            }
            
            // Blink effect and launch
            LaunchedEffect(appToLaunch.value) {
                appToLaunch.value?.let { app ->
                    // Blink 3 times
                    blinkingApp.value = app.packageName
                    kotlinx.coroutines.delay(150)
                    blinkingApp.value = null
                    kotlinx.coroutines.delay(150)
                    blinkingApp.value = app.packageName
                    kotlinx.coroutines.delay(150)
                    blinkingApp.value = null
                    kotlinx.coroutines.delay(150)
                    blinkingApp.value = app.packageName
                    kotlinx.coroutines.delay(150)
                    blinkingApp.value = null
                    
                    // Launch the app
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    onQueryChange("")
                    onLaunchApp(app)
                    appToLaunch.value = null
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (filteredApps.isEmpty() && filteredHiddenApps.isEmpty()) {
            item {
                Text(
                    text = "No apps match your search",
                    color = Color(0xFF666666),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(filteredApps) { app ->
                val isBlinking = blinkingApp.value == app.packageName
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = app.label,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .alpha(if (isBlinking) 0.3f else 1f)
                            .combinedClickable(
                                onClick = { onLaunchApp(app) },
                                onLongClick = { expandedApp.value = app.packageName }
                            )
                    )
                    DropdownMenu(
                        expanded = expandedApp.value == app.packageName,
                        onDismissRequest = { expandedApp.value = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (app.isPinned) "Unpin" else "Pin") },
                            onClick = {
                                if (app.isPinned) onUnpinApp(app.packageName) else onPinApp(app.packageName)
                                expandedApp.value = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hide") },
                            onClick = {
                                onHideApp(app.packageName)
                                expandedApp.value = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Lock") },
                            onClick = {
                                lockDialogApp.value = app
                                expandedApp.value = null
                            }
                        )
                        if (app.isLocked) {
                            DropdownMenuItem(
                                text = { Text("Unlock") },
                                onClick = {
                                    onUnlockApp(app.packageName)
                                    expandedApp.value = null
                                }
                            )
                        }
                    }
                }
            }
            if (filteredHiddenApps.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "Hidden Apps", color = Color.Gray, fontSize = 20.sp)
                }
                items(filteredHiddenApps) { app ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = app.label,
                            color = Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .combinedClickable(
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { expandedApp.value = app.packageName }
                                )
                        )
                        DropdownMenu(
                            expanded = expandedApp.value == app.packageName,
                            onDismissRequest = { expandedApp.value = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Unhide") },
                                onClick = {
                                    onUnhideApp(app.packageName)
                                    expandedApp.value = null
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Unlock") },
                                onClick = {
                                    onUnlockApp(app.packageName)
                                    expandedApp.value = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Lock duration dialog
    lockDialogApp.value?.let { app ->
        LockDurationDialog(
            appName = app.label,
            onDismiss = { lockDialogApp.value = null },
            onLock = { minutes ->
                onLockApp(app.packageName, minutes)
                lockDialogApp.value = null
            }
        )
    }
}

@Composable
private fun SearchOverlay(
    query: String,
    results: List<SearchResult>,
    onQueryChange: (String) -> Unit,
    autoFocus: Boolean = false,
    onResultClick: (AppEntry) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.TopCenter)) {
            val focusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            LaunchedEffect(autoFocus) {
                if (autoFocus) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                } else {
                    keyboardController?.hide()
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(results) { result ->
                    when (result) {
                        is SearchResult.App -> Text(
                            text = result.entry.label,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .combinedClickable(
                                    onClick = { onResultClick(result.entry) },
                                    onLongClick = {}
                                )
                        )
                        is SearchResult.Task -> Text(
                            text = result.item.title,
                            color = Color(0xFF888888),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                        is SearchResult.Command -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomIconPickerDialog(
    slot: BottomIconSlot?,
    apps: List<AppEntry>,
    onDismiss: () -> Unit,
    onSelect: (BottomIconSlot, String) -> Unit
) {
    if (slot == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Assign App") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                items(apps) { app ->
                    Text(
                        text = app.label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .combinedClickable(
                                onClick = { onSelect(slot, app.packageName) },
                                onLongClick = { onSelect(slot, app.packageName) }
                            )
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun SettingsScreen(
    clockFormat: ClockFormat,
    keyboardOnSwipe: Boolean,
    showSeconds: Boolean,
    notificationRetentionDays: Int,
    logRetentionDays: Int,
    bottomLeftApp: AppEntry?,
    bottomRightApp: AppEntry?,
    onKeyboardToggle: (Boolean) -> Unit,
    onClockFormatChange: (ClockFormat) -> Unit,
    onShowSecondsToggle: (Boolean) -> Unit,
    onBottomIconClick: (BottomIconSlot) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        // Header with back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "←",
                color = Color.White,
                fontSize = 32.sp,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .combinedClickable(onClick = onBack)
            )
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Keyboard on swipe setting
        Text(
            text = "Keyboard on All Apps swipe",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Open search and keyboard when swiping to All Apps",
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = keyboardOnSwipe, onCheckedChange = onKeyboardToggle)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Show seconds setting
        Text(
            text = "Show seconds in clock",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Display seconds on home screen clock",
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = showSeconds, onCheckedChange = onShowSecondsToggle)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Notification settings
        Text(
            text = "Notifications",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        SettingsRow(
            title = "Notification Settings",
            subtitle = "Inbox, filters, and retention",
            onClick = onOpenNotificationSettings
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom icons customization
        Text(
            text = "Bottom Quick Launch Icons",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        // Left bottom icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .combinedClickable(onClick = { onBottomIconClick(BottomIconSlot.LEFT) })
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Left Icon",
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = bottomLeftApp?.label ?: "None",
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp
                )
            }
            Text(
                text = "→",
                color = Color(0xFFAAAAAA),
                fontSize = 20.sp
            )
        }
        
        // Right bottom icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .combinedClickable(onClick = { onBottomIconClick(BottomIconSlot.RIGHT) })
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Right Icon",
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = bottomRightApp?.label ?: "None",
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp
                )
            }
            Text(
                text = "→",
                color = Color(0xFFAAAAAA),
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Clock format setting
        Text(
            text = "Clock Format",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
        ClockFormat.values().forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .combinedClickable(onClick = { onClockFormatChange(option) })
            ) {
                RadioButton(
                    selected = clockFormat == option,
                    onClick = { onClockFormatChange(option) }
                )
                Text(
                    text = when (option) {
                        ClockFormat.H24 -> "24-hour"
                        ClockFormat.H12 -> "12-hour"
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

private suspend fun PointerInputScope.detectSwipeUp(onSwipeUp: () -> Unit) {
    var totalDrag = 0f
    detectVerticalDragGestures(
        onDragStart = { totalDrag = 0f },
        onDragEnd = {
            if (totalDrag < -120f) {
                onSwipeUp()
            }
            totalDrag = 0f
        },
        onDragCancel = { totalDrag = 0f }
    ) { _, dragAmount ->
        totalDrag += dragAmount
    }
}

@Composable
private fun LockDurationDialog(
    appName: String,
    onDismiss: () -> Unit,
    onLock: (Long) -> Unit
) {
    val predefinedDurations = listOf(
        "30min" to 30L,
        "1hr" to 60L,
        "2hr" to 120L,
        "4hr" to 240L,
        "8hr" to 480L,
        "10hr" to 600L,
        "12hr" to 720L,
        "24hr" to 1440L,
        "48hr" to 2880L,
        "5days" to 7200L,
        "10days" to 14400L,
        "14days" to 20160L,
        "20days" to 28800L,
        "25days" to 36000L,
        "31days" to 44640L
    )
    
    val sliderPosition = remember { mutableFloatStateOf(0f) }
    val customHours = remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        title = {
            Text(
                text = "Lock $appName",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = predefinedDurations[sliderPosition.floatValue.toInt()].first,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Slider(
                    value = sliderPosition.floatValue,
                    onValueChange = { sliderPosition.floatValue = it },
                    valueRange = 0f..(predefinedDurations.size - 1).toFloat(),
                    steps = predefinedDurations.size - 2,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0xFF333333)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Or enter custom hours:",
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = customHours.value,
                    onValueChange = { 
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            customHours.value = it
                        }
                    },
                    placeholder = { Text("Enter hours", color = Color(0xFF555555)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0xFF555555),
                        cursorColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minutes = if (customHours.value.isNotEmpty()) {
                        customHours.value.toLongOrNull()?.times(60) ?: 0L
                    } else {
                        predefinedDurations[sliderPosition.floatValue.toInt()].second
                    }
                    if (minutes > 0) {
                        onLock(minutes)
                        onDismiss()
                    }
                }
            ) {
                Text("Lock", color = Color.White, fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFAAAAAA), fontSize = 16.sp)
            }
        }
    )
}
