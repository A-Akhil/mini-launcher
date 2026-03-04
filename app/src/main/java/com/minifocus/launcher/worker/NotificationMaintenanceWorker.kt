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

package com.minifocus.launcher.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.minifocus.launcher.LauncherApplication
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class NotificationMaintenanceWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? LauncherApplication ?: return Result.failure()
        return try {
            val container = app.container
            val notificationManager = container.notificationInboxManager
            val settingsManager = container.settingsManager
            val notificationRetention = settingsManager.observeNotificationRetentionDays().first()
            val logRetention = settingsManager.observeLogRetentionDays().first()
            notificationManager.updateRetention(notificationRetention)
            notificationManager.updateLogRetention(logRetention)
            notificationManager.trimExpired()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "notification-maintenance"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationMaintenanceWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
