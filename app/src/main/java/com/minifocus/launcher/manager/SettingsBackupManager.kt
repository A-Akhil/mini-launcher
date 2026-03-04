/*
 * Minimalist Focus Launcher
 * Copyright (C) 2025 A-Akhil
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.minifocus.launcher.manager

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

class SettingsBackupManager {

    suspend fun backupToStream(
        outputStream: OutputStream,
        textSize: String,
        clockFormat: String,
        bottomLeftPackage: String?,
        bottomRightPackage: String?,
        keyboardSearchOnSwipe: Boolean,
        showSeconds: Boolean,
        showDailyTasksOnHome: Boolean,
        smartSuggestionsEnabled: Boolean,
        notificationInboxEnabled: Boolean,
        notificationRetentionDays: Int,
        logRetentionDays: Int,
        doubleTapLockScreen: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            json.put("textSize", textSize)
            json.put("clockFormat", clockFormat)
            json.put("bottomLeftPackage", bottomLeftPackage ?: "")
            json.put("bottomRightPackage", bottomRightPackage ?: "")
            json.put("keyboardSearchOnSwipe", keyboardSearchOnSwipe)
            json.put("showSeconds", showSeconds)
            json.put("showDailyTasksOnHome", showDailyTasksOnHome)
            json.put("smartSuggestionsEnabled", smartSuggestionsEnabled)
            json.put("notificationInboxEnabled", notificationInboxEnabled)
            json.put("notificationRetentionDays", notificationRetentionDays)
            json.put("logRetentionDays", logRetentionDays)
            json.put("doubleTapLockScreen", doubleTapLockScreen)
            json.put("backupTimestamp", System.currentTimeMillis())
            outputStream.writer().use { it.write(json.toString(2)) }
            Log.d("SettingsBackup", "Backup written to stream")
            true
        } catch (e: Exception) {
            Log.e("SettingsBackup", "Backup failed", e)
            false
        }
    }

    data class BackupData(
        val textSize: String?,
        val clockFormat: String?,
        val bottomLeftPackage: String?,
        val bottomRightPackage: String?,
        val keyboardSearchOnSwipe: Boolean?,
        val showSeconds: Boolean?,
        val showDailyTasksOnHome: Boolean?,
        val smartSuggestionsEnabled: Boolean?,
        val notificationInboxEnabled: Boolean?,
        val notificationRetentionDays: Int?,
        val logRetentionDays: Int?,
        val doubleTapLockScreen: Boolean?
    )

    suspend fun restoreFromStream(inputStream: InputStream): BackupData? = withContext(Dispatchers.IO) {
        try {
            val text = inputStream.reader().use { it.readText() }
            val json = JSONObject(text)
            Log.d("SettingsBackup", "Restoring from stream")
            BackupData(
                textSize = if (json.has("textSize")) json.getString("textSize") else null,
                clockFormat = if (json.has("clockFormat")) json.getString("clockFormat") else null,
                bottomLeftPackage = if (json.has("bottomLeftPackage")) {
                    val pkg = json.getString("bottomLeftPackage")
                    if (pkg.isEmpty()) null else pkg
                } else null,
                bottomRightPackage = if (json.has("bottomRightPackage")) {
                    val pkg = json.getString("bottomRightPackage")
                    if (pkg.isEmpty()) null else pkg
                } else null,
                keyboardSearchOnSwipe = if (json.has("keyboardSearchOnSwipe")) json.getBoolean("keyboardSearchOnSwipe") else null,
                showSeconds = if (json.has("showSeconds")) json.getBoolean("showSeconds") else null,
                showDailyTasksOnHome = if (json.has("showDailyTasksOnHome")) json.getBoolean("showDailyTasksOnHome") else null,
                smartSuggestionsEnabled = if (json.has("smartSuggestionsEnabled")) json.getBoolean("smartSuggestionsEnabled") else null,
                notificationInboxEnabled = if (json.has("notificationInboxEnabled")) json.getBoolean("notificationInboxEnabled") else null,
                notificationRetentionDays = if (json.has("notificationRetentionDays")) json.getInt("notificationRetentionDays") else null,
                logRetentionDays = if (json.has("logRetentionDays")) json.getInt("logRetentionDays") else null,
                doubleTapLockScreen = if (json.has("doubleTapLockScreen")) json.getBoolean("doubleTapLockScreen") else null
            )
        } catch (e: Exception) {
            Log.e("SettingsBackup", "Restore failed", e)
            null
        }
    }
}

