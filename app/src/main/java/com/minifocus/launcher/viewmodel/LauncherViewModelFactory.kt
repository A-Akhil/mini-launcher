package com.minifocus.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.minifocus.launcher.manager.AppsManager
import com.minifocus.launcher.manager.DailyTasksManager
import com.minifocus.launcher.manager.HiddenAppsManager
import com.minifocus.launcher.manager.LockManager
import com.minifocus.launcher.manager.SearchManager
import com.minifocus.launcher.manager.SettingsBackupManager
import com.minifocus.launcher.manager.SettingsManager
import com.minifocus.launcher.manager.TasksManager
import com.minifocus.launcher.manager.AppUsageStatsManager

class LauncherViewModelFactory(
    private val appsManager: AppsManager,
    private val tasksManager: TasksManager,
    private val dailyTasksManager: DailyTasksManager,
    private val hiddenAppsManager: HiddenAppsManager,
    private val lockManager: LockManager,
    private val settingsManager: SettingsManager,
    private val settingsBackupManager: SettingsBackupManager,
    private val searchManager: SearchManager,
    private val appUsageStatsManager: AppUsageStatsManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LauncherViewModel(
                appsManager = appsManager,
                tasksManager = tasksManager,
                dailyTasksManager = dailyTasksManager,
                hiddenAppsManager = hiddenAppsManager,
                lockManager = lockManager,
                settingsManager = settingsManager,
                settingsBackupManager = settingsBackupManager,
                searchManager = searchManager,
                appUsageStatsManager = appUsageStatsManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}
