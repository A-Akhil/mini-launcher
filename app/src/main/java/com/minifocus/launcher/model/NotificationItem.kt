package com.minifocus.launcher.model

data class NotificationItem(
    val id: Long,
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long,
    val isRead: Boolean,
    val expiresAt: Long?
)
