package com.minifocus.launcher.permissions

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.minifocus.launcher.LauncherApplication
import com.minifocus.launcher.manager.NotificationInboxManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationInboxListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val inboxManager: NotificationInboxManager?
        get() = (application as? LauncherApplication)?.container?.notificationInboxManager

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == packageName) {
            if (sbn.id == SUMMARY_NOTIFICATION_ID) {
                // Ignore our own summary notification to avoid feedback loops.
                return
            }
            return
        }
        val manager = inboxManager ?: return
        serviceScope.launch {
            val intercepted = manager.addNotification(sbn)
            Log.d(TAG, "posted pkg=${sbn.packageName} intercepted=$intercepted")
            if (intercepted) {
                val keep = shouldKeepOriginal(sbn)
                Log.d(TAG, "decision pkg=${sbn.packageName} keepOriginal=$keep")
                if (!keep) {
                    cancelNotification(sbn.key)
                    Log.d(TAG, "cancelled pkg=${sbn.packageName} key=${sbn.key}")
                }
                manager.trimExpired()
            } else {
                val shouldArchive = manager.shouldIntercept(sbn.packageName)
                val keep = shouldKeepOriginal(sbn)
                val isGroupSummary = sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
                Log.d(
                    TAG,
                    "no-intercept pkg=${sbn.packageName} archive=$shouldArchive keepOriginal=$keep groupSummary=$isGroupSummary"
                )
                if (shouldArchive && !keep && isGroupSummary) {
                    cancelNotification(sbn.key)
                    Log.d(TAG, "cancelled summary pkg=${sbn.packageName} key=${sbn.key}")
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No additional work required; removal is handled via retention.
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val SUMMARY_NOTIFICATION_ID = 0x4E5446 // "NTF" hex
        private const val TAG = "InboxListener"
    }

    private fun shouldKeepOriginal(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == "android") {
            return true
        }
        return try {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
        } catch (_: Exception) {
            false
        }
    }
}
