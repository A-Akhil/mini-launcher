package com.minifocus.launcher.manager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.minifocus.launcher.MainActivity
import com.minifocus.launcher.R
import com.minifocus.launcher.data.dao.NotificationDao
import com.minifocus.launcher.data.dao.NotificationFilterDao
import com.minifocus.launcher.data.entity.NotificationEntity
import com.minifocus.launcher.data.entity.NotificationFilterEntity
import com.minifocus.launcher.model.NotificationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.telecom.TelecomManager
import java.util.concurrent.TimeUnit

class NotificationInboxManager(
    private val context: Context,
    private val notificationDao: NotificationDao,
    private val notificationFilterDao: NotificationFilterDao,
    private val logger: InboxLogger
) {
    private val retentionDays = MutableStateFlow(DEFAULT_NOTIFICATION_RETENTION_DAYS)
    private val packageManager = context.packageManager
    private val essentialSystemPackages: Set<String> by lazy { discoverEssentialPackages() }

    fun observeNotifications(): Flow<List<NotificationItem>> =
        notificationDao.observeAll().map { entities ->
            entities
                .filterNot { entity -> isSystemPackage(entity.packageName) }
                .map { entity -> entity.toItem() }
        }

    fun observeUnreadCount(): Flow<Int> = notificationDao.observeUnreadCount()

    fun observeTotalCount(): Flow<Int> = notificationDao.observeTotalCount()

    fun observeFilters(): Flow<List<NotificationFilterEntity>> =
        notificationFilterDao.observeAll().map { entities ->
            entities.filterNot { entity -> isSystemPackage(entity.packageName) }
        }

    suspend fun addNotification(sbn: StatusBarNotification): Boolean {
        logger.log(
            event = "posted",
            message = "Notification received",
            metadata = mapOf(
                "package" to sbn.packageName,
                "id" to sbn.id.toString(),
                "key" to sbn.key,
                "flags" to sbn.notification.flags.toString()
            )
        )
        debug("posted pkg=${sbn.packageName} id=${sbn.id} key=${sbn.key} flags=${sbn.notification.flags}")
        if (sbn.packageName == context.packageName) {
            if (sbn.id == SUMMARY_NOTIFICATION_ID) {
                logger.log(
                    event = "summary_skip",
                    message = "Skipped launcher summary notification",
                    metadata = emptyMap()
                )
            }
            return false
        }

        if (isSystemPackage(sbn.packageName)) {
            logger.log(
                event = "system_skip",
                message = "Skipped system package",
                metadata = mapOf("package" to sbn.packageName)
            )
            debug("system skip pkg=${sbn.packageName}")
            notificationDao.deleteByPackage(sbn.packageName)
            notificationFilterDao.deleteByPackage(sbn.packageName)
            return false
        }

        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            val extras = sbn.notification.extras
            val summaryText = extras?.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
            val textLines = extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            val hasAggregateContent = !summaryText.isNullOrBlank() || (textLines != null && textLines.size > 1)

            if (hasAggregateContent) {
                logger.log(
                    event = "group_summary_skip",
                    message = "Skipped aggregate group summary",
                    metadata = mapOf(
                        "package" to sbn.packageName,
                        "summary" to (summaryText ?: ""),
                        "lines" to (textLines?.size?.toString() ?: "0")
                    )
                )
                debug(
                    "group summary skip pkg=${sbn.packageName} summary=${summaryText ?: ""} lines=${textLines?.size ?: 0}"
                )
                return false
            }
            logger.log(
                event = "group_summary_allow",
                message = "Allowing group summary",
                metadata = mapOf(
                    "package" to sbn.packageName,
                    "summary" to (summaryText ?: ""),
                    "lines" to (textLines?.size?.toString() ?: "0")
                )
            )
            debug(
                "group summary allowed pkg=${sbn.packageName} summary=${summaryText ?: ""} lines=${textLines?.size ?: 0}"
            )
        }

        if (!shouldIntercept(sbn.packageName)) {
            logger.log(
                event = "filter_skip",
                message = "Notification filter disabled",
                metadata = mapOf("package" to sbn.packageName)
            )
            debug("filter skip pkg=${sbn.packageName}")
            return false
        }

        val appName = resolveAppName(sbn.packageName)
        ensureFilterExists(sbn.packageName, appName)

        val timestamp = sbn.postTime
        val title = sbn.notification.extras?.let { extras ->
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        }
        val contentText = sbn.notification.extras?.let { extras ->
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        }
        debug(
            "content pkg=${sbn.packageName} title=${title ?: ""} text=${contentText ?: ""} summary=${sbn.notification.extras?.getCharSequence(Notification.EXTRA_SUMMARY_TEXT) ?: ""}"
        )

        val existing = notificationDao.findByKey(sbn.key)
        val expiresAt = timestamp + TimeUnit.DAYS.toMillis(retentionDays.value.toLong())

        val entity = NotificationEntity(
            id = existing?.id ?: 0,
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            title = title,
            text = contentText,
            timestamp = timestamp,
            isRead = existing?.isRead ?: false,
            smallIcon = null,
            expiresAt = expiresAt
        )

    notificationDao.upsert(entity)
        logger.log(
            event = "intercept",
            message = "Stored notification",
            metadata = mapOf(
                "package" to sbn.packageName,
                "title" to (title ?: ""),
                "timestamp" to timestamp.toString()
            )
        )
        debug("stored pkg=${sbn.packageName} id=${entity.id} key=${entity.key}")
        refreshSummaryNotification()
        return true
    }



    suspend fun markAsRead(id: Long) {
        notificationDao.markAsRead(id)
        logger.log("mark_read", "Marked notification as read", mapOf("id" to id.toString()))
        refreshSummaryNotification()
    }

    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
        logger.log("mark_all_read", "Marked all notifications as read")
        refreshSummaryNotification()
    }

    suspend fun deleteNotification(id: Long): NotificationItem? {
        val entity = notificationDao.findById(id) ?: return null
        notificationDao.delete(id)
        logger.log(
            event = "delete",
            message = "Deleted notification",
            metadata = mapOf("id" to id.toString())
        )
        refreshSummaryNotification()
        return entity.toItem()
    }

    suspend fun clearAll() {
        notificationDao.deleteAll()
        logger.log("clear_all", "Cleared all notifications")
        refreshSummaryNotification()
    }

    suspend fun restoreNotification(item: NotificationItem) {
        val entity = item.toEntity()
        notificationDao.upsert(entity)
        logger.log(
            event = "restore",
            message = "Restored notification",
            metadata = mapOf("id" to item.id.toString())
        )
        refreshSummaryNotification()
    }

    suspend fun getNotification(id: Long): NotificationItem? =
        notificationDao.findById(id)?.toItem()

    suspend fun shouldIntercept(packageName: String): Boolean {
        if (isSystemPackage(packageName)) {
            return false
        }
        val filter = notificationFilterDao.getFilter(packageName)
        return filter?.isEnabled ?: true
    }

    suspend fun setFilterEnabled(packageName: String, enabled: Boolean) {
        if (isSystemPackage(packageName)) {
            notificationFilterDao.deleteByPackage(packageName)
            return
        }
        ensureFilterExists(packageName, resolveAppName(packageName))
        notificationFilterDao.setEnabled(packageName, enabled)
        logger.log(
            event = "filter_update",
            message = "Filter toggled",
            metadata = mapOf("package" to packageName, "enabled" to enabled.toString())
        )
    }

    suspend fun ensureFilterExists(packageName: String, appName: String) {
        if (isSystemPackage(packageName)) {
            notificationFilterDao.deleteByPackage(packageName)
            return
        }
        val existing = notificationFilterDao.getFilter(packageName)
        if (existing == null) {
            notificationFilterDao.upsert(
                NotificationFilterEntity(
                    packageName = packageName,
                    appName = appName,
                    isEnabled = true
                )
            )
        }
    }

    suspend fun updateRetention(retentionInDays: Int) {
        val sanitized = retentionInDays.coerceAtLeast(1)
        retentionDays.value = sanitized
        logger.log(
            event = "retention_update",
            message = "Updated retention window",
            metadata = mapOf("days" to sanitized.toString())
        )
        withContext(Dispatchers.IO) {
            val expiryOffset = TimeUnit.DAYS.toMillis(sanitized.toLong())
            notificationDao.getAll().forEach { entity ->
                notificationDao.updateExpiry(entity.id, entity.timestamp + expiryOffset)
            }
        }
        refreshSummaryNotification()
    }

    suspend fun trimExpired() {
        withContext(Dispatchers.IO) {
            val threshold = System.currentTimeMillis()
            notificationDao.deleteExpired(threshold)
            logger.log(
                event = "trim",
                message = "Trimmed expired notifications",
                metadata = mapOf("threshold" to threshold.toString())
            )
        }
        refreshSummaryNotification()
    }

    fun updateLogRetention(days: Int) {
        logger.updateRetention(days)
        logger.log(
            event = "log_retention_update",
            message = "Updated log retention window",
            metadata = mapOf("days" to days.toString())
        )
    }

    suspend fun refreshSummaryNotification() {
        withContext(Dispatchers.IO) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val total = notificationDao.countAll()
            if (total <= 0) {
                notificationManager.cancel(SUMMARY_NOTIFICATION_ID)
                return@withContext
            }

            val unread = notificationDao.countUnread()

            ensureSummaryChannel(notificationManager)

            val pendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(
                    Intent(context, MainActivity::class.java).apply {
                        action = MainActivity.ACTION_OPEN_INBOX
                    }
                )
                .getPendingIntent(
                    0,
                    PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentFlags()
                )

            val summaryTitle = context.resources.getQuantityString(
                R.plurals.notification_summary_title,
                total,
                total
            )

            val summaryText = if (unread > 0) {
                context.resources.getQuantityString(
                    R.plurals.notification_summary_subtitle_unread,
                    unread,
                    unread
                )
            } else {
                context.getString(R.string.notification_summary_subtitle)
            }

            val summary = NotificationCompat.Builder(context, SUMMARY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_summary)
                .setContentTitle(summaryTitle)
                .setContentText(summaryText)
                .setNumber(total)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .build()

            notificationManager.notify(SUMMARY_NOTIFICATION_ID, summary)
        }
    }

    private fun NotificationEntity.toItem(): NotificationItem = NotificationItem(
        id = id,
        key = key,
        packageName = packageName,
        appName = appName,
        title = title,
        text = text,
        timestamp = timestamp,
        isRead = isRead,
        expiresAt = expiresAt
    )

    private fun NotificationItem.toEntity(): NotificationEntity = NotificationEntity(
        id = id,
        key = key,
        packageName = packageName,
        appName = appName,
        title = title,
        text = text,
        timestamp = timestamp,
        isRead = isRead,
        smallIcon = null,
        expiresAt = expiresAt
    )

    private fun resolveAppName(packageName: String): String =
        try {
            val pm = context.packageManager
            val label = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0))
            label?.toString() ?: packageName
        } catch (_: Exception) {
            packageName
        }

    private fun ensureSummaryChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val existing = notificationManager.getNotificationChannel(SUMMARY_CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                SUMMARY_CHANNEL_ID,
                context.getString(R.string.notification_inbox_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_inbox_channel_description)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun pendingIntentFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    companion object {
        private const val LOG_TAG = "NotificationInbox"
        private const val DEFAULT_NOTIFICATION_RETENTION_DAYS = 2
        private const val SUMMARY_NOTIFICATION_ID = 0x4E_54_46
        private const val SUMMARY_CHANNEL_ID = "notification_inbox_summary"
    }

    private fun debug(message: String) {
        Log.d(LOG_TAG, message)
    }

    private fun isSystemPackage(packageName: String): Boolean {
        if (packageName in essentialSystemPackages) {
            return true
        }
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            isSystem || isUpdatedSystem
        } catch (_: Exception) {
            false
        }
    }

    private fun discoverEssentialPackages(): Set<String> {
        val pm = packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            Intent(Settings.ACTION_SETTINGS)
        )

        val resolvedPackages = buildSet {
            intents.forEach { intent ->
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo
                    ?.packageName
                    ?.let { add(it) }
            }

            val telecom = context.getSystemService(TelecomManager::class.java)
            telecom?.defaultDialerPackage?.let { add(it) }
        }

        return resolvedPackages
    }
}
