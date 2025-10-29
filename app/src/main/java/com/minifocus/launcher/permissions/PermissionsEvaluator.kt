package com.minifocus.launcher.permissions

import android.Manifest
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object PermissionsEvaluator {

    fun currentState(context: Context): PermissionsState {
        return PermissionsState(
            notificationsGranted = isPostNotificationsGranted(context),
            notificationListenerGranted = isNotificationListenerGranted(context),
            deviceAdminGranted = isDeviceAdminActive(context),
            exactAlarmsGranted = canScheduleExactAlarms(context),
            usageStatsGranted = hasUsageStatsPermission(context),
            overlayGranted = canDrawOverlays(context)
        )
    }

    private fun isPostNotificationsGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isNotificationListenerGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    private fun isDeviceAdminActive(context: Context): Boolean {
        val component = ComponentName(context, LauncherDeviceAdminReceiver::class.java)
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        return dpm?.isAdminActive(component) == true
    }

    private fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            true
        } else {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager?.canScheduleExactAlarms() == true
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
}
