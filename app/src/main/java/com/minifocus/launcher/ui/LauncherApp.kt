@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.minifocus.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.minifocus.launcher.model.AppEntry
import com.minifocus.launcher.model.BottomIconSlot
import com.minifocus.launcher.model.ClockFormat
import com.minifocus.launcher.model.SearchResult
import com.minifocus.launcher.model.DailyTaskItem
import com.minifocus.launcher.model.DailyTaskRepeatMode
import com.minifocus.launcher.model.TaskItem
import com.minifocus.launcher.model.patternLabel
import com.minifocus.launcher.viewmodel.LauncherUiState
import com.minifocus.launcher.viewmodel.NotificationFilterViewModel.FilterUiState
import com.minifocus.launcher.viewmodel.NotificationFilterViewModel.NotificationFilterItem
import com.minifocus.launcher.viewmodel.NotificationInboxViewModel.NotificationInboxUiState
import com.minifocus.launcher.ui.components.AppContextMenu
import com.minifocus.launcher.ui.components.MinimalCheckbox
import com.minifocus.launcher.ui.components.ScreenHeader
import com.minifocus.launcher.ui.screens.AboutScreen
import com.minifocus.launcher.ui.screens.EmergencyUnlockScreen
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
    onAddDailyTask: (String, Long?, Long?, Boolean, DailyTaskRepeatMode, Int, Int) -> Unit,
    onUpdateDailyTask: (Long, String, Long?, Long?, Boolean, DailyTaskRepeatMode, Int, Int) -> Unit,
    onDeleteDailyTask: (Long) -> Unit,
    onDailyTaskEnabledChange: (Long, Boolean) -> Unit,
    onDailyTaskCompleted: (Long) -> Unit,
    onDailyTaskReset: (Long) -> Unit,
    onPinApp: (String) -> Unit,
    onUnpinApp: (String) -> Unit,
    onHideApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit,
    onLockApp: (String, Long) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchVisibilityChange: (Boolean) -> Unit,
    onBottomIconChange: (BottomIconSlot, String) -> Unit,
    onSettingsVisibilityChange: (Boolean) -> Unit,
    onHomeSettingsVisibilityChange: (Boolean) -> Unit,
    onClockSettingsVisibilityChange: (Boolean) -> Unit,
    onAppDrawerSettingsVisibilityChange: (Boolean) -> Unit,
    onAboutVisibilityChange: (Boolean) -> Unit,
    onEmergencyUnlockVisibilityChange: (Boolean) -> Unit,
    onHistoryVisibilityChange: (Boolean) -> Unit,
    onClockFormatChange: (ClockFormat) -> Unit,
    onKeyboardSearchOnSwipeChange: (Boolean) -> Unit,
    onShowSecondsChange: (Boolean) -> Unit,
    onShowDailyTasksOnHomeChange: (Boolean) -> Unit,
    onNotificationInboxEnabledChange: (Boolean) -> Unit,
    onNotificationInboxVisibilityChange: (Boolean) -> Unit,
    onNotificationSettingsVisibilityChange: (Boolean) -> Unit,
    onNotificationFilterVisibilityChange: (Boolean) -> Unit,
    onNotificationRetentionSelected: (Int) -> Unit,
    onLogRetentionSelected: (Int) -> Unit,
    onNotificationDelete: (Long) -> Unit,
    onNotificationMarkAllRead: () -> Unit,
    onNotificationFilterQueryChange: (String) -> Unit,
    onNotificationFilterToggle: (NotificationFilterItem) -> Unit,
    canLaunch: suspend (String) -> Boolean,
    onLaunchApp: (String) -> Unit,
    onOpenClock: () -> Unit,
    lockManager: com.minifocus.launcher.manager.LockManager,
    onRootBack: () -> Unit = {}
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
    val homeSettingsBackTarget = remember { mutableStateOf(HomeSettingsBackTarget.None) }
    val clockSettingsBackTarget = remember { mutableStateOf(ClockSettingsBackTarget.None) }
    val appDrawerSettingsBackTarget = remember { mutableStateOf(AppDrawerSettingsBackTarget.None) }

    val shouldSnapToHome = state.isSettingsVisible ||
        state.isHomeSettingsVisible ||
        state.isClockSettingsVisible ||
        state.isNotificationSettingsVisible ||
        state.isNotificationFilterVisible ||
        state.isNotificationInboxVisible ||
        state.isAboutVisible ||
        state.isEmergencyUnlockVisible ||
        state.isHistoryVisible ||
        state.isSearchVisible

    LaunchedEffect(state.homeResetTick) {
        if (pagerState.currentPage != 1) {
            pagerState.scrollToPage(1)
        }
    }

    LaunchedEffect(shouldSnapToHome) {
        if (shouldSnapToHome && pagerState.currentPage != 1) {
            pagerState.scrollToPage(1)
        }
    }

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

    fun openHomeSettings(from: HomeSettingsBackTarget) {
        homeSettingsBackTarget.value = from
        if (from == HomeSettingsBackTarget.Settings) {
            onSettingsVisibilityChange(false)
        }
        onHomeSettingsVisibilityChange(true)
    }

    fun closeHomeSettings() {
        onHomeSettingsVisibilityChange(false)
        when (homeSettingsBackTarget.value) {
            HomeSettingsBackTarget.Settings -> onSettingsVisibilityChange(true)
            HomeSettingsBackTarget.None -> Unit
        }
        homeSettingsBackTarget.value = HomeSettingsBackTarget.None
    }

    fun openClockSettings(from: ClockSettingsBackTarget) {
        clockSettingsBackTarget.value = from
        if (from == ClockSettingsBackTarget.Settings) {
            onSettingsVisibilityChange(false)
        }
        onClockSettingsVisibilityChange(true)
    }

    fun closeClockSettings() {
        onClockSettingsVisibilityChange(false)
        when (clockSettingsBackTarget.value) {
            ClockSettingsBackTarget.Settings -> onSettingsVisibilityChange(true)
            ClockSettingsBackTarget.None -> Unit
        }
        clockSettingsBackTarget.value = ClockSettingsBackTarget.None
    }

    fun openAppDrawerSettings(from: AppDrawerSettingsBackTarget) {
        appDrawerSettingsBackTarget.value = from
        if (from == AppDrawerSettingsBackTarget.Settings) {
            onSettingsVisibilityChange(false)
        }
        onAppDrawerSettingsVisibilityChange(true)
    }

    fun closeAppDrawerSettings() {
        onAppDrawerSettingsVisibilityChange(false)
        when (appDrawerSettingsBackTarget.value) {
            AppDrawerSettingsBackTarget.Settings -> onSettingsVisibilityChange(true)
            AppDrawerSettingsBackTarget.None -> Unit
        }
        appDrawerSettingsBackTarget.value = AppDrawerSettingsBackTarget.None
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

    BackHandler {
        when {
            state.isNotificationFilterVisible -> closeFilters()
            state.isNotificationSettingsVisible -> closeNotifSettings()
            state.isClockSettingsVisible -> closeClockSettings()
            state.isAppDrawerSettingsVisible -> closeAppDrawerSettings()
            state.isHomeSettingsVisible -> closeHomeSettings()
            state.isNotificationInboxVisible -> closeInbox()
            state.isSettingsVisible -> onSettingsVisibilityChange(false)
            state.isHistoryVisible -> onHistoryVisibilityChange(false)
            state.isAboutVisible -> onAboutVisibilityChange(false)
            state.isEmergencyUnlockVisible -> onEmergencyUnlockVisibilityChange(false)
            searchVisible -> onSearchVisibilityChange(false)
            pagerState.currentPage != 1 -> {
                coroutineScope.launch { pagerState.scrollToPage(1) }
            }
            else -> onRootBack()
        }
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
                        showSeconds = state.showSeconds,
                        showDailyTasksOnHome = state.showDailyTasksOnHome,
                        bottomLeftApp = state.bottomLeft,
                        bottomRightApp = state.bottomRight,
                        keyboardOnSwipe = state.isKeyboardSearchOnSwipe,
                        notificationInboxEnabled = state.notificationInboxEnabled,
                        notificationRetentionDays = state.notificationRetentionDays,
                        logRetentionDays = state.logRetentionDays,
                        onOpenHomeSettings = { openHomeSettings(HomeSettingsBackTarget.Settings) },
                        onOpenClockSettings = { openClockSettings(ClockSettingsBackTarget.Settings) },
                        onOpenAppDrawerSettings = { openAppDrawerSettings(AppDrawerSettingsBackTarget.Settings) },
                        onOpenNotificationSettings = { openNotifSettings(NotifSettingsBackTarget.Settings) },
                        onOpenAbout = { onAboutVisibilityChange(true) },
                        onBack = { onSettingsVisibilityChange(false) }
                    )
                }
                state.isHomeSettingsVisible -> {
                    HomeSettingsScreen(
                        showDailyTasksOnHome = state.showDailyTasksOnHome,
                        showDailyTasksHomeSection = state.showDailyTasksHomeSection,
                        bottomLeftApp = state.bottomLeft,
                        bottomRightApp = state.bottomRight,
                        onToggleDailyTasksOnHome = onShowDailyTasksOnHomeChange,
                        onBottomIconClick = { slot -> bottomIconPickerSlot.value = slot },
                        onBack = { closeHomeSettings() }
                    )
                }
                state.isClockSettingsVisible -> {
                    ClockSettingsScreen(
                        clockFormat = state.clockFormat,
                        showSeconds = state.showSeconds,
                        onClockFormatChange = onClockFormatChange,
                        onShowSecondsToggle = onShowSecondsChange,
                        onBack = { closeClockSettings() }
                    )
                }
                state.isAppDrawerSettingsVisible -> {
                    AppDrawerSettingsScreen(
                        keyboardOnSwipe = state.isKeyboardSearchOnSwipe,
                        onKeyboardToggle = onKeyboardSearchOnSwipeChange,
                        onBack = { closeAppDrawerSettings() }
                    )
                }
                state.isNotificationSettingsVisible -> {
                    NotificationSettingsScreen(
                        notificationInboxEnabled = state.notificationInboxEnabled,
                        notificationRetentionDays = state.notificationRetentionDays,
                        logRetentionDays = state.logRetentionDays,
                        onBack = { closeNotifSettings() },
                        onOpenAppFilters = { openFilters(FilterBackTarget.NotifSettings) },
                        onOpenNotificationRetention = { showNotificationRetentionDialog.value = true },
                        onOpenLogRetention = { showLogRetentionDialog.value = true },
                        onNotificationInboxToggle = onNotificationInboxEnabledChange
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
                state.isAboutVisible -> {
                    AboutScreen(
                        onBack = { onAboutVisibilityChange(false) },
                        onNavigateToEmergencyUnlock = { onEmergencyUnlockVisibilityChange(true) }
                    )
                }
                state.isEmergencyUnlockVisible -> {
                    EmergencyUnlockScreen(
                        lockManager = lockManager,
                        onBack = { onEmergencyUnlockVisibilityChange(false) },
                        onUnlockApp = lockManager::unlockApp
                    )
                }
                else -> {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        when (page) {
                            0 -> TasksScreen(
                                tasks = state.tasks,
                                dailyTasks = state.dailyTasks,
                                heldDailyTaskIds = state.heldDailyTaskIds,
                                onToggleTask = onToggleTask,
                                onAddTask = onAddTask,
                                onDeleteTask = onDeleteTask,
                                onOpenHistory = { onHistoryVisibilityChange(true) },
                                onAddDailyTask = onAddDailyTask,
                                onUpdateDailyTask = onUpdateDailyTask,
                                onDeleteDailyTask = onDeleteDailyTask,
                                onDailyTaskEnabledChange = onDailyTaskEnabledChange,
                                onDailyTaskCompleted = onDailyTaskCompleted,
                                onDailyTaskReset = onDailyTaskReset
                            )
                            1 -> HomeScreen(
                                state = state,
                                onLaunchApp = { entry ->
                                    handleAppLaunch(
                                        entry,
                                        coroutineScope,
                                        onLaunchApp,
                                        navigateToHome = { /* Already on home */ }
                                    )
                                },
                                bottomIconPickerSlot = bottomIconPickerSlot,
                                onUnpinApp = onUnpinApp,
                                onHideApp = onHideApp,
                                onLockApp = onLockApp,
                                onOpenClock = onOpenClock,
                                onDailyTaskCompleted = onDailyTaskCompleted,
                                onDailyTaskReset = onDailyTaskReset
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
    onLaunchApp: (String) -> Unit,
    navigateToHome: () -> Unit
) {
    coroutineScope.launch {
        // Always call onLaunchApp - it will handle locked apps by showing overlay
        onLaunchApp(entry.packageName)
        // Small delay to allow UI to settle before navigation
        kotlinx.coroutines.delay(50)
        navigateToHome()  // Navigate back to home after launching app
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
private enum class HomeSettingsBackTarget { None, Settings }
private enum class ClockSettingsBackTarget { None, Settings }
private enum class AppDrawerSettingsBackTarget { None, Settings }

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
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
    onOpenClock: () -> Unit,
    onDailyTaskCompleted: (Long) -> Unit,
    onDailyTaskReset: (Long) -> Unit
) {
    val expandedPinned = remember { mutableStateOf<String?>(null) }
    val lockDialogApp = remember { mutableStateOf<AppEntry?>(null) }
    val activeDailyTasks = remember(state.dailyTasks, state.heldDailyTaskIds) {
        state.dailyTasks.filter { task ->
            task.isActiveToday && task.isEnabled && (!task.isCompletedToday || task.id in state.heldDailyTaskIds)
        }
    }
    val hasDailyTasksForHome = state.showDailyTasksHomeSection && activeDailyTasks.isNotEmpty()
    val pinnedLimit = if (hasDailyTasksForHome) 3 else 5
    val pinnedToShow = remember(state.pinnedApps, pinnedLimit) {
        state.pinnedApps.take(pinnedLimit)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 32.dp, vertical = 48.dp),
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

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically)
            ) {
                if (hasDailyTasksForHome) {
                    DailyTasksHomeSection(
                        dailyTasks = activeDailyTasks,
                        onDailyTaskCompleted = onDailyTaskCompleted,
                        onDailyTaskReset = onDailyTaskReset
                    )
                }

                if (pinnedToShow.isNotEmpty()) {
                    PinnedAppsHomeSection(
                        pinnedApps = pinnedToShow,
                        onLaunchApp = onLaunchApp,
                        onLongPress = { app -> expandedPinned.value = app.packageName }
                    )
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
    
    // Context menu for pinned apps
    expandedPinned.value?.let { pkgName ->
        state.pinnedApps.find { it.packageName == pkgName }?.let { app ->
            AppContextMenu(
                app = app,
                onDismiss = { expandedPinned.value = null },
                onUnpin = { onUnpinApp(app.packageName) },
                onHide = { onHideApp(app.packageName) },
                onLock = { lockDialogApp.value = app }
            )
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
private fun DailyTasksHomeSection(
    dailyTasks: List<DailyTaskItem>,
    onDailyTaskCompleted: (Long) -> Unit,
    onDailyTaskReset: (Long) -> Unit
) {
    val activeTasks = remember(dailyTasks) {
        dailyTasks
            .filter { it.isActiveToday && it.isEnabled }
            .sortedBy { it.createdAt }
    }

    if (activeTasks.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        activeTasks.forEach { task ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .combinedClickable(
                        onClick = {
                            if (task.isCompletedToday) {
                                onDailyTaskReset(task.id)
                            } else {
                                onDailyTaskCompleted(task.id)
                            }
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MinimalCheckbox(
                    checked = task.isCompletedToday,
                    onCheckedChange = { checked ->
                        if (checked) {
                            onDailyTaskCompleted(task.id)
                        } else {
                            onDailyTaskReset(task.id)
                        }
                    }
                )
                Text(
                    text = task.title,
                    color = if (task.isCompletedToday) Color(0xFF777777) else Color.White,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PinnedAppsHomeSection(
    pinnedApps: List<AppEntry>,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPress: (AppEntry) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        pinnedApps.forEach { app ->
            Text(
                text = app.label,
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onLaunchApp(app) },
                        onLongClick = { onLongPress(app) }
                    ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DailyTaskManagerRow(
    task: DailyTaskItem,
    todayEpochDay: Long,
    formatter: DateTimeFormatter,
    onEditDailyTask: (DailyTaskItem) -> Unit,
    onDailyTaskEnabledChange: (Long, Boolean) -> Unit,
    onDailyTaskCompleted: (Long) -> Unit,
    onDailyTaskReset: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(
                onClick = {
                    if (!task.isActiveToday || !task.isEnabled) return@combinedClickable
                    if (task.isCompletedToday) {
                        onDailyTaskReset(task.id)
                    } else {
                        onDailyTaskCompleted(task.id)
                    }
                },
                onLongClick = { onEditDailyTask(task) }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MinimalCheckbox(
            checked = task.isCompletedToday,
            onCheckedChange = { checked ->
                if (!task.isActiveToday || !task.isEnabled) return@MinimalCheckbox
                if (checked) {
                    onDailyTaskCompleted(task.id)
                } else {
                    onDailyTaskReset(task.id)
                }
            },
            enabled = task.isActiveToday && task.isEnabled
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = task.title,
                color = when {
                    !task.isEnabled -> Color(0xFF555555)
                    task.isCompletedToday -> Color(0xFF777777)
                    else -> Color.White
                },
                fontSize = 16.sp
            )
            val status = dailyTaskStatusText(task, todayEpochDay, formatter)
            val pattern = task.patternLabel()
            Text(
                text = status,
                color = Color(0xFF888888),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = pattern,
                color = Color(0xFF666666),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(
            checked = task.isEnabled,
            onCheckedChange = { enabled -> onDailyTaskEnabledChange(task.id, enabled) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF444444),
                uncheckedThumbColor = Color(0xFF666666),
                uncheckedTrackColor = Color(0xFF222222)
            )
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
    dailyTasks: List<DailyTaskItem>,
    tasks: List<TaskItem>,
    heldDailyTaskIds: Set<Long>,
    onToggleTask: (Long) -> Unit,
    onAddTask: (String, Long?) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onAddDailyTask: (String, Long?, Long?, Boolean, DailyTaskRepeatMode, Int, Int) -> Unit,
    onUpdateDailyTask: (Long, String, Long?, Long?, Boolean, DailyTaskRepeatMode, Int, Int) -> Unit,
    onDeleteDailyTask: (Long) -> Unit,
    onDailyTaskEnabledChange: (Long, Boolean) -> Unit,
    onDailyTaskCompleted: (Long) -> Unit,
    onDailyTaskReset: (Long) -> Unit
) {
    val showAddDialog = remember { mutableStateOf(false) }
    val showEditDialog = remember { mutableStateOf<TaskItem?>(null) }
    val showEditDailyDialog = remember { mutableStateOf<DailyTaskItem?>(null) }

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

            val enableDailyTask = onDailyTaskEnabledChange
            val completeDailyTask = onDailyTaskCompleted
            val resetDailyTask = onDailyTaskReset
            val todayEpochDay = remember { LocalDate.now().toEpochDay() }
            val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
            val sortedDailyTasks = remember(dailyTasks, heldDailyTaskIds) {
                dailyTasks
                    .filter { task -> !task.isCompletedToday || task.id in heldDailyTaskIds }
                    .sortedWith(
                        compareByDescending<DailyTaskItem> { it.isActiveToday }
                            .thenByDescending { it.isEnabled }
                            .thenBy { it.createdAt }
                    )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (sortedDailyTasks.isNotEmpty()) {
                    items(sortedDailyTasks, key = { "daily-${it.id}" }) { task ->
                        DailyTaskManagerRow(
                            task = task,
                            todayEpochDay = todayEpochDay,
                            formatter = dateFormatter,
                            onEditDailyTask = { showEditDailyDialog.value = it },
                            onDailyTaskEnabledChange = { taskId, enabled -> enableDailyTask(taskId, enabled) },
                            onDailyTaskCompleted = { taskId -> completeDailyTask(taskId) },
                            onDailyTaskReset = { taskId -> resetDailyTask(taskId) }
                        )
                    }
                    if (tasks.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }
                items(tasks, key = { "task-${it.id}" }) { task ->
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
            onAddTask = { title, scheduledTime ->
                onAddTask(title, scheduledTime)
                showAddDialog.value = false
            },
            onAddDailyTask = { title, start, end, enabled, repeatMode, intervalDays, daysMask ->
                onAddDailyTask(title, start, end, enabled, repeatMode, intervalDays, daysMask)
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

    showEditDailyDialog.value?.let { task ->
        DailyTaskEditorDialog(
            title = "Edit Daily Task",
            initialTitle = task.title,
            initialStartEpochDay = task.startEpochDay,
            initialEndEpochDay = task.endEpochDay,
            initialEnabled = task.isEnabled,
            initialRepeatMode = task.repeatMode,
            initialIntervalDays = task.intervalDays,
            initialDaysOfWeekMask = task.daysOfWeekMask,
            onDismiss = { showEditDailyDialog.value = null },
            onConfirm = { updatedTitle, start, end, enabled, repeatMode, intervalDays, daysMask ->
                onUpdateDailyTask(task.id, updatedTitle, start, end, enabled, repeatMode, intervalDays, daysMask)
                if (!enabled && task.isCompletedToday) {
                    onDailyTaskReset(task.id)
                }
                showEditDailyDialog.value = null
            },
            onDelete = {
                onDeleteDailyTask(task.id)
                showEditDailyDialog.value = null
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

private fun dailyTaskStatusText(
    task: DailyTaskItem,
    todayEpochDay: Long,
    formatter: DateTimeFormatter
): String {
    val today = LocalDate.ofEpochDay(todayEpochDay)
    val startDate = task.startEpochDay?.let { LocalDate.ofEpochDay(it) }
    val endDate = task.endEpochDay?.let { LocalDate.ofEpochDay(it) }

    return when {
        !task.isEnabled -> "Paused"
        task.isActiveToday && task.isCompletedToday -> "Completed today"
        task.isActiveToday -> when {
            endDate != null -> "Active • ends ${endDate.format(formatter)}"
            startDate != null -> "Active • since ${startDate.format(formatter)}"
            else -> "Active every day"
        }
        startDate != null && today.isBefore(startDate) -> "Starts ${startDate.format(formatter)}"
        endDate != null && today.isAfter(endDate) -> "Ended ${endDate.format(formatter)}"
        endDate != null -> "Inactive • ended ${endDate.format(formatter)}"
        else -> "Inactive today"
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
            // Small delay to ensure focus target is attached to composition
            kotlinx.coroutines.delay(50)
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: IllegalStateException) {
                // Focus requester not ready yet, ignore
            }
        } else if (!shouldFocusSearch) {
            keyboardController?.hide()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            }
            if (filteredHiddenApps.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "Hidden Apps", color = Color.Gray, fontSize = 20.sp)
                }
                items(filteredHiddenApps) { app ->
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
                }
            }
        }
    }
        
    // Context menu for regular apps
    expandedApp.value?.let { pkgName ->
        filteredApps.find { it.packageName == pkgName }?.let { app ->
            AppContextMenu(
                app = app,
                onDismiss = { expandedApp.value = null },
                onPin = if (!app.isPinned) ({ onPinApp(app.packageName) }) else null,
                onUnpin = if (app.isPinned) ({ onUnpinApp(app.packageName) }) else null,
                onHide = { onHideApp(app.packageName) },
                onLock = { lockDialogApp.value = app }
            )
        } ?: filteredHiddenApps.find { it.packageName == pkgName }?.let { app ->
            AppContextMenu(
                app = app,
                onDismiss = { expandedApp.value = null },
                onUnhide = { onUnhideApp(app.packageName) }
            )
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
                    // Small delay to ensure focus target is attached to composition
                    kotlinx.coroutines.delay(50)
                    try {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    } catch (e: IllegalStateException) {
                        // Focus requester not ready yet, ignore
                    }
                } else {
                    keyboardController?.hide()
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
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
    showSeconds: Boolean,
    showDailyTasksOnHome: Boolean,
    bottomLeftApp: AppEntry?,
    bottomRightApp: AppEntry?,
    keyboardOnSwipe: Boolean,
    notificationInboxEnabled: Boolean,
    notificationRetentionDays: Int,
    logRetentionDays: Int,
    onOpenHomeSettings: () -> Unit,
    onOpenClockSettings: () -> Unit,
    onOpenAppDrawerSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit
) {
    val homeSummary = buildString {
        append(if (showDailyTasksOnHome) "Daily tasks visible" else "Daily tasks hidden")
        val leftLabel = bottomLeftApp?.label ?: "None"
        val rightLabel = bottomRightApp?.label ?: "None"
        append(" · bottom: $leftLabel / $rightLabel")
    }

    val clockSummary = buildString {
        val formatLabel = if (clockFormat == ClockFormat.H24) "24-hour" else "12-hour"
        append(formatLabel)
        append(if (showSeconds) " · seconds on" else " · seconds off")
    }

    val notificationSummary = if (notificationInboxEnabled) {
        val notifLabel = if (notificationRetentionDays == 1) "1 day" else "$notificationRetentionDays days"
        val logLabel = if (logRetentionDays == 1) "1 day" else "$logRetentionDays days"
        "Inbox on · clears $notifLabel · logs $logLabel"
    } else {
        "Inbox disabled"
    }

    val appDrawerSummary = if (keyboardOnSwipe) {
        "Keyboard opens with swipe"
    } else {
        "Keyboard opens manually"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        ScreenHeader(
            title = "Settings",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(32.dp))

        SettingsRow(
            title = "Home Screen",
            subtitle = homeSummary,
            onClick = onOpenHomeSettings
        )

        Spacer(modifier = Modifier.height(20.dp))

        SettingsRow(
            title = "Clock",
            subtitle = clockSummary,
            onClick = onOpenClockSettings
        )

        Spacer(modifier = Modifier.height(20.dp))

        SettingsRow(
            title = "Notification Bar",
            subtitle = notificationSummary,
            onClick = onOpenNotificationSettings
        )

        Spacer(modifier = Modifier.height(20.dp))

        SettingsRow(
            title = "App Drawer",
            subtitle = appDrawerSummary,
            onClick = onOpenAppDrawerSettings
        )

        Spacer(modifier = Modifier.height(32.dp))

        SettingsRow(
            title = "About",
            subtitle = "Developer info and feedback",
            onClick = onOpenAbout
        )
    }
}

@Composable
private fun HomeSettingsScreen(
    showDailyTasksOnHome: Boolean,
    showDailyTasksHomeSection: Boolean,
    bottomLeftApp: AppEntry?,
    bottomRightApp: AppEntry?,
    onToggleDailyTasksOnHome: (Boolean) -> Unit,
    onBottomIconClick: (BottomIconSlot) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        ScreenHeader(
            title = "Home Screen",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Daily tasks module",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Show daily tasks on home",
                    color = Color.White,
                    fontSize = 16.sp
                )
                val description = buildString {
                    append("Active daily reminders appear beneath the clock.")
                    if (showDailyTasksOnHome && !showDailyTasksHomeSection) {
                        append(" All tasks are complete right now, so the section is hidden until tomorrow or new activity.")
                    } else if (showDailyTasksOnHome) {
                        append(" Complete everything to hide it for the day.")
                    }
                }
                Text(
                    text = description,
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Switch(
                checked = showDailyTasksOnHome,
                onCheckedChange = onToggleDailyTasksOnHome
            )
        }

        if (showDailyTasksOnHome) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Daily tasks stay visible for 10 seconds after completion, then hide automatically.",
                color = Color(0xFF888888),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Bottom quick launch",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsRow(
            title = "Left icon",
            subtitle = bottomLeftApp?.label ?: "None",
            onClick = { onBottomIconClick(BottomIconSlot.LEFT) }
        )

        SettingsRow(
            title = "Right icon",
            subtitle = bottomRightApp?.label ?: "None",
            onClick = { onBottomIconClick(BottomIconSlot.RIGHT) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Tip",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Manage the tasks themselves from the Tasks page — this screen only controls visibility on home.",
            color = Color(0xFFAAAAAA),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun ClockSettingsScreen(
    clockFormat: ClockFormat,
    showSeconds: Boolean,
    onClockFormatChange: (ClockFormat) -> Unit,
    onShowSecondsToggle: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        ScreenHeader(
            title = "Clock",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Time format",
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
                    .padding(vertical = 6.dp)
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

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Seconds",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Show seconds",
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = "Add a seconds indicator to the clock on the home screen.",
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Switch(
                checked = showSeconds,
                onCheckedChange = onShowSecondsToggle
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tip",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "These preferences also update the tasks screen header so everything matches.",
            color = Color(0xFFAAAAAA),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun AppDrawerSettingsScreen(
    keyboardOnSwipe: Boolean,
    onKeyboardToggle: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        ScreenHeader(
            title = "App Drawer",
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Keyboard",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Open keyboard on swipe",
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = "Automatically show the search field and keyboard when you enter All Apps.",
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Switch(checked = keyboardOnSwipe, onCheckedChange = onKeyboardToggle)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tip",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "When disabled, you can still pull down to focus the search box manually.",
            color = Color(0xFFAAAAAA),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun LockDurationDialog(
    appName: String,
    onDismiss: () -> Unit,
    onLock: (Long) -> Unit
) {
    val predefinedDurations = listOf(
        "30 min" to 30L,
        "1 hour" to 60L,
        "2 hours" to 120L,
        "4 hours" to 240L,
        "8 hours" to 480L,
        "12 hours" to 720L,
        "1 day" to 1440L,
        "3 days" to 4320L,
        "1 week" to 10080L
    )
    
    val sliderPosition = remember { mutableFloatStateOf(0f) }
    val customHours = remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        containerColor = Color.Transparent,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A1A1A))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Lock $appName",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Text(
                    text = predefinedDurations[sliderPosition.floatValue.toInt()].first,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                Slider(
                    value = sliderPosition.floatValue,
                    onValueChange = { sliderPosition.floatValue = it },
                    valueRange = 0f..(predefinedDurations.size - 1).toFloat(),
                    steps = predefinedDurations.size - 2,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0xFF444444)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Custom duration (hours)",
                    color = Color(0xFF999999),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2A2A))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    if (customHours.value.isEmpty()) {
                        Text(
                            text = "e.g., 5",
                            color = Color(0xFF666666),
                            fontSize = 16.sp
                        )
                    }
                    BasicTextField(
                        value = customHours.value,
                        onValueChange = { 
                            if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.length <= 4)) {
                                customHours.value = it
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White,
                            fontSize = 16.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2A2A2A))
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF999999),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable {
                                val minutes = if (customHours.value.isNotEmpty()) {
                                    customHours.value.toLongOrNull()?.times(60) ?: 0L
                                } else {
                                    predefinedDurations[sliderPosition.floatValue.toInt()].second
                                }
                                if (minutes > 0) {
                                    onLock(minutes)
                                }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lock",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}
