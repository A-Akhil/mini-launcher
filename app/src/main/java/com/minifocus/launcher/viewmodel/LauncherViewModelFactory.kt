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
import com.minifocus.launcher.manager.AppTimeReminderManager

class LauncherViewModelFactory(
    private val appsManager: AppsManager,
    private val tasksManager: TasksManager,
    private val dailyTasksManager: DailyTasksManager,
    private val hiddenAppsManager: HiddenAppsManager,
    private val lockManager: LockManager,
    private val settingsManager: SettingsManager,
    private val settingsBackupManager: SettingsBackupManager,
    private val searchManager: SearchManager,
    private val appUsageStatsManager: AppUsageStatsManager,
    private val appTimeReminderManager: AppTimeReminderManager
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
                appUsageStatsManager = appUsageStatsManager,
                appTimeReminderManager = appTimeReminderManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}
