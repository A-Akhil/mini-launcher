package com.minifocus.launcher.model

data class TaskItem(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val completedAt: Long? = null,
    val scheduledFor: Long? = null,
    val notificationId: Int? = null
)
