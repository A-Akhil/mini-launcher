package com.minifocus.launcher.manager

import android.content.Context
import com.minifocus.launcher.LauncherApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * HARDLOCK BRANCH ONLY
 * 
 * Monitors lock/hidden state changes and automatically backs up to external storage
 * so restrictions survive clear data.
 */
class HardlockAutoBackup(
    private val context: Context,
    private val scope: CoroutineScope
) {
    
    fun start() {
        val app = context.applicationContext as? LauncherApplication ?: return
        val container = app.container
        
        // Monitor locks and hidden apps, trigger backup on any change
        scope.launch {
            combine(
                container.lockManager.observeLocks(),
                container.hiddenAppsManager.observeHiddenApps(),
                container.settingsManager.observeNotificationInboxEnabled(),
                container.settingsManager.observeSmartSuggestionsEnabled(),
                container.settingsManager.observeShowDailyTasksOnHome()
            ) { locks, hiddenApps, inboxEnabled, smartEnabled, dailyTasksEnabled ->
                
                val lockedApps = locks.map { it.packageName to it.lockedUntil }
                val hiddenPackages = hiddenApps.map { it.packageName }
                
                container.hardlockBackupManager.backupState(
                    lockedApps = lockedApps,
                    hiddenApps = hiddenPackages,
                    notificationInboxEnabled = inboxEnabled,
                    smartSuggestionsEnabled = smartEnabled,
                    showDailyTasksOnHome = dailyTasksEnabled
                )
            }.collect { /* backup triggered */ }
        }
    }
}
