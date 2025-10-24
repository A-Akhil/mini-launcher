package com.minifocus.launcher.model

data class AppEntry(
    val packageName: String,
    val label: String,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val lockedUntil: Long? = null
) {
    val isLocked: Boolean
        get() = lockedUntil?.let { it > System.currentTimeMillis() } == true
}
