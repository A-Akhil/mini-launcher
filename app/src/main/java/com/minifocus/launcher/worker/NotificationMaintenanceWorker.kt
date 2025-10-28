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
