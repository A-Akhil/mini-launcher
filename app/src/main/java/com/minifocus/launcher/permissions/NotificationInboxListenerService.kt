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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        if (!manager.isEnabled()) {
            return
        }
        serviceScope.launch {
            val intercepted = manager.addNotification(sbn)
            val isSilent = !hasAlertingBehavior(sbn.notification)
            val isClearable = sbn.isClearable
            val flags = sbn.notification.flags
            Log.d(
                TAG,
                "posted pkg=${sbn.packageName} intercepted=$intercepted silent=$isSilent clearable=$isClearable flags=$flags"
            )
            if (intercepted) {
                val keep = shouldKeepOriginal(sbn, isClearable, flags)
                Log.d(
                    TAG,
                    "decision pkg=${sbn.packageName} keepOriginal=$keep silent=$isSilent clearable=$isClearable flags=$flags"
                )
                if (!keep) {
                    cancelAndVerifyRemoval(sbn, isSilent, isClearable)
                }
                manager.trimExpired()
            } else {
                val shouldArchive = manager.shouldIntercept(sbn.packageName)
                val keep = shouldKeepOriginal(sbn, isClearable, flags)
                val isGroupSummary = sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
                Log.d(
                    TAG,
                    "no-intercept pkg=${sbn.packageName} archive=$shouldArchive keepOriginal=$keep groupSummary=$isGroupSummary silent=$isSilent"
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
        private const val CANCEL_RETRY_DELAY_MS = 100L
    }

    private fun hasAlertingBehavior(notification: Notification): Boolean {
        return notification.sound != null ||
            notification.defaults and Notification.DEFAULT_SOUND != 0 ||
            (notification.vibrate != null && notification.vibrate.isNotEmpty()) ||
            notification.defaults and Notification.DEFAULT_VIBRATE != 0
    }

    private fun shouldKeepOriginal(
        sbn: StatusBarNotification,
        isClearable: Boolean,
        flags: Int
    ): Boolean {
        if (!isClearable) {
            // System marks these as non-clearable (ex: Brave private tabs reminder), so
            // NotificationListenerService cannot dismiss them. Keep the original so the
            // banner remains until the source app clears it. TODO: explore a user-facing
            // override to explicitly cancel stubborn silent notifications.
            Log.d(
                TAG,
                "keeping pkg=${sbn.packageName} reason=non_clearable flags=$flags"
            )
            return true
        }
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

    private suspend fun cancelAndVerifyRemoval(
        sbn: StatusBarNotification,
        isSilent: Boolean,
        isClearable: Boolean
    ) {
        val key = sbn.key
        val packageName = sbn.packageName
        issueCancel(key, packageName, "initial", isSilent, isClearable)
        delay(CANCEL_RETRY_DELAY_MS)
        if (!isNotificationActive(key)) {
            Log.d(TAG, "cancelled pkg=$packageName key=$key silent=$isSilent clearable=$isClearable")
            return
        }
        Log.w(
            TAG,
            "retry cancel pkg=$packageName key=$key silent=$isSilent clearable=$isClearable"
        )
        issueBatchCancel(key, packageName, isSilent, isClearable)
        delay(CANCEL_RETRY_DELAY_MS)
        if (isNotificationActive(key)) {
            Log.w(
                TAG,
                "notification still active pkg=$packageName key=$key silent=$isSilent clearable=$isClearable"
            )
        } else {
            Log.d(
                TAG,
                "cancelled on retry pkg=$packageName key=$key silent=$isSilent clearable=$isClearable"
            )
        }
    }

    private suspend fun issueCancel(
        key: String,
        packageName: String,
        label: String,
        isSilent: Boolean,
        isClearable: Boolean
    ) {
        withContext(Dispatchers.Main) {
            try {
                cancelNotification(key)
            } catch (t: Throwable) {
                Log.e(
                    TAG,
                    "cancelNotification failed ($label) pkg=$packageName key=$key silent=$isSilent clearable=$isClearable",
                    t
                )
            }
        }
    }

    private suspend fun issueBatchCancel(
        key: String,
        packageName: String,
        isSilent: Boolean,
        isClearable: Boolean
    ) {
        withContext(Dispatchers.Main) {
            try {
                cancelNotifications(arrayOf(key))
            } catch (t: Throwable) {
                Log.e(
                    TAG,
                    "cancelNotifications failed pkg=$packageName key=$key silent=$isSilent clearable=$isClearable",
                    t
                )
            }
        }
    }

    private fun isNotificationActive(key: String): Boolean {
        return try {
            activeNotifications.any { it.key == key }
        } catch (t: Throwable) {
            Log.w(TAG, "activeNotifications unavailable for key=$key", t)
            false
        }
    }
}
