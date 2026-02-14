package com.minifocus.launcher.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * HARDLOCK BRANCH ONLY - NOT FOR PLAY STORE
 * 
 * This service intercepts Settings screens and prevents users from accessing
 * app info, device admin, or default apps pages. It VIOLATES Google Play's
 * "Mobile Unwanted Software" policy and must NEVER be included in any
 * Play-distributed build.
 * 
 * WARNING: Using this service:
 * - Voids all Play Store eligibility
 * - May be flagged as malware by security scanners
 * - Requires explicit user consent and escape documentation
 * 
 * Users can disable this by going to:
 * Settings > Accessibility > Downloaded Services > Hardlock Guard > Turn Off
 * Or via ADB: adb shell pm disable com.minifocus.launcher/.service.HardlockAccessibilityService
 */
class HardlockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Process all relevant event types for better coverage
        val relevantTypes = listOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        )
        
        if (event.eventType !in relevantTypes) {
            return
        }
        
        val packageName = event.packageName?.toString() ?: ""
        
        // Only monitor Settings app
        if (packageName != "com.android.settings") {
            return
        }
        
        val contentDescription = event.contentDescription?.toString()?.lowercase() ?: ""
        val text = event.text.joinToString(" ").lowercase()
        val className = event.className?.toString() ?: ""

        // Detect app info for this launcher
        val isMiniSettings = text.contains("mini") || 
                             text.contains("minimalist focus") ||
                             contentDescription.contains("mini") ||
                             contentDescription.contains("minimalist focus") ||
                             text.contains("com.minifocus.launcher")

        // Detect dangerous system menus
        val isDangerousMenu = text.contains("device admin") || 
                              text.contains("default home") || 
                              text.contains("default apps") ||
                              text.contains("home app") ||
                              text.contains("choose home") ||
                              text.contains("launcher") ||
                              contentDescription.contains("device admin") ||
                              contentDescription.contains("default home") ||
                              contentDescription.contains("home app") ||
                              className.contains("AppInfoDashboardFragment") ||
                              className.contains("DefaultHomePreference")

        if (isMiniSettings || isDangerousMenu) {
            // Force user back to launcher
            performGlobalAction(GLOBAL_ACTION_HOME)
            
            // Kill Settings process first
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    Log.d(TAG, "Killing Settings process...")
                    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    activityManager.killBackgroundProcesses("com.android.settings")
                    Runtime.getRuntime().exec("am force-stop com.android.settings")
                    Log.d(TAG, "Killed Settings process")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to kill Settings", e)
                }
            }, 100)
            
            // Open recents
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Opening recents...")
                performGlobalAction(GLOBAL_ACTION_RECENTS)
            }, 500)
            
            // Wait for recents to load, then swipe away Settings
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    Log.d(TAG, "Swiping Settings away...")
                    swipeAwaySettingsFromRecents()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to swipe Settings away", e)
                }
            }, 1200)
            
            // After swipe, click "Clear All" to remove remaining apps
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val rootNode = rootInActiveWindow
                    if (rootNode != null) {
                        Log.d(TAG, "Clicking Clear All button...")
                        val cleared = findAndClickClearAll(rootNode)
                        if (cleared) {
                            Log.d(TAG, "Successfully clicked Clear All")
                        } else {
                            Log.w(TAG, "Could not find Clear All button")
                        }
                        rootNode.recycle()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to click Clear All", e)
                }
            }, 1800)
            
            // Return home
            Handler(Looper.getMainLooper()).postDelayed({
                performGlobalAction(GLOBAL_ACTION_HOME)
            }, 2200)
            
            // Show blocking message
            Toast.makeText(
                this, 
                "Hardlock Active: Sorry but time to focus.", 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun swipeAwaySettingsFromRecents() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(TAG, "Gesture dispatch requires Android 7.0+")
            return
        }
        
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Log.w(TAG, "Could not get root window for swipe")
            return
        }
        
        try {
            // Find Settings card in recents
            val settingsNode = findSettingsCard(rootNode)
            if (settingsNode == null) {
                Log.w(TAG, "Could not find Settings card in recents")
                rootNode.recycle()
                return
            }
            
            // Get the bounds of the Settings card
            val bounds = android.graphics.Rect()
            settingsNode.getBoundsInScreen(bounds)
            Log.d(TAG, "Found Settings card at: $bounds")
            
            // Calculate swipe path (swipe up to dismiss)
            val startX = bounds.centerX().toFloat()
            val startY = bounds.centerY().toFloat()
            val endY = 0f // Swipe to top of screen
            
            val path = Path()
            path.moveTo(startX, startY)
            path.lineTo(startX, endY)
            
            val gestureBuilder = GestureDescription.Builder()
            val gesture = GestureDescription.StrokeDescription(path, 0, 200) // 200ms swipe
            gestureBuilder.addStroke(gesture)
            
            val success = dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Swipe gesture completed successfully")
                }
                
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "Swipe gesture was cancelled")
                }
            }, null)
            
            if (!success) {
                Log.w(TAG, "Failed to dispatch swipe gesture")
            }
            
            settingsNode.recycle()
            rootNode.recycle()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during swipe", e)
        }
    }
    
    private fun findSettingsCard(node: android.view.accessibility.AccessibilityNodeInfo?): android.view.accessibility.AccessibilityNodeInfo? {
        if (node == null) return null
        
        // Look for Settings text in recents cards
        val settingsTexts = listOf("Settings", "settings", "System", "设置")
        for (text in settingsTexts) {
            val nodes = node.findAccessibilityNodeInfosByText(text)
            if (nodes.isNotEmpty()) {
                // Return the parent card container
                var parent = nodes[0].parent
                while (parent != null) {
                    val className = parent.className?.toString() ?: ""
                    // Look for RecyclerView item or card container
                    if (className.contains("TaskView") || 
                        className.contains("CardView") ||
                        className.contains("FrameLayout") && parent.isClickable) {
                        return parent
                    }
                    parent = parent.parent
                }
                return nodes[0]
            }
        }
        
        return null
    }

    private fun findAndClickClearAll(node: android.view.accessibility.AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        
        // Common texts for Clear All button across different OEMs
        val clearTexts = listOf(
            "Clear all", "Close all", "clear all", "close all",
            "Xóa tất cả", "全部清除", "Limpiar todo", "すべて閉じる",
            "Clear", "Close", "Delete all"
        )
        
        // Try finding by text
        for (text in clearTexts) {
            try {
                val clearNodes = node.findAccessibilityNodeInfosByText(text)
                if (clearNodes.isNotEmpty()) {
                    for (clearNode in clearNodes) {
                        if (clearNode.isClickable) {
                            Log.d(TAG, "Found Clear All by text: $text")
                            val clicked = clearNode.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                            clearNode.recycle()
                            if (clicked) return true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error finding by text: $text", e)
            }
        }
        
        // Try common view IDs (OEM-specific)
        val commonViewIds = listOf(
            "com.android.systemui:id/dismiss_task_button",
            "com.android.systemui:id/clear_all_button",
            "com.android.systemui:id/button",
            "android:id/button",
            "com.android.launcher3:id/clear_all",
            "com.miui.home:id/clear_all_button",  // Xiaomi
            "com.oppo.launcher:id/clear_all",      // Oppo/Realme
            "com.huawei.android.launcher:id/clear_all"  // Huawei
        )
        
        for (viewId in commonViewIds) {
            try {
                val idNodes = node.findAccessibilityNodeInfosByViewId(viewId)
                if (idNodes.isNotEmpty()) {
                    for (idNode in idNodes) {
                        if (idNode.isClickable) {
                            Log.d(TAG, "Found Clear All by ID: $viewId")
                            val clicked = idNode.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                            idNode.recycle()
                            if (clicked) return true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error finding by ID: $viewId", e)
            }
        }
        
        // Recurse through children (for icon-only buttons)
        try {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    // Look for buttons at the bottom of screen (common for Clear All)
                    val bounds = android.graphics.Rect()
                    child.getBoundsInScreen(bounds)
                    
                    // If it's a clickable element near bottom and looks like a button
                    if (child.isClickable && child.className?.contains("Button") == true) {
                        Log.d(TAG, "Found potential Clear All button at bottom: ${child.className}")
                        val clicked = child.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                        if (clicked) {
                            child.recycle()
                            return true
                        }
                    }
                    
                    val found = findAndClickClearAll(child)
                    child.recycle()
                    if (found) return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recursing children", e)
        }
        
        return false
    }

    companion object {
        private const val TAG = "HardlockAccessibility"
    }
}
