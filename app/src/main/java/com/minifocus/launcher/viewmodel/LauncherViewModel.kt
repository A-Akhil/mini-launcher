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
        val gracePeroid = 3000L // 3 seconds to allow undo
        
        // Active tasks: not completed OR completed less than 3 seconds ago
        val activeTasks = tasks.filter { task ->
            !task.isCompleted || 
            (task.completedAt != null && (now - task.completedAt) < gracePeroid)
        }.sortedWith(compareBy<TaskItem> { 
            // Sort by scheduled time (nearest first), nulls last
            it.scheduledFor ?: Long.MAX_VALUE
        }.thenBy { 
            // Then by creation time
            it.createdAt 
        })
        
        // History: completed and past grace period
        val historyTasks = tasks.filter { task ->
            task.isCompleted && 
            task.completedAt != null && 
            (now - task.completedAt) >= gracePeroid
        }
        
        DataSnapshot(pinned, allApps, hiddenApps, activeTasks, historyTasks)
    }

    private val preferencesSnapshot = combine(
        settingsManager.observeTheme(),
        settingsManager.observeClockFormat(),
        settingsManager.observeBottomIcon(BottomIconSlot.LEFT),
        settingsManager.observeBottomIcon(BottomIconSlot.RIGHT),
        settingsManager.observeKeyboardSearchOnSwipe(),
        settingsManager.observeShowSeconds()
    ) { flows: Array<Any?> ->
        PreferencesSnapshot(
            theme = flows[0] as LauncherTheme,
            clockFormat = flows[1] as ClockFormat,
            bottomLeftPackage = flows[2] as String?,
            bottomRightPackage = flows[3] as String?,
            keyboardSearchOnSwipe = flows[4] as Boolean,
            showSeconds = flows[5] as Boolean
        )
    }

    private val overlaySnapshot = combine(
        timeFlow,
        searchQuery,
        searchResults,
        isSearchVisible,
        isSettingsVisible,
        snackbarMessage
    ) { flows: Array<Any?> ->
        OverlaySnapshot(
            time = flows[0] as LocalDateTime,
            query = flows[1] as String,
            results = flows[2] as List<SearchResult>,
            searchVisible = flows[3] as Boolean,
            settingsVisible = flows[4] as Boolean,
            message = flows[5] as String?
        )
    }

    private val isHistoryVisible = MutableStateFlow(false)

    val uiState = combine(
        dataSnapshot,
        preferencesSnapshot,
        overlaySnapshot,
        isHistoryVisible
    ) { data, prefs, overlay, historyVisible ->
        val bottomLeft = resolveBottomIcon(BottomIconSlot.LEFT, prefs.bottomLeftPackage, data)
        val bottomRight = resolveBottomIcon(BottomIconSlot.RIGHT, prefs.bottomRightPackage, data)
        LauncherUiState(
            time = overlay.time,
            clockFormat = prefs.clockFormat,
            pinnedApps = data.pinned,
            allApps = data.all,
            hiddenApps = data.hidden,
            tasks = data.tasks,
            historyTasks = data.historyTasks,
            theme = prefs.theme,
            bottomLeft = bottomLeft,
            bottomRight = bottomRight,
            searchQuery = overlay.query,
            searchResults = overlay.results,
            isSearchVisible = overlay.searchVisible,
            isSettingsVisible = overlay.settingsVisible,
            isHistoryVisible = historyVisible,
            isKeyboardSearchOnSwipe = prefs.keyboardSearchOnSwipe,
            showSeconds = prefs.showSeconds,
            message = overlay.message
        )
    }.stateIn(
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
            
            // Schedule a delayed refresh to move completed tasks to history after grace period
            delay(3100L) // Slightly longer than grace period
            // Trigger re-evaluation by updating a dummy flow
            tasksManager.observeTasks().first() // Force re-collect
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
        }
    }

    fun setHistoryVisibility(visible: Boolean) {
        isHistoryVisible.value = visible
        if (visible) {
            isSearchVisible.value = false
            isSettingsVisible.value = false
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
    val showSeconds: Boolean
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
    val showSeconds: Boolean = false,
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
