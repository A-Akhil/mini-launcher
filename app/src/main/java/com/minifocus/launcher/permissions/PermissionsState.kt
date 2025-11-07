package com.minifocus.launcher.permissions

/**
 * Represents the launcher's critical permission state. All flags must be true
 * before the main launcher UI can be shown.
 */
data class PermissionsState(
    val notificationsGranted: Boolean,
    val notificationListenerGranted: Boolean,
    val deviceAdminGranted: Boolean,
    val exactAlarmsGranted: Boolean,
    val usageStatsGranted: Boolean,
    val overlayGranted: Boolean
) {
    val requiredGranted: Boolean
        get() = notificationsGranted

    val allGranted: Boolean
        get() = notificationsGranted &&
            notificationListenerGranted &&
            deviceAdminGranted &&
            exactAlarmsGranted &&
            usageStatsGranted &&
            overlayGranted
}
