package com.minifocus.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

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

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) {
            instance = null
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
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
    }
}
