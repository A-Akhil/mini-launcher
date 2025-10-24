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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                lockManager.clearExpiredLocks()
                delay(60_000)
            }
        }
    }

    private val dataSnapshot = combine(
        appsManager.observePinnedApps(),
        appsManager.observeAllApps(),
        appsManager.observeHiddenApps(),
        tasksManager.observeTasks()
    ) { pinned, allApps, hiddenApps, tasks ->
        DataSnapshot(pinned, allApps, hiddenApps, tasks)
    }

    private val preferencesSnapshot = combine(
        settingsManager.observeTheme(),
        settingsManager.observeClockFormat(),
        settingsManager.observeBottomIcon(BottomIconSlot.LEFT),
        settingsManager.observeBottomIcon(BottomIconSlot.RIGHT),
        settingsManager.observeKeyboardSearchOnSwipe()
    ) { theme, clockFormat, leftPackage, rightPackage, keyboardOnSwipe ->
        PreferencesSnapshot(theme, clockFormat, leftPackage, rightPackage, keyboardOnSwipe)
    }

    private val overlaySnapshot = combine(
        timeFlow,
        searchQuery,
        searchResults,
        isSearchVisible,
        snackbarMessage
    ) { time, query, results, searchVisible, message ->
        OverlaySnapshot(time, query, results, searchVisible, message)
    }

    val uiState = combine(
        dataSnapshot,
        preferencesSnapshot,
        overlaySnapshot,
        isSettingsVisible
    ) { data, prefs, overlay, settingsVisible ->
        val bottomLeft = resolveBottomIcon(BottomIconSlot.LEFT, prefs.bottomLeftPackage, data)
        val bottomRight = resolveBottomIcon(BottomIconSlot.RIGHT, prefs.bottomRightPackage, data)
        LauncherUiState(
            time = overlay.time,
            clockFormat = prefs.clockFormat,
            pinnedApps = data.pinned,
            allApps = data.all,
            hiddenApps = data.hidden,
            tasks = data.tasks,
            theme = prefs.theme,
            bottomLeft = bottomLeft,
            bottomRight = bottomRight,
            searchQuery = overlay.query,
            searchResults = overlay.results,
            isSearchVisible = overlay.searchVisible,
            isSettingsVisible = settingsVisible,
            isKeyboardSearchOnSwipe = prefs.keyboardSearchOnSwipe,
            message = overlay.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState()
    )

    fun addTask(title: String) {
        viewModelScope.launch {
            val added = tasksManager.addTask(title)
            if (added) {
                snackbarMessage.update { "Task added" }
            }
        }
    }

    fun toggleTask(taskId: Long) {
        viewModelScope.launch { tasksManager.toggleTask(taskId) }
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
    val tasks: List<TaskItem>
)

private data class PreferencesSnapshot(
    val theme: LauncherTheme,
    val clockFormat: ClockFormat,
    val bottomLeftPackage: String?,
    val bottomRightPackage: String?
    ,
    val keyboardSearchOnSwipe: Boolean
)

private data class OverlaySnapshot(
    val time: LocalDateTime,
    val query: String,
    val results: List<SearchResult>,
    val searchVisible: Boolean,
    val message: String?
)

data class LauncherUiState(
    val time: LocalDateTime = LocalDateTime.now(),
    val clockFormat: ClockFormat = ClockFormat.H24,
    val pinnedApps: List<AppEntry> = emptyList(),
    val allApps: List<AppEntry> = emptyList(),
    val hiddenApps: List<AppEntry> = emptyList(),
    val tasks: List<TaskItem> = emptyList(),
    val theme: LauncherTheme = LauncherTheme.AMOLED,
    val bottomLeft: AppEntry? = null,
    val bottomRight: AppEntry? = null,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearchVisible: Boolean = false,
    val isKeyboardSearchOnSwipe: Boolean = false,
    val isSettingsVisible: Boolean = false,
    val message: String? = null
) {
    val timeFormatted: String
        get() = DateTimeFormatter.ofPattern(
            when (clockFormat) {
                ClockFormat.H24 -> "HH:mm"
                ClockFormat.H12 -> "hh:mm"
            }
        ).format(time)

    val dateFormatted: String
        get() = DateTimeFormatter.ofPattern("EEEE, d MMMM").format(time)
}
