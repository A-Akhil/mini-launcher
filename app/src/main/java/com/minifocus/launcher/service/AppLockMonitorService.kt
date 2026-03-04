/*
 * Minimalist Focus Launcher
 * Copyright (C) 2025 A-Akhil
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.minifocus.launcher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.minifocus.launcher.R
import com.minifocus.launcher.manager.LockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppLockMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var lastCheckedTime = System.currentTimeMillis()
    private lateinit var lockManager: LockManager
    private lateinit var usageStatsManager: UsageStatsManager

    private val screenStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> stopMonitoring()
                Intent.ACTION_SCREEN_ON -> startMonitoring()
            }
        }
    }

    // Removed polling runnable to prevent high CPU usage (DoS).
    // AppLock now relies exclusively on LockScreenAccessibilityService.
    
    override fun onCreate() {
        super.onCreate()
        
        val app = application as com.minifocus.launcher.LauncherApplication
        lockManager = app.container.lockManager
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // Register screen state receiver (keep for cleanup only, or potential future use)
        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // No longer starting monitoring loop. 
        // If Accessibility Service is not enabled, AppLock will simply not function 
        // until the user enables it. This prevents the "Heat" issue.
    }

    private fun startMonitoring() {
       // Loop removed.
    }

    private fun stopMonitoring() {
       // Loop removed.
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopMonitoring()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun checkForLockedAppLaunch() {
        serviceScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                val events = usageStatsManager.queryEvents(lastCheckedTime, currentTime)
                
                while (events.hasNextEvent()) {
                    val event = UsageEvents.Event()
                    events.getNextEvent(event)
                    
                    if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        val packageName = event.packageName
                        
                        if (packageName != this@AppLockMonitorService.packageName &&
                            lockManager.isLocked(packageName)) {
                            
                            val lock = lockManager.getLockInfo(packageName)
                            if (lock != null) {
                                showLockOverlay(packageName, lock.lockedUntil)
                            }
                        }
                    }
                }
                
                lastCheckedTime = currentTime
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showLockOverlay(packageName: String, lockedUntil: Long) {
        val intent = Intent(this, AppLockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_LOCKED_UNTIL, lockedUntil)
        }
        startActivity(intent)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Lock Active")
            .setContentText("Monitoring locked apps")
            .setSmallIcon(R.drawable.ic_notification_summary)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Lock Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors locked apps in background"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 0x4C_4F_43_4B
        private const val CHANNEL_ID = "app_lock_monitor"
        private const val CHECK_INTERVAL_MS = 1000L
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_LOCKED_UNTIL = "locked_until"

        fun start(context: Context) {
            val intent = Intent(context, AppLockMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppLockMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
