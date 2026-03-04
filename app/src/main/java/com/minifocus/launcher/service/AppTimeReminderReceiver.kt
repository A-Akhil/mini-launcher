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

package com.minifocus.launcher.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.minifocus.launcher.MainActivity

class AppTimeReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL) ?: return
        val actionName = intent.getStringExtra(EXTRA_EXPIRY_ACTION) ?: "NOTIFICATION"

        when (actionName) {
            "NOTIFICATION" -> showReminderNotification(context, packageName, appLabel)
            "PROMPT" -> navigateToLauncherWithExpiredPrompt(context, packageName, appLabel)
            "RETURN_HOME" -> returnToLauncher(context)
        }
    }

    private fun showReminderNotification(context: Context, packageName: String, appLabel: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Time Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders when your intended app usage time expires"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launcherIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launcherIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time is up")
            .setContentText("Your intended time for $appLabel has expired")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(packageName.hashCode(), notification)
    }

    /**
     * Navigates the user back to the launcher and delivers an intent that
     * triggers the time-expired prompt dialog (with cooldown timer).
     * No notification is shown for this action type.
     */
    private fun navigateToLauncherWithExpiredPrompt(
        context: Context,
        packageName: String,
        appLabel: String
    ) {
        val intent = Intent(context, TimeExpiredOverlayActivity::class.java).apply {
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_APP_LABEL, appLabel)
            putExtra(TimeExpiredOverlayActivity.EXTRA_IS_TIME_EXPIRED, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Silently brings the user back to the launcher home screen.
     * No notification, no dialog -- just closes the current app.
     */
    private fun returnToLauncher(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    companion object {
        private const val CHANNEL_ID = "app_time_reminder"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_EXPIRY_ACTION = "extra_expiry_action"
        const val ACTION_TIME_EXPIRED = "com.minifocus.launcher.action.TIME_EXPIRED"

        /**
         * Tracks which package currently has an active alarm so that
         * MainActivity.onResume() can cancel it when the user returns
         * to the launcher before the timer expires. Shared between
         * LauncherApp (pre-launch confirm) and TimeExpiredOverlayActivity
         * (extend time confirm).
         */
        @Volatile
        var activeReminderPackage: String? = null

        fun schedule(
            context: Context,
            packageName: String,
            appLabel: String,
            durationMinutes: Int,
            expiryAction: String
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AppTimeReminderReceiver::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_APP_LABEL, appLabel)
                putExtra(EXTRA_EXPIRY_ACTION, expiryAction)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                packageName.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAt = System.currentTimeMillis() + durationMinutes * 60_000L

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
                    )
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
                )
            }
        }

        fun cancel(context: Context, packageName: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AppTimeReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                packageName.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
