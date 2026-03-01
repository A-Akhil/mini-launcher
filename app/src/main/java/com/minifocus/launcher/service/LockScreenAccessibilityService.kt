package com.minifocus.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.minifocus.launcher.LauncherApplication
import com.minifocus.launcher.service.AppLockOverlayActivity
import com.minifocus.launcher.service.AppLockMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Accessibility service used to issue a global lock action so the device behaves
 * like a hardware power-button press (biometrics remain available).
 *
 * WHY ACCESSIBILITY SERVICE INSTEAD OF DEVICE ADMIN:
 * Initially tried DevicePolicyManager.lockNow() which worked for locking,
 * but forced PIN-only unlock and disabled biometric authentication.
 * Switched to accessibility service GLOBAL_ACTION_LOCK_SCREEN to preserve
 * biometric unlock behavior (fingerprint/face) just like pressing power button.
 */
class LockScreenAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Package we last intercepted for time-reminder overlay. */
    @Volatile
    private var lastInterceptedPackage: String? = null

    /** Timestamp of last interception to debounce rapid re-triggers. */
    @Volatile
    private var lastInterceptTime: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        
        val eventPackageName = event.packageName?.toString() ?: return
        
        // Don't lock ourselves or System UI
        if (eventPackageName == packageName) return

        // When the launcher (home screen) comes to foreground, clear the
        // time-reminder debounce so the next recents launch is intercepted.
        if (eventPackageName == "com.minifocus.launcher") {
            lastInterceptedPackage = null
            return
        }
        
        // Launch coroutine to check database
        serviceScope.launch {
            checkAndLock(eventPackageName)
        }
    }
    
    private suspend fun checkAndLock(targetPackage: String) {
        val app = applicationContext as? LauncherApplication ?: return
        val lockManager = app.container.lockManager
        
        if (lockManager.isLocked(targetPackage)) {
            val lockInfo = lockManager.getLockInfo(targetPackage)
            if (lockInfo != null) {
                withContext(Dispatchers.Main) {
                    // Launch overlay
                    val intent = Intent(this@LockScreenAccessibilityService, AppLockOverlayActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra(AppLockMonitorService.EXTRA_PACKAGE_NAME, targetPackage)
                        putExtra(AppLockMonitorService.EXTRA_LOCKED_UNTIL, lockInfo.lockedUntil)
                    }
                    startActivity(intent)
                }
            }
            return
        }

        // If the app is not locked, check whether it is a tracked
        // time-reminder app opened without an active timer
        // (e.g. via recent apps or external launch).
        checkTimeReminder(app, targetPackage)
    }

    /**
     * Shows a time-intention dialog overlay when a tracked app is
     * opened from recents or any non-launcher path while there is
     * no active alarm running for it.
     */
    private suspend fun checkTimeReminder(app: LauncherApplication, targetPackage: String) {
        // Already has an active timer -- user set time through launcher
        if (AppTimeReminderReceiver.activeReminderPackage == targetPackage) return

        // An overlay is already being displayed -- avoid stacking
        if (TimeExpiredOverlayActivity.overlayActive) return

        // Debounce: skip if we very recently intercepted this package
        // (handles race between overlay dismiss and app re-focus)
        val now = System.currentTimeMillis()
        if (targetPackage == lastInterceptedPackage && now - lastInterceptTime < DEBOUNCE_MS) return

        val manager = app.container.appTimeReminderManager
        val tracked = withContext(Dispatchers.IO) {
            manager.isTracked(targetPackage)
        }
        if (!tracked) return

        val trackedInfo = withContext(Dispatchers.IO) {
            manager.getTrackedApp(targetPackage)
        }
        val appLabel = trackedInfo?.appLabel ?: targetPackage

        lastInterceptedPackage = targetPackage
        lastInterceptTime = now

        withContext(Dispatchers.Main) {
            val intent = Intent(
                this@LockScreenAccessibilityService,
                TimeExpiredOverlayActivity::class.java
            ).apply {
                putExtra(AppTimeReminderReceiver.EXTRA_PACKAGE_NAME, targetPackage)
                putExtra(AppTimeReminderReceiver.EXTRA_APP_LABEL, appLabel)
                putExtra(TimeExpiredOverlayActivity.EXTRA_IS_TIME_EXPIRED, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) {
            instance = null
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instance: LockScreenAccessibilityService? = null

        /** Minimum interval between time-reminder overlay triggers for the same package. */
        private const val DEBOUNCE_MS = 3_000L

        /**
         * Attempts to lock the device via a global lock-screen action.
         *
         * @return true if the global action was dispatched, false if the service
         * is not yet enabled or the action failed.
         */
        fun lockDevice(): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }

        fun isEnabled(): Boolean {
            return instance != null
        }
    }
}
