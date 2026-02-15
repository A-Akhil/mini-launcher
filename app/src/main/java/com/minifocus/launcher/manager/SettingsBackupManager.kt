package com.minifocus.launcher.manager

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class SettingsBackupManager(private val context: Context) {

    private val backupDir: File
        get() {
            // Use public Documents directory so backup survives uninstalls/clear data
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val dir = File(documentsDir, ".minilauncher")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    private val backupFile: File
        get() = File(backupDir, "settings_backup.json")

    suspend fun backupSettings(
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
    ) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            
            // All app settings
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
            
            backupFile.writeText(json.toString(2))
            Log.d("SettingsBackup", "Backup successful to ${backupFile.absolutePath}")
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

    suspend fun restoreSettings(): BackupData? = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) return@withContext null
            
            val json = JSONObject(backupFile.readText())
            Log.d("SettingsBackup", "Restoring from backup")
            
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
