package com.minifocus.launcher.permissions

/**
 * DEPRECATED: Device admin integration removed.
 * 
 * Originally used DevicePolicyManager.lockNow() for double-tap lock feature,
 * but that approach disabled biometric unlock and forced PIN-only authentication.
 * 
 * Replaced with LockScreenAccessibilityService which uses GLOBAL_ACTION_LOCK_SCREEN
 * to preserve fingerprint/face unlock behavior like the hardware power button.
 */
