package com.minifocus.launcher.manager

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.minifocus.launcher.model.BottomIconSlot
import com.minifocus.launcher.model.ClockFormat
import com.minifocus.launcher.model.LauncherTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsManager(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val clockFormat = stringPreferencesKey("clock_format")
        val fontChoice = stringPreferencesKey("font_choice")
        val bottomLeft = stringPreferencesKey("bottom_left_package")
        val bottomRight = stringPreferencesKey("bottom_right_package")
        val keyboardSearchOnSwipe = intPreferencesKey("keyboard_search_on_swipe")
        val showSeconds = intPreferencesKey("show_seconds")
        val notificationRetentionDays = intPreferencesKey("notification_retention_days")
        val logRetentionDays = intPreferencesKey("log_retention_days")
        val lastLogRotationAt = longPreferencesKey("last_log_rotation_at")
    }

    fun observeTheme(): Flow<LauncherTheme> = dataStore.data.map { prefs ->
        prefs[Keys.theme]?.let { runCatching { LauncherTheme.valueOf(it) }.getOrNull() } ?: LauncherTheme.AMOLED
    }

    fun observeClockFormat(): Flow<ClockFormat> = dataStore.data.map { prefs ->
        prefs[Keys.clockFormat]?.let { runCatching { ClockFormat.valueOf(it) }.getOrNull() } ?: ClockFormat.H24
    }

    fun observeBottomIcon(slot: BottomIconSlot): Flow<String?> = dataStore.data.map { prefs ->
        when (slot) {
            BottomIconSlot.LEFT -> prefs[Keys.bottomLeft]
            BottomIconSlot.RIGHT -> prefs[Keys.bottomRight]
        }
    }

    fun observeKeyboardSearchOnSwipe(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.keyboardSearchOnSwipe]?.let { it == 1 } ?: false
    }

    fun observeShowSeconds(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.showSeconds]?.let { it == 1 } ?: false
    }

    fun observeNotificationRetentionDays(): Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.notificationRetentionDays] ?: DEFAULT_NOTIFICATION_RETENTION_DAYS
    }

    fun observeLogRetentionDays(): Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.logRetentionDays] ?: DEFAULT_LOG_RETENTION_DAYS
    }

    fun observeLastLogRotation(): Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.lastLogRotationAt] ?: 0L
    }

    suspend fun setTheme(theme: LauncherTheme) {
        dataStore.edit { prefs -> prefs[Keys.theme] = theme.name }
    }

    suspend fun setClockFormat(format: ClockFormat) {
        dataStore.edit { prefs -> prefs[Keys.clockFormat] = format.name }
    }

    suspend fun setBottomIcon(slot: BottomIconSlot, packageName: String) {
        dataStore.edit { prefs ->
            when (slot) {
                BottomIconSlot.LEFT -> prefs[Keys.bottomLeft] = packageName
                BottomIconSlot.RIGHT -> prefs[Keys.bottomRight] = packageName
            }
        }
    }

    suspend fun setKeyboardSearchOnSwipe(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.keyboardSearchOnSwipe] = if (enabled) 1 else 0
        }
    }

    suspend fun setShowSeconds(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.showSeconds] = if (enabled) 1 else 0
        }
    }

    suspend fun setNotificationRetentionDays(days: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.notificationRetentionDays] = days
        }
    }

    suspend fun setLogRetentionDays(days: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.logRetentionDays] = days
        }
    }

    suspend fun setLastLogRotation(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.lastLogRotationAt] = timestamp
        }
    }

    companion object {
        private const val DEFAULT_NOTIFICATION_RETENTION_DAYS = 2
        private const val DEFAULT_LOG_RETENTION_DAYS = 30
    }
}
