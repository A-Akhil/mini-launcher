package com.minifocus.launcher.model

data class AppTimeSession(
    val packageName: String,
    val appLabel: String,
    val startedAt: Long,
    val durationMinutes: Int,
    val expiryAction: ExpiryAction
) {
    val expiresAt: Long
        get() = startedAt + durationMinutes * 60_000L

    val isExpired: Boolean
        get() = System.currentTimeMillis() >= expiresAt

    val remainingMillis: Long
        get() = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
}
