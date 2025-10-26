package com.minifocus.launcher.manager

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.minifocus.launcher.worker.TaskNotificationWorker
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {

    fun scheduleNotification(taskId: Long, taskTitle: String, scheduledTime: Long) {
        val currentTime = System.currentTimeMillis()
        val delay = scheduledTime - currentTime

        if (delay <= 0) {
            // Task is in the past, don't schedule
            return
        }

        val data = Data.Builder()
            .putLong(TaskNotificationWorker.KEY_TASK_ID, taskId)
            .putString(TaskNotificationWorker.KEY_TASK_TITLE, taskTitle)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TaskNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("task_notification_$taskId")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun cancelNotification(taskId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag("task_notification_$taskId")
    }
}
