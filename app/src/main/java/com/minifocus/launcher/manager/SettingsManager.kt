package com.minifocus.launcher.manager

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
}
