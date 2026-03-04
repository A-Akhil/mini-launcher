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
        val logEntries: List<NotificationItem> = emptyList(),
        val unreadCount: Int = 0,
        val notificationRetentionDays: Int = DEFAULT_NOTIFICATION_RETENTION,
        val logRetentionDays: Int = DEFAULT_LOG_RETENTION,
        val lastDeleted: NotificationItem? = null
    )

    private val lastDeleted = MutableStateFlow<NotificationItem?>(null)
    private val logEntries = MutableStateFlow<List<NotificationItem>>(emptyList())
    private val notificationsFlow = inboxManager.observeNotifications()

    private val baseStateFlow = combine(
        notificationsFlow,
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

    val uiState = combine(baseStateFlow, logEntries) { state, logs ->
        state.copy(logEntries = logs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = NotificationInboxUiState()
    )

    init {
        refreshLogEntries()
        viewModelScope.launch {
            notificationsFlow.collect {
                refreshLogEntries()
            }
        }
        viewModelScope.launch {
            settingsManager.observeLogRetentionDays().collect {
                refreshLogEntries()
            }
        }
    }

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

    fun refreshLogEntries() {
        viewModelScope.launch {
            val logs = inboxManager.loadLogHistory(LOG_HISTORY_LIMIT)
            logEntries.value = logs
        }
    }

    companion object {
        private const val DEFAULT_NOTIFICATION_RETENTION = 2
        private const val DEFAULT_LOG_RETENTION = 30
        private const val LOG_HISTORY_LIMIT = 5000
    }
}
