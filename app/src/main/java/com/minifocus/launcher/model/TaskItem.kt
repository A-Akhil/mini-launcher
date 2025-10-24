package com.minifocus.launcher.model

data class TaskItem(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
    val createdAt: Long
)
