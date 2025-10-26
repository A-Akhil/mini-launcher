package com.minifocus.launcher.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.minifocus.launcher.MainActivity
import com.minifocus.launcher.R

class TaskNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: return Result.failure()
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        
        if (taskId == -1L) return Result.failure()

        showNotification(taskTitle)
        
        return Result.success()
    }

    private fun showNotification(taskTitle: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (required for Android 8.0+)
        val channelId = "task_reminders"
        val channel = NotificationChannel(
            channelId,
            "Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for scheduled tasks"
        }
        notificationManager.createNotificationChannel(channel)

        // Intent to open the launcher when notification is tapped
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Task Reminder")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(taskTitle.hashCode(), notification)
    }

    companion object {
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_TASK_ID = "task_id"
    }
}
