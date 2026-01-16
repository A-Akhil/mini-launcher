package com.minifocus.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
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
        
        // Launch coroutine to check database
        serviceScope.launch {
            checkAndLock(eventPackageName)
        }
    }
    
    private suspend fun checkAndLock(targetPackage: String) {
        val app = applicationContext as? com.minifocus.launcher.LauncherApplication ?: return
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
