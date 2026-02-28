package com.minifocus.launcher.service

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.minifocus.launcher.LauncherApplication
import com.minifocus.launcher.model.AppEntry
import com.minifocus.launcher.model.ExpiryAction
import com.minifocus.launcher.ui.TimeIntentionDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Transparent overlay activity that displays either the time-expired prompt
 * dialog (with cooldown) or a normal time-intention dialog on top of
 * whatever app the user is currently using.
 *
 * Launched by:
 * - AppTimeReminderReceiver when the PROMPT expiry action fires (expired mode).
 * - LockScreenAccessibilityService when a tracked app is opened from recents
 *   or other non-launcher paths without an active timer (normal mode).
 */
class TimeExpiredOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(AppTimeReminderReceiver.EXTRA_PACKAGE_NAME)
        val appLabel = intent.getStringExtra(AppTimeReminderReceiver.EXTRA_APP_LABEL)

        if (packageName == null || appLabel == null) {
            finish()
            return
        }

        overlayActive = true

        // Intercept system back / swipe-back gesture so it navigates
        // to home screen instead of returning to the tracked app.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateHome()
            }
        })

        val isTimeExpired = intent.getBooleanExtra(EXTRA_IS_TIME_EXPIRED, true)

        val container = (application as LauncherApplication).container
        val manager = container.appTimeReminderManager

        // Read tracked app info from DB (expiry action, default duration)
        val trackedInfo = runBlocking(Dispatchers.IO) {
            manager.getTrackedApp(packageName)
        }

        val defaultAction = trackedInfo?.expiryAction?.let {
            runCatching { ExpiryAction.valueOf(it) }.getOrNull()
        } ?: ExpiryAction.NOTIFICATION

        val defaultDuration = trackedInfo?.defaultDurationMinutes

        val cooldownEnd = if (isTimeExpired) {
            System.currentTimeMillis() + COOLDOWN_DURATION_MS
        } else {
            0L
        }

        setContent {
            TimeIntentionDialog(
                app = AppEntry(packageName = packageName, label = appLabel),
                defaultDurationMinutes = defaultDuration,
                defaultExpiryAction = defaultAction,
                isTimeExpired = isTimeExpired,
                cooldownEndTime = cooldownEnd,
                onConfirm = { durationMinutes, expiryAction ->
                    // Save chosen expiry action for next time
                    CoroutineScope(Dispatchers.IO).launch {
                        manager.updateExpiryAction(packageName, expiryAction)
                    }
                    // Track this package so alarm can be cancelled if user returns
                    // to launcher before the new timer expires
                    AppTimeReminderReceiver.activeReminderPackage = packageName
                    // Schedule the new alarm
                    AppTimeReminderReceiver.schedule(
                        this@TimeExpiredOverlayActivity,
                        packageName,
                        appLabel,
                        durationMinutes,
                        expiryAction.name
                    )
                    finish()
                },
                onDismiss = {
                    navigateHome()
                }
            )
        }
    }

    private fun navigateHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    override fun onDestroy() {
        overlayActive = false
        super.onDestroy()
    }

    companion object {
        private const val COOLDOWN_DURATION_MS = 15_000L
        const val EXTRA_IS_TIME_EXPIRED = "extra_is_time_expired"

        /**
         * Tracks whether an overlay is currently visible to prevent
         * the accessibility service from re-triggering while one is
         * already shown.
         */
        @Volatile
        var overlayActive: Boolean = false
    }
}
