package com.minifocus.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minifocus.launcher.manager.NotificationInboxManager
import com.minifocus.launcher.manager.SettingsManager
import com.minifocus.launcher.model.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationInboxViewModel(
    private val inboxManager: NotificationInboxManager,
    private val settingsManager: SettingsManager
) : ViewModel() {

    data class NotificationInboxUiState(
        val notifications: List<NotificationItem> = emptyList(),
        val unreadCount: Int = 0,
        val notificationRetentionDays: Int = DEFAULT_NOTIFICATION_RETENTION,
        val logRetentionDays: Int = DEFAULT_LOG_RETENTION,
        val lastDeleted: NotificationItem? = null
    )

    private val lastDeleted = MutableStateFlow<NotificationItem?>(null)

    private val stateFlow = combine(
        inboxManager.observeNotifications(),
        inboxManager.observeUnreadCount(),
        settingsManager.observeNotificationRetentionDays(),
        settingsManager.observeLogRetentionDays(),
        lastDeleted
    ) { notifications, unread, notificationRetention, logRetention, deleted ->
        NotificationInboxUiState(
            notifications = notifications,
            unreadCount = unread,
            notificationRetentionDays = notificationRetention,
            logRetentionDays = logRetention,
            lastDeleted = deleted
        )
    }

    val uiState = stateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = NotificationInboxUiState()
    )

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            inboxManager.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            inboxManager.markAllAsRead()
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            val removed = inboxManager.deleteNotification(id)
            lastDeleted.value = removed
        }
    }

    fun undoDelete() {
        val deleted = lastDeleted.value ?: return
        viewModelScope.launch {
            try {
                inboxManager.restoreNotification(deleted)
            } finally {
                lastDeleted.value = null
            }
        }
    }

    fun dismissUndo() {
        lastDeleted.update { null }
    }

    fun setNotificationRetentionDays(days: Int) {
        viewModelScope.launch {
            settingsManager.setNotificationRetentionDays(days)
        }
    }

    fun setLogRetentionDays(days: Int) {
        viewModelScope.launch {
            settingsManager.setLogRetentionDays(days)
        }
    }

    companion object {
        private const val DEFAULT_NOTIFICATION_RETENTION = 2
        private const val DEFAULT_LOG_RETENTION = 30
    }
}
