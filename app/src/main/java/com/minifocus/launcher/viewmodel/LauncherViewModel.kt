package com.minifocus.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minifocus.launcher.manager.AppsManager
import com.minifocus.launcher.manager.DailyTasksManager
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
import com.minifocus.launcher.model.DailyTaskItem
import com.minifocus.launcher.data.entity.toItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val CLOCK_TICK_INTERVAL_MS = 1000L
private const val DAILY_TASK_COMPLETION_HOLD_MS = 10_000L
private const val TASK_HISTORY_GRACE_MS = 10_000L

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModel(
    private val appsManager: AppsManager,
    private val tasksManager: TasksManager,
    private val dailyTasksManager: DailyTasksManager,
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

    private val dailyTaskHoldExpiries = MutableStateFlow<Map<Long, Long>>(emptyMap())
    private val dailyTaskHoldJobs = mutableMapOf<Long, Job>()

    private val tickingTasksFlow: Flow<List<TaskItem>> = combine(
        tasksManager.observeTasks(),
        timeFlow
    ) { tasks, _ -> tasks }

    private val todayEpochDayFlow: Flow<Long> = timeFlow
        .map { it.toLocalDate().toEpochDay() }
        .distinctUntilChanged()

    private val dailyTasksFlow = dailyTasksManager.observeDailyTasks()

    private val dailyTaskItemsFlow: Flow<List<DailyTaskItem>> = combine(dailyTasksFlow, todayEpochDayFlow) { entities, epochDay ->
        entities.map { it.toItem(epochDay) }
    }

    private val searchQuery = MutableStateFlow("")
    private val isSearchVisible = MutableStateFlow(false)
    private val snackbarMessage = MutableStateFlow<String?>(null)
    private val isSettingsVisible = MutableStateFlow(false)
    private val homeResetTick = MutableStateFlow(0)

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
        tickingTasksFlow
    ) { pinned, allApps, hiddenApps, tasks ->
        val now = System.currentTimeMillis()

        val activeTasks = tasks.filter { task ->
            !task.isCompleted ||
                (task.completedAt != null && (now - task.completedAt) < TASK_HISTORY_GRACE_MS)
        }.sortedWith(
            compareBy<TaskItem> { it.scheduledFor ?: Long.MAX_VALUE }
                .thenBy { it.createdAt }
        )

        val historyTasks = tasks.filter { task ->
            task.isCompleted &&
                task.completedAt != null &&
                (now - task.completedAt) >= TASK_HISTORY_GRACE_MS
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
                showDailyTasksOnHome = true,
                notificationInboxEnabled = false,
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
        .combine(settingsManager.observeShowDailyTasksOnHome()) { snapshot, showDailyTasks ->
            snapshot.copy(showDailyTasksOnHome = showDailyTasks)
        }
        .combine(settingsManager.observeNotificationInboxEnabled()) { snapshot, enabled ->
            snapshot.copy(notificationInboxEnabled = enabled)
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
    private val isHomeSettingsVisible = MutableStateFlow(false)
    private val isClockSettingsVisible = MutableStateFlow(false)
    private val isAppDrawerSettingsVisible = MutableStateFlow(false)
    private val isNotificationInboxVisible = MutableStateFlow(false)
    private val isNotificationFilterVisible = MutableStateFlow(false)
    private val isNotificationSettingsVisible = MutableStateFlow(false)
    private val isAboutVisible = MutableStateFlow(false)
    private val isEmergencyUnlockVisible = MutableStateFlow(false)

    private val baseUiState = combine(
        dataSnapshot,
        preferencesSnapshot,
        dailyTaskItemsFlow,
        dailyTaskHoldExpiries
    ) { data, prefs, dailyTasks, holdMap ->
        val bottomLeft = resolveBottomIcon(BottomIconSlot.LEFT, prefs.bottomLeftPackage, data)
        val bottomRight = resolveBottomIcon(BottomIconSlot.RIGHT, prefs.bottomRightPackage, data)
        val nowMillis = System.currentTimeMillis()
        val anyDailyActive = dailyTasks.any { it.isActiveToday }
        val anyDailyIncomplete = dailyTasks.any { it.isActiveToday && !it.isCompletedToday }
        val activeHoldIds = holdMap.filterValues { it > nowMillis }.keys
        val holdActive = activeHoldIds.isNotEmpty()
        val showHomeDailySection = prefs.showDailyTasksOnHome && anyDailyActive && (anyDailyIncomplete || holdActive)
        LauncherUiState(
            time = LocalDateTime.now(),
            clockFormat = prefs.clockFormat,
            pinnedApps = data.pinned,
            allApps = data.all,
            hiddenApps = data.hidden,
            tasks = data.tasks,
            dailyTasks = dailyTasks,
            historyTasks = data.historyTasks,
            theme = prefs.theme,
            bottomLeft = bottomLeft,
            bottomRight = bottomRight,
            searchQuery = "",
            searchResults = emptyList(),
            isSearchVisible = false,
            isKeyboardSearchOnSwipe = prefs.keyboardSearchOnSwipe,
            isSettingsVisible = false,
            isHomeSettingsVisible = false,
            isClockSettingsVisible = false,
            isAppDrawerSettingsVisible = false,
            isHistoryVisible = false,
            isNotificationInboxVisible = false,
            isNotificationFilterVisible = false,
            isNotificationSettingsVisible = false,
            isAboutVisible = false,
            isEmergencyUnlockVisible = false,
            showSeconds = prefs.showSeconds,
            showDailyTasksOnHome = prefs.showDailyTasksOnHome,
            showDailyTasksHomeSection = showHomeDailySection,
            heldDailyTaskIds = activeHoldIds,
            notificationInboxEnabled = prefs.notificationInboxEnabled,
            notificationRetentionDays = prefs.notificationRetentionDays,
            logRetentionDays = prefs.logRetentionDays,
            message = null
        )
    }

    val uiState = baseUiState
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
        .combine(isHomeSettingsVisible) { state, homeSettingsVisible ->
            state.copy(isHomeSettingsVisible = homeSettingsVisible)
        }
        .combine(isClockSettingsVisible) { state, clockSettingsVisible ->
            state.copy(isClockSettingsVisible = clockSettingsVisible)
        }
        .combine(isAppDrawerSettingsVisible) { state, appDrawerVisible ->
            state.copy(isAppDrawerSettingsVisible = appDrawerVisible)
        }
        .combine(isNotificationInboxVisible) { state, inboxVisible ->
            state.copy(isNotificationInboxVisible = inboxVisible)
        }
        .combine(isNotificationFilterVisible) { state, filterVisible ->
            state.copy(isNotificationFilterVisible = filterVisible)
        }
        .combine(isNotificationSettingsVisible) { state, notifSettingsVisible ->
            state.copy(isNotificationSettingsVisible = notifSettingsVisible)
        }
        .combine(isAboutVisible) { state, aboutVisible ->
            state.copy(isAboutVisible = aboutVisible)
        }
        .combine(isEmergencyUnlockVisible) { state, emergencyUnlockVisible ->
            state.copy(isEmergencyUnlockVisible = emergencyUnlockVisible)
        }
        .combine(homeResetTick) { state, resetTick ->
            state.copy(homeResetTick = resetTick)
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

    fun addDailyTask(
        title: String,
        startEpochDay: Long?,
        endEpochDay: Long?,
        enabled: Boolean = true
    ) {
        viewModelScope.launch {
            val trimmed = title.trim()
            if (trimmed.isEmpty()) return@launch
            val (normalizedStart, normalizedEnd) = normalizeDailyWindow(startEpochDay, endEpochDay)
            val id = dailyTasksManager.addDailyTask(trimmed, normalizedStart, normalizedEnd, enabled)
            if (id > 0) {
                snackbarMessage.update { "Daily task added" }
            }
        }
    }

    fun updateDailyTask(
        taskId: Long,
        title: String,
        startEpochDay: Long?,
        endEpochDay: Long?,
        enabled: Boolean
    ) {
        viewModelScope.launch {
            val entity = dailyTasksManager.getDailyTask(taskId) ?: return@launch
            val trimmed = title.trim()
            if (trimmed.isEmpty()) return@launch
            val (normalizedStart, normalizedEnd) = normalizeDailyWindow(startEpochDay, endEpochDay)
            dailyTasksManager.updateDailyTask(
                entity.copy(
                    title = trimmed,
                    startEpochDay = normalizedStart,
                    endEpochDay = normalizedEnd,
                    isEnabled = enabled
                )
            )
            snackbarMessage.update { "Daily task updated" }
        }
    }

    fun deleteDailyTask(taskId: Long) {
        viewModelScope.launch {
            dailyTasksManager.deleteDailyTask(taskId)
            snackbarMessage.update { "Daily task removed" }
            clearDailyTaskHold(taskId)
        }
    }

    fun setDailyTaskEnabled(taskId: Long, enabled: Boolean) {
        viewModelScope.launch {
            val entity = dailyTasksManager.getDailyTask(taskId) ?: return@launch
            dailyTasksManager.updateDailyTask(entity.copy(isEnabled = enabled))
            if (!enabled) {
                clearDailyTaskHold(taskId)
            }
        }
    }

    fun markDailyTaskCompleted(taskId: Long) {
        viewModelScope.launch {
            registerDailyTaskHold(taskId)
            try {
                dailyTasksManager.markCompletedForToday(taskId)
            } catch (cancellation: CancellationException) {
                clearDailyTaskHold(taskId)
                throw cancellation
            } catch (error: Throwable) {
                clearDailyTaskHold(taskId)
                throw error
            }
        }
    }

    fun resetDailyTaskForToday(taskId: Long) {
        viewModelScope.launch {
            dailyTasksManager.resetCompletion(taskId)
            clearDailyTaskHold(taskId)
        }
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
            isHomeSettingsVisible.value = false
            isClockSettingsVisible.value = false
            isAppDrawerSettingsVisible.value = false
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
            isHomeSettingsVisible.value = false
            isClockSettingsVisible.value = false
            isAppDrawerSettingsVisible.value = false
            isNotificationInboxVisible.value = false
            isNotificationFilterVisible.value = false
        }
    }

    fun setHomeSettingsVisibility(visible: Boolean) {
        isHomeSettingsVisible.value = visible
        if (visible) {
            isSearchVisible.value = false
            isSettingsVisible.value = false
            isHistoryVisible.value = false
            isClockSettingsVisible.value = false
            isAppDrawerSettingsVisible.value = false
            isNotificationInboxVisible.value = false
            isNotificationFilterVisible.value = false
        }
    }

    fun setClockSettingsVisibility(visible: Boolean) {
        isClockSettingsVisible.value = visible
        if (visible) {
            isSearchVisible.value = false
            isSettingsVisible.value = false
            isHomeSettingsVisible.value = false
            isHistoryVisible.value = false
            isAppDrawerSettingsVisible.value = false
            isNotificationInboxVisible.value = false
            isNotificationFilterVisible.value = false
        }
    }

    fun setAppDrawerSettingsVisibility(visible: Boolean) {
        isAppDrawerSettingsVisible.value = visible
        if (visible) {
            isSearchVisible.value = false
            isSettingsVisible.value = false
            isHomeSettingsVisible.value = false
            isClockSettingsVisible.value = false
            isHistoryVisible.value = false
            isNotificationInboxVisible.value = false
            isNotificationFilterVisible.value = false
        }
    }

    fun setNotificationInboxVisibility(visible: Boolean) {
        isNotificationInboxVisible.value = visible
        if (visible) {
            isSearchVisible.value = false
            isSettingsVisible.value = false
            isHomeSettingsVisible.value = false
            isClockSettingsVisible.value = false
            isAppDrawerSettingsVisible.value = false
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
            isNotificationSettingsVisible.value = false
            isHomeSettingsVisible.value = false
            isClockSettingsVisible.value = false
            isAppDrawerSettingsVisible.value = false
        }
    }

    fun setNotificationSettingsVisibility(visible: Boolean) {
        isNotificationSettingsVisible.value = visible
        if (visible) {
            isSearchVisible.value = false
            isSettingsVisible.value = false
            isHistoryVisible.value = false
            isNotificationInboxVisible.value = false
            isNotificationFilterVisible.value = false
            isAboutVisible.value = false
            isHomeSettingsVisible.value = false
            isClockSettingsVisible.value = false
            isAppDrawerSettingsVisible.value = false
        }
    }

    fun setAboutVisibility(visible: Boolean) {
        isAboutVisible.value = visible
        if (visible) {
            isSearchVisible.value = false
            isSettingsVisible.value = false
            isHistoryVisible.value = false
            isNotificationInboxVisible.value = false
            isNotificationFilterVisible.value = false
            isNotificationSettingsVisible.value = false
            isEmergencyUnlockVisible.value = false
            isHomeSettingsVisible.value = false
            isClockSettingsVisible.value = false
            isAppDrawerSettingsVisible.value = false
        }
    }

    fun setEmergencyUnlockVisibility(visible: Boolean) {
        isEmergencyUnlockVisible.value = visible
        if (visible) {
            isSearchVisible.value = false
            isSettingsVisible.value = false
            isHistoryVisible.value = false
            isNotificationInboxVisible.value = false
            isNotificationFilterVisible.value = false
            isNotificationSettingsVisible.value = false
            isAboutVisible.value = false
            isHomeSettingsVisible.value = false
            isClockSettingsVisible.value = false
            isAppDrawerSettingsVisible.value = false
        }
    }

    fun resetToHome() {
        isSettingsVisible.value = false
        isHomeSettingsVisible.value = false
    isClockSettingsVisible.value = false
        isAppDrawerSettingsVisible.value = false
        isHistoryVisible.value = false
        isNotificationInboxVisible.value = false
        isNotificationFilterVisible.value = false
        isNotificationSettingsVisible.value = false
        isAboutVisible.value = false
        isEmergencyUnlockVisible.value = false
        isSearchVisible.value = false
        searchQuery.value = ""
        homeResetTick.update { it + 1 }
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

    fun setShowDailyTasksOnHome(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setShowDailyTasksOnHome(enabled) }
    }

    fun setNotificationInboxEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setNotificationInboxEnabled(enabled) }
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

    private fun normalizeDailyWindow(startEpochDay: Long?, endEpochDay: Long?): Pair<Long?, Long?> {
        if (startEpochDay == null || endEpochDay == null) {
            return startEpochDay to endEpochDay
        }
        return if (startEpochDay <= endEpochDay) {
            startEpochDay to endEpochDay
        } else {
            endEpochDay to startEpochDay
        }
    }

    private fun registerDailyTaskHold(taskId: Long) {
        val expiry = System.currentTimeMillis() + DAILY_TASK_COMPLETION_HOLD_MS
        dailyTaskHoldJobs.remove(taskId)?.cancel()
        dailyTaskHoldExpiries.update { current -> current + (taskId to expiry) }
        dailyTaskHoldJobs[taskId] = viewModelScope.launch {
            delay(DAILY_TASK_COMPLETION_HOLD_MS)
            dailyTaskHoldExpiries.update { current ->
                val currentExpiry = current[taskId]
                if (currentExpiry == null || currentExpiry > System.currentTimeMillis()) {
                    current
                } else {
                    current - taskId
                }
            }
            dailyTaskHoldJobs.remove(taskId)
        }
    }

    private fun clearDailyTaskHold(taskId: Long) {
        dailyTaskHoldJobs.remove(taskId)?.cancel()
        dailyTaskHoldExpiries.update { current -> current - taskId }
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
    val showDailyTasksOnHome: Boolean,
    val notificationInboxEnabled: Boolean,
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
    val dailyTasks: List<DailyTaskItem> = emptyList(),
    val historyTasks: List<TaskItem> = emptyList(),
    val theme: LauncherTheme = LauncherTheme.AMOLED,
    val bottomLeft: AppEntry? = null,
    val bottomRight: AppEntry? = null,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearchVisible: Boolean = false,
    val isKeyboardSearchOnSwipe: Boolean = false,
    val isSettingsVisible: Boolean = false,
    val isHomeSettingsVisible: Boolean = false,
    val isClockSettingsVisible: Boolean = false,
    val isAppDrawerSettingsVisible: Boolean = false,
    val isHistoryVisible: Boolean = false,
    val isNotificationInboxVisible: Boolean = false,
    val isNotificationFilterVisible: Boolean = false,
    val isNotificationSettingsVisible: Boolean = false,
    val isAboutVisible: Boolean = false,
    val isEmergencyUnlockVisible: Boolean = false,
    val showSeconds: Boolean = false,
    val showDailyTasksOnHome: Boolean = true,
    val showDailyTasksHomeSection: Boolean = true,
    val heldDailyTaskIds: Set<Long> = emptySet(),
    val notificationInboxEnabled: Boolean = false,
    val notificationRetentionDays: Int = 2,
    val logRetentionDays: Int = 30,
    val message: String? = null,
    val homeResetTick: Int = 0
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
