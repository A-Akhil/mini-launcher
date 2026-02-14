package com.minifocus.launcher.receiver

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/** Minimal device-admin receiver for the hardlock build. */
class HardlockDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        updateUninstallBlock(context, true)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        updateUninstallBlock(context, false)
    }

    private fun updateUninstallBlock(context: Context, blocked: Boolean) {
        val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)
        val adminComponent = ComponentName(context, HardlockDeviceAdminReceiver::class.java)
        if (devicePolicyManager == null) {
            Log.w(TAG, "DevicePolicyManager unavailable; cannot change uninstall block")
            return
        }
        runCatching {
            devicePolicyManager.setUninstallBlocked(adminComponent, context.packageName, blocked)
            Log.i(TAG, "setUninstallBlocked=$blocked for ${context.packageName}")
        }.onFailure { error ->
            Log.e(TAG, "Failed to change uninstall block", error)
        }
    }

    companion object {
        private const val TAG = "HardlockDeviceAdmin"
    }
}
