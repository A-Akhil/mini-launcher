package com.minifocus.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minifocus.launcher.manager.AppsManager
import com.minifocus.launcher.manager.HiddenAppsManager
import com.minifocus.launcher.manager.LockManager
import com.minifocus.launcher.manager.SearchManager
import com.minifocus.launcher.manager.SettingsManager
import com.minifocus.launcher.manager.TasksManager
import com.minifocus.launcher.model.AppEntry
import com.minifocus.launcher.model.BottomIconSlot
import com.minifocus.launcher.model.ClockFormat
import com.minifocus.launcher.model.LauncherTheme
import com.minifocus.launcher.model.SearchResult
import com.minifocus.launcher.model.TaskItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val CLOCK_TICK_INTERVAL_MS = 1000L

class LauncherViewModel(
    private val appsManager: AppsManager,
    private val tasksManager: TasksManager,
    private val hiddenAppsManager: HiddenAppsManager,
    private val lockManager: LockManager,
    private val settingsManager: SettingsManager,
    private val searchManager: SearchManager
) : ViewModel() {

    private val zoneId = ZoneId.systemDefault()

    private val timeFlow: Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), zoneId))
            delay(CLOCK_TICK_INTERVAL_MS)
        }
    }

    private val searchQuery = MutableStateFlow("")
    private val isSearchVisible = MutableStateFlow(false)
    private val snackbarMessage = MutableStateFlow<String?>(null)
    private val isSettingsVisible = MutableStateFlow(false)

    private val searchResults: Flow<List<SearchResult>> = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            flowOf(emptyList())
        } else {
            flow {
                emit(searchManager.search(query))
            }
        }
    }

    private val dataSnapshot = combine(
        appsManager.observePinnedApps(),
        appsManager.observeAllApps(),
        appsManager.observeHiddenApps(),
        tasksManager.observeTasks()
    ) { pinned, allApps, hiddenApps, tasks ->
        val now = System.currentTimeMillis()
        val gracePeriod = 3000L

        val activeTasks = tasks.filter { task ->
            !task.isCompleted ||
                (task.completedAt != null && (now - task.completedAt) < gracePeriod)
        }.sortedWith(
            compareBy<TaskItem> { it.scheduledFor ?: Long.MAX_VALUE }
                .thenBy { it.createdAt }
        )

        val historyTasks = tasks.filter { task ->
            task.isCompleted &&
                task.completedAt != null &&
                (now - task.completedAt) >= gracePeriod
        }

        DataSnapshot(pinned, allApps, hiddenApps, activeTasks, historyTasks)
    }

    private val preferencesSnapshot = settingsManager.observeTheme()
        .combine(settingsManager.observeClockFormat()) { theme, clockFormat ->
            PreferencesSnapshot(
                theme = theme,
                clockFormat = clockFormat,
                bottomLeftPackage = null,
                bottomRightPackage = null,
                keyboardSearchOnSwipe = false,
                showSeconds = false,
                notificationRetentionDays = 0,
                logRetentionDays = 0
            )
        }
        .combine(settingsManager.observeBottomIcon(BottomIconSlot.LEFT)) { snapshot, bottomLeft ->
            snapshot.copy(bottomLeftPackage = bottomLeft)
        }
        .combine(settingsManager.observeBottomIcon(BottomIconSlot.RIGHT)) { snapshot, bottomRight ->
            snapshot.copy(bottomRightPackage = bottomRight)
        }
        .combine(settingsManager.observeKeyboardSearchOnSwipe()) { snapshot, keyboardSearch ->
            snapshot.copy(keyboardSearchOnSwipe = keyboardSearch)
        }
        .combine(settingsManager.observeShowSeconds()) { snapshot, showSeconds ->
            snapshot.copy(showSeconds = showSeconds)
        }
        .combine(settingsManager.observeNotificationRetentionDays()) { snapshot, retention ->
            snapshot.copy(notificationRetentionDays = retention)
        }
        .combine(settingsManager.observeLogRetentionDays()) { snapshot, retention ->
            snapshot.copy(logRetentionDays = retention)
        }

    private val overlaySnapshot = timeFlow
        .combine(searchQuery) { time, query ->
            OverlaySnapshot(
                time = time,
                query = query,
                results = emptyList(),
                searchVisible = false,
                settingsVisible = false,
                message = null
            )
        }
        .combine(searchResults) { snapshot, results ->
            snapshot.copy(results = results)
        }
        .combine(isSearchVisible) { snapshot, searchVisible ->
            snapshot.copy(searchVisible = searchVisible)
        }
        .combine(isSettingsVisible) { snapshot, settingsVisible ->
            snapshot.copy(settingsVisible = settingsVisible)
        }
        .combine(snackbarMessage) { snapshot, message ->
            snapshot.copy(message = message)
        }

    private val isHistoryVisible = MutableStateFlow(false)
    private val isNotificationInboxVisible = MutableStateFlow(false)
    private val isNotificationFilterVisible = MutableStateFlow(false)

    val uiState = dataSnapshot
        .combine(preferencesSnapshot) { data, prefs ->
            val bottomLeft = resolveBottomIcon(BottomIconSlot.LEFT, prefs.bottomLeftPackage, data)
            val bottomRight = resolveBottomIcon(BottomIconSlot.RIGHT, prefs.bottomRightPackage, data)
            LauncherUiState(
                time = LocalDateTime.now(),
                clockFormat = prefs.clockFormat,
                pinnedApps = data.pinned,
                allApps = data.all,
                hiddenApps = data.hidden,
                tasks = data.tasks,
                historyTasks = data.historyTasks,
                theme = prefs.theme,
                bottomLeft = bottomLeft,
                bottomRight = bottomRight,
                searchQuery = "",
                searchResults = emptyList(),
                isSearchVisible = false,
                isSettingsVisible = false,
                isHistoryVisible = false,
                isNotificationInboxVisible = false,
                isNotificationFilterVisible = false,
                isKeyboardSearchOnSwipe = prefs.keyboardSearchOnSwipe,
                showSeconds = prefs.showSeconds,
                notificationRetentionDays = prefs.notificationRetentionDays,
                logRetentionDays = prefs.logRetentionDays,
                message = null
            )
        }
        .combine(overlaySnapshot) { state, overlay ->
            state.copy(
                time = overlay.time,
                searchQuery = overlay.query,
                searchResults = overlay.results,
                isSearchVisible = overlay.searchVisible,
                isSettingsVisible = overlay.settingsVisible,
                message = overlay.message
            )
        }
        .combine(isHistoryVisible) { state, historyVisible ->
            state.copy(isHistoryVisible = historyVisible)
        }
        .combine(isNotificationInboxVisible) { state, inboxVisible ->
            state.copy(isNotificationInboxVisible = inboxVisible)
        }
        .combine(isNotificationFilterVisible) { state, filterVisible ->
            state.copy(isNotificationFilterVisible = filterVisible)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LauncherUiState()
        )

    fun addTask(title: String, scheduledFor: Long? = null) {
        viewModelScope.launch {
            val added = tasksManager.addTask(title, scheduledFor)
            if (added) {
                snackbarMessage.update { "Task added" }
            }
        }
    }

    fun toggleTask(taskId: Long) {
        viewModelScope.launch {
            tasksManager.toggleTask(taskId)
            delay(3100L)
            tasksManager.observeTasks().first()
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch { tasksManager.delete(taskId) }
    }

    fun pinApp(packageName: String) {
        viewModelScope.launch { appsManager.pinApp(packageName) }
    }

    fun unpinApp(packageName: String) {
        viewModelScope.launch { appsManager.unpinApp(packageName) }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch { hiddenAppsManager.hideApp(packageName) }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch { hiddenAppsManager.unhideApp(packageName) }
    }

    fun lockApp(packageName: String, durationMinutes: Long) {
        viewModelScope.launch {
            val until = System.currentTimeMillis() + durationMinutes * 60_000
            lockManager.lockApp(packageName, until)
            snackbarMessage.update { "Locked until ${formatTime(until)}" }
        }
    }

    fun unlockApp(packageName: String) {
        viewModelScope.launch {
            lockManager.unlockApp(packageName)
            snackbarMessage.update { "Lock cleared" }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSearchVisibility(visible: Boolean) {
        isSearchVisible.value = visible
        if (visible) {
            isSettingsVisible.value = false
        }
        if (!visible) {
            searchQuery.value = ""
        }
    }

    fun setSettingsVisibility(visible: Boolean) {
        isSettingsVisible.value = visible
        if (visible) {
            isSearchVisible.value = false
            isHistoryVisible.value = false
            isNotificationInboxVisible.value = false
            isNotificationFilterVisible.value = false
        }
    }

    fun setHistoryVisibility(visible: Boolean) {
        isHistoryVisible.value = visible
        if (visible) {
            isSearchVisible.value = false
            isSettingsVisible.value = false
            isNotificationInboxVisible.value = false
            isNotificationFilterVisible.value = false
        }
    }

    fun setNotificationInboxVisibility(visible: Boolean) {
        isNotificationInboxVisible.value = visible
        if (visible) {
            isSearchVisible.value = false
            isSettingsVisible.value = false
            isHistoryVisible.value = false
            isNotificationFilterVisible.value = false
        }
    }

    fun setNotificationFilterVisibility(visible: Boolean) {
        isNotificationFilterVisible.value = visible
        if (visible) {
            isSearchVisible.value = false
            isSettingsVisible.value = false
            isHistoryVisible.value = false
            isNotificationInboxVisible.value = false
        }
    }

    fun consumeMessage() {
        snackbarMessage.update { null }
    }

    fun setBottomIcon(slot: BottomIconSlot, packageName: String) {
        viewModelScope.launch { settingsManager.setBottomIcon(slot, packageName) }
    }

    fun setClockFormat(format: ClockFormat) {
        viewModelScope.launch { settingsManager.setClockFormat(format) }
    }

    fun setTheme(theme: LauncherTheme) {
        viewModelScope.launch { settingsManager.setTheme(theme) }
    }

    fun setKeyboardSearchOnSwipe(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setKeyboardSearchOnSwipe(enabled) }
    }

    fun setShowSeconds(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setShowSeconds(enabled) }
    }

    fun setNotificationRetentionDays(days: Int) {
        viewModelScope.launch { settingsManager.setNotificationRetentionDays(days) }
    }

    fun setLogRetentionDays(days: Int) {
        viewModelScope.launch { settingsManager.setLogRetentionDays(days) }
    }

    suspend fun canLaunch(packageName: String): Boolean = withContext(Dispatchers.IO) {
        !lockManager.isLocked(packageName)
    }

    private fun resolveBottomIcon(slot: BottomIconSlot, packageName: String?, data: DataSnapshot): AppEntry? {
        val candidates = data.pinned + data.all + data.hidden
        val targetPackage = packageName ?: defaultPackageForSlot(slot, candidates)
        if (targetPackage == null) return null
        return candidates.firstOrNull { it.packageName == targetPackage }
    }

    private fun defaultPackageForSlot(slot: BottomIconSlot, apps: List<AppEntry>): String? {
        val packagePreferences = when (slot) {
            BottomIconSlot.LEFT -> listOf(
                "com.google.android.dialer",
                "com.android.dialer",
                "com.samsung.android.dialer",
                "com.oneplus.dialer",
                "com.miui.dialer"
            )
            BottomIconSlot.RIGHT -> listOf(
                "com.google.android.GoogleCamera",
                "com.android.camera",
                "com.sec.android.app.camera",
                "com.oneplus.camera",
                "com.oppo.camera"
            )
        }
        val availablePackages = apps.map { it.packageName }.toSet()
        return packagePreferences.firstOrNull { it in availablePackages }
    }

    private fun formatTime(timestamp: Long): String {
        val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId)
        return time.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}

private data class DataSnapshot(
    val pinned: List<AppEntry>,
    val all: List<AppEntry>,
    val hidden: List<AppEntry>,
    val tasks: List<TaskItem>,
    val historyTasks: List<TaskItem>
)

private data class PreferencesSnapshot(
    val theme: LauncherTheme,
    val clockFormat: ClockFormat,
    val bottomLeftPackage: String?,
    val bottomRightPackage: String?,
    val keyboardSearchOnSwipe: Boolean,
    val showSeconds: Boolean,
    val notificationRetentionDays: Int,
    val logRetentionDays: Int
)

private data class OverlaySnapshot(
    val time: LocalDateTime,
    val query: String,
    val results: List<SearchResult>,
    val searchVisible: Boolean,
    val settingsVisible: Boolean,
    val message: String?
)

data class LauncherUiState(
    val time: LocalDateTime = LocalDateTime.now(),
    val clockFormat: ClockFormat = ClockFormat.H24,
    val pinnedApps: List<AppEntry> = emptyList(),
    val allApps: List<AppEntry> = emptyList(),
    val hiddenApps: List<AppEntry> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val historyTasks: List<TaskItem> = emptyList(),
    val theme: LauncherTheme = LauncherTheme.AMOLED,
    val bottomLeft: AppEntry? = null,
    val bottomRight: AppEntry? = null,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearchVisible: Boolean = false,
    val isKeyboardSearchOnSwipe: Boolean = false,
    val isSettingsVisible: Boolean = false,
    val isHistoryVisible: Boolean = false,
    val isNotificationInboxVisible: Boolean = false,
    val isNotificationFilterVisible: Boolean = false,
    val showSeconds: Boolean = false,
    val notificationRetentionDays: Int = 2,
    val logRetentionDays: Int = 30,
    val message: String? = null
) {
    val timeFormatted: String
        get() = DateTimeFormatter.ofPattern(
            when (clockFormat) {
                ClockFormat.H24 -> if (showSeconds) "HH:mm:ss" else "HH:mm"
                ClockFormat.H12 -> if (showSeconds) "hh:mm:ss" else "hh:mm"
            }
        ).format(time)

    val dateFormatted: String
        get() = DateTimeFormatter.ofPattern("EEEE, d MMMM").format(time)
}
