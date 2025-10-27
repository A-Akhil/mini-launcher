package com.minifocus.launcher

import android.app.role.RoleManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.minifocus.launcher.ui.LauncherApp
import com.minifocus.launcher.ui.theme.MinimalistFocusTheme
import com.minifocus.launcher.viewmodel.LauncherViewModel
import com.minifocus.launcher.viewmodel.LauncherViewModelFactory

class MainActivity : ComponentActivity() {

    private val requestHomeRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // No-op; user choice handled by system UI.
    }

    private val viewModel: LauncherViewModel by viewModels {
        val app = application as LauncherApplication
        LauncherViewModelFactory(
            appsManager = app.container.appsManager,
            tasksManager = app.container.tasksManager,
            hiddenAppsManager = app.container.hiddenAppsManager,
            lockManager = app.container.lockManager,
            settingsManager = app.container.settingsManager,
            searchManager = app.container.searchManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enforce default launcher requirement - app cannot be used otherwise
        if (!isDefaultLauncher()) {
            showDefaultLauncherPrompt()
            return
        }
        
        applySystemBarStyling()
        setContent {
            MinimalistFocusTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                LauncherApp(
                    state = state,
                    onToggleTask = viewModel::toggleTask,
                    onAddTask = viewModel::addTask,
                    onDeleteTask = viewModel::deleteTask,
                    onPinApp = viewModel::pinApp,
                    onUnpinApp = viewModel::unpinApp,
                    onHideApp = viewModel::hideApp,
                    onUnhideApp = viewModel::unhideApp,
                    onLockApp = viewModel::lockApp,
                    onUnlockApp = viewModel::unlockApp,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onSearchVisibilityChange = viewModel::setSearchVisibility,
                    onBottomIconChange = viewModel::setBottomIcon,
                    onSettingsVisibilityChange = viewModel::setSettingsVisibility,
                    onHistoryVisibilityChange = viewModel::setHistoryVisibility,
                    onClockFormatChange = viewModel::setClockFormat,
                    onKeyboardSearchOnSwipeChange = viewModel::setKeyboardSearchOnSwipe,
                    onShowSecondsChange = viewModel::setShowSeconds,
                    onConsumeMessage = viewModel::consumeMessage,
                    canLaunch = viewModel::canLaunch,
                    onLaunchApp = { packageName -> launchPackage(packageName) },
                    onOpenClock = { openClockApp() }
                )
            }
        }
    }

    private fun applySystemBarStyling() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check default launcher status to prevent bypass
        // Note: This may cause issues after phone calls - monitor behavior
        if (!isDefaultLauncher()) {
            showDefaultLauncherPrompt()
        }
    }

    private fun showDefaultLauncherPrompt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                requestHomeRoleLauncher.launch(intent)
            }
        } else {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
        finish()
    }

    private fun launchPackage(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        }
    }

    private fun openClockApp() {
        val pm = packageManager

        // Level 1: The "Official" Intent (Best Method)
        val standardIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (standardIntent.resolveActivity(pm) != null) {
            try {
                startActivity(standardIntent)
                return
            } catch (e: Exception) {
                // Could still fail on some devices
            }
        }

        // Level 2: Hardcoded List (Good Fallback)
        val knownClockPackages = listOf(
            // Google Pixel / AOSP
            "com.google.android.deskclock",
            "com.android.deskclock",

            // Samsung
            "com.samsung.android.app.clockpackage",
            "com.sec.android.app.clockpackage",

            // OnePlus
            "com.oneplus.deskclock",

            // Oppo (ColorOS)
            "com.coloros.alarmclock",

            // Vivo (Funtouch OS / OriginOS)
            "com.vivo.alarmclock",
            "com.bbk.alarmclock",

            // Huawei (EMUI / HarmonyOS)
            "com.huawei.android.deskclock",
            "com.android.util",

            // LG
            "com.lge.clock",
            "com.lge.alarmclock",

            // Motorola
            "com.motorola.blur.alarmclock",
            "com.motorola.timeweatherwidget",

            // Sony
            "com.sonyericsson.organizer",
            "com.sonymobile.tocca.clock",

            // Asus
            "com.asus.deskclock",

            // HTC
            "com.htc.android.worldclock"
        )

        for (pkg in knownClockPackages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                try {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                    return
                } catch (e: Exception) {
                    // Ignore and try next package
                }
            }
        }

        // Level 3: The "Best-Guess" Dynamic Search (Last Resort)
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            
            val appList = pm.queryIntentActivities(mainIntent, 0)

            for (info in appList) {
                val pkgName = info.activityInfo.packageName.lowercase()
                val appLabel = info.loadLabel(pm).toString().lowercase()

                if (pkgName.contains("clock") || appLabel.contains("clock") ||
                    pkgName.contains("alarm") || appLabel.contains("alarm")) {
                    
                    val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launchIntent)
                        return
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Level 4: Failed
        Toast.makeText(this, "Could not find clock app", Toast.LENGTH_SHORT).show()
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        val resolveInfo = packageManager.resolveActivity(intent, 0) ?: return false
        return resolveInfo.activityInfo?.packageName == packageName
    }

    companion object {
        private const val PREFS_NAME = "launcher_preferences"
        private const val KEY_PROMPTED_HOME_ROLE = "prompted_home_role"
    }
}
