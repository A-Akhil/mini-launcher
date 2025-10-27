package com.minifocus.launcher.permissions

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Placeholder listener so the launcher can request notification access upfront.
 * Future work will persist notifications into an inbox per TODO item #5.
 */
class NotificationInboxListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // No-op for now; inbox functionality will be implemented separately.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op until notification inbox is implemented.
    }
}
