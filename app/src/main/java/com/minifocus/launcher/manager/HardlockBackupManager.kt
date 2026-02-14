package com.minifocus.launcher.manager

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

/**
 * HARDLOCK BRANCH ONLY
 * 
 * Backs up app locks, hidden apps, and critical settings to Documents/.minilauncher/
 * so that clearing app data cannot wipe the user's restrictions.
 * 
 * WARNING: This is part of the hardlock variant and should never ship to Play Store.
 */
class HardlockBackupManager(private val context: Context) {

    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val backupDir: File
        get() {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val dir = File(documentsDir, ".minilauncher")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    private val backupFile: File
        get() = File(backupDir, "hardlock_state.json")

    @Serializable
    data class HardlockState(
        val lockedApps: List<LockedApp> = emptyList(),
        val hiddenApps: List<String> = emptyList(),
        val notificationInboxEnabled: Boolean = false,
        val smartSuggestionsEnabled: Boolean = true,
        val showDailyTasksOnHome: Boolean = true,
        val backupTimestamp: Long = System.currentTimeMillis()
    )

    @Serializable
    data class LockedApp(
        val packageName: String,
        val lockedUntil: Long
    )

    suspend fun backupState(
        lockedApps: List<Pair<String, Long>>,
        hiddenApps: List<String>,
        notificationInboxEnabled: Boolean,
        smartSuggestionsEnabled: Boolean,
        showDailyTasksOnHome: Boolean
    ) = withContext(Dispatchers.IO) {
        try {
            val state = HardlockState(
                lockedApps = lockedApps.map { LockedApp(it.first, it.second) },
                hiddenApps = hiddenApps,
                notificationInboxEnabled = notificationInboxEnabled,
                smartSuggestionsEnabled = smartSuggestionsEnabled,
                showDailyTasksOnHome = showDailyTasksOnHome,
                backupTimestamp = System.currentTimeMillis()
            )
            
            val jsonString = json.encodeToString(state)
            backupFile.writeText(jsonString)
            
            Log.i(TAG, "Backed up hardlock state: ${lockedApps.size} locks, ${hiddenApps.size} hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup hardlock state", e)
        }
    }

    suspend fun restoreState(): HardlockState? = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) {
                Log.w(TAG, "No backup file found at ${backupFile.absolutePath}")
                return@withContext null
            }
            
            val jsonString = backupFile.readText()
            val state = json.decodeFromString<HardlockState>(jsonString)
            
            // Filter out expired locks
            val now = System.currentTimeMillis()
            val validLocks = state.lockedApps.filter { it.lockedUntil > now }
            
            Log.i(TAG, "Restored hardlock state: ${validLocks.size} active locks, ${state.hiddenApps.size} hidden")
            state.copy(lockedApps = validLocks)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore hardlock state", e)
            null
        }
    }

    suspend fun hasBackup(): Boolean = withContext(Dispatchers.IO) {
        backupFile.exists()
    }

    companion object {
        private const val TAG = "HardlockBackup"
    }
}
