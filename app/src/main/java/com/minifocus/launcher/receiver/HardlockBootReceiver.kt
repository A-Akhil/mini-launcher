package com.minifocus.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.minifocus.launcher.LauncherApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * HARDLOCK BRANCH ONLY
 * 
 * Receives BOOT_COMPLETED broadcast and restores hardlock state from
 * Documents/.minilauncher/ backup so restrictions survive device reboots.
 */
class HardlockBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        Log.i(TAG, "Boot completed, checking for hardlock backup...")

        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope.launch {
            try {
                val app = context.applicationContext as? LauncherApplication ?: return@launch
                val backupManager = app.container.hardlockBackupManager
                val lockManager = app.container.lockManager
                val hiddenManager = app.container.hiddenAppsManager
                
                val state = backupManager.restoreState() ?: return@launch
                
                // Restore locked apps
                state.lockedApps.forEach { lockedApp ->
                    if (lockedApp.lockedUntil > System.currentTimeMillis()) {
                        lockManager.lockApp(lockedApp.packageName, lockedApp.lockedUntil)
                        Log.i(TAG, "Restored lock for ${lockedApp.packageName} until ${lockedApp.lockedUntil}")
                    }
                }
                
                // Restore hidden apps
                state.hiddenApps.forEach { packageName ->
                    hiddenManager.hideApp(packageName)
                    Log.i(TAG, "Restored hidden status for $packageName")
                }
                
                Log.i(TAG, "Hardlock state restored successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore hardlock state on boot", e)
            }
        }
    }

    companion object {
        private const val TAG = "HardlockBootReceiver"
    }
}
