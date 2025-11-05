package com.minifocus.launcher

import android.Manifest
import android.app.AlarmManager
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.widget.Toast
import android.service.notification.NotificationListenerService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import com.minifocus.launcher.R
import com.minifocus.launcher.ui.LauncherApp
import com.minifocus.launcher.ui.PermissionScreen
import com.minifocus.launcher.ui.theme.MinimalistFocusTheme
import com.minifocus.launcher.viewmodel.LauncherViewModel
import com.minifocus.launcher.viewmodel.LauncherViewModelFactory
import com.minifocus.launcher.viewmodel.NotificationFilterViewModel
import com.minifocus.launcher.viewmodel.NotificationFilterViewModelFactory
import com.minifocus.launcher.viewmodel.NotificationInboxViewModel
import com.minifocus.launcher.viewmodel.NotificationInboxViewModelFactory
import com.minifocus.launcher.permissions.PermissionsState
import com.minifocus.launcher.permissions.PermissionsEvaluator
import com.minifocus.launcher.permissions.NotificationInboxListenerService
import com.minifocus.launcher.permissions.LauncherDeviceAdminReceiver
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val requestHomeRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // No-op; user choice handled by system UI.
    }

    private val requestPostNotificationsLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        updatePermissionsState()
    }

    private val notificationAccessLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updatePermissionsState()
    }

    private val deviceAdminLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updatePermissionsState()
    }

    private val exactAlarmLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updatePermissionsState()
    }

    private val usageStatsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updatePermissionsState()
    }

    private val overlayLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        updatePermissionsState()
    }

    private val permissionsState = MutableStateFlow(
        PermissionsState(
            notificationsGranted = false,
            notificationListenerGranted = false,
            deviceAdminGranted = false,
            exactAlarmsGranted = false,
            usageStatsGranted = false,
            overlayGranted = false
        )
    )

    private val notificationRestrictionHint = MutableStateFlow(false)
    private var notificationAccessRequested = false

    private val viewModel: LauncherViewModel by viewModels {
        val app = application as LauncherApplication
        LauncherViewModelFactory(
            appsManager = app.container.appsManager,
            tasksManager = app.container.tasksManager,
            dailyTasksManager = app.container.dailyTasksManager,
            hiddenAppsManager = app.container.hiddenAppsManager,
            lockManager = app.container.lockManager,
            settingsManager = app.container.settingsManager,
            searchManager = app.container.searchManager
        )
    }

    private val notificationInboxViewModel: NotificationInboxViewModel by viewModels {
        val app = application as LauncherApplication
        NotificationInboxViewModelFactory(
            inboxManager = app.container.notificationInboxManager,
            settingsManager = app.container.settingsManager
        )
    }

    private val notificationFilterViewModel: NotificationFilterViewModel by viewModels {
        val app = application as LauncherApplication
        NotificationFilterViewModelFactory(
            notificationInboxManager = app.container.notificationInboxManager,
            appsManager = app.container.appsManager
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
        notificationRestrictionHint.value = !isFromTrustedStore()
        updatePermissionsState()
        setContent {
            MinimalistFocusTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val inboxState by notificationInboxViewModel.uiState.collectAsStateWithLifecycle()
                val filterState by notificationFilterViewModel.uiState.collectAsStateWithLifecycle()
                val permissions by permissionsState.collectAsStateWithLifecycle()
                val restrictedHint by notificationRestrictionHint.collectAsStateWithLifecycle()
                if (permissions.allGranted) {
                    LauncherApp(
                        state = state,
                        notificationInboxState = inboxState,
                        notificationFilterState = filterState,
                        onToggleTask = viewModel::toggleTask,
                        onAddTask = viewModel::addTask,
                        onDeleteTask = viewModel::deleteTask,
                        onAddDailyTask = viewModel::addDailyTask,
                        onUpdateDailyTask = viewModel::updateDailyTask,
                        onDeleteDailyTask = viewModel::deleteDailyTask,
                        onDailyTaskEnabledChange = viewModel::setDailyTaskEnabled,
                        onDailyTaskCompleted = viewModel::markDailyTaskCompleted,
                        onDailyTaskReset = viewModel::resetDailyTaskForToday,
                        onPinApp = viewModel::pinApp,
                        onUnpinApp = viewModel::unpinApp,
                        onHideApp = viewModel::hideApp,
                        onUnhideApp = viewModel::unhideApp,
                        onLockApp = viewModel::lockApp,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onSearchVisibilityChange = viewModel::setSearchVisibility,
                        onBottomIconChange = viewModel::setBottomIcon,
                        onSettingsVisibilityChange = viewModel::setSettingsVisibility,
                        onHomeSettingsVisibilityChange = viewModel::setHomeSettingsVisibility,
                        onClockSettingsVisibilityChange = viewModel::setClockSettingsVisibility,
                        onAppDrawerSettingsVisibilityChange = viewModel::setAppDrawerSettingsVisibility,
                        onHistoryVisibilityChange = viewModel::setHistoryVisibility,
                        onAboutVisibilityChange = viewModel::setAboutVisibility,
                        onEmergencyUnlockVisibilityChange = viewModel::setEmergencyUnlockVisibility,
                        onClockFormatChange = viewModel::setClockFormat,
                        onKeyboardSearchOnSwipeChange = viewModel::setKeyboardSearchOnSwipe,
                        onShowSecondsChange = viewModel::setShowSeconds,
                        onShowDailyTasksOnHomeChange = viewModel::setShowDailyTasksOnHome,
                        onNotificationInboxEnabledChange = viewModel::setNotificationInboxEnabled,
                        onNotificationInboxVisibilityChange = viewModel::setNotificationInboxVisibility,
                        onNotificationSettingsVisibilityChange = viewModel::setNotificationSettingsVisibility,
                        onNotificationFilterVisibilityChange = viewModel::setNotificationFilterVisibility,
                        onNotificationRetentionSelected = notificationInboxViewModel::setNotificationRetentionDays,
                        onLogRetentionSelected = notificationInboxViewModel::setLogRetentionDays,
                        onNotificationDelete = notificationInboxViewModel::deleteNotification,
                        onNotificationMarkAllRead = notificationInboxViewModel::markAllAsRead,
                        onNotificationFilterQueryChange = notificationFilterViewModel::updateSearchQuery,
                        onNotificationFilterToggle = notificationFilterViewModel::toggle,
                        canLaunch = viewModel::canLaunch,
                        onLaunchApp = { packageName -> launchPackage(packageName) },
                        onOpenClock = { openClockApp() },
                        lockManager = (application as LauncherApplication).container.lockManager
                    )
                } else {
                    PermissionScreen(
                        state = permissions,
                        onRequestNotifications = ::requestPostNotificationsPermission,
                        onRequestNotificationListener = ::requestNotificationAccess,
                        onRequestExactAlarms = ::requestExactAlarmPermission,
                        onRequestDeviceAdmin = ::requestDeviceAdmin,
                        onRequestUsageStats = ::requestUsageStatsPermission,
                        onRequestOverlay = ::requestOverlayPermission,
                        showRestrictedNotificationHint = restrictedHint,
                        onOpenRestrictedSettings = ::openRestrictedSettings
                    )
                }
            }
        }

        if (intent?.action == ACTION_OPEN_INBOX) {
            viewModel.setNotificationInboxVisibility(true)
            intent.action = Intent.ACTION_MAIN
        }
    }

    private fun requestPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!permissionsState.value.notificationsGranted) {
                requestPostNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            updatePermissionsState()
        }
    }

    private fun requestNotificationAccess() {
        if (permissionsState.value.notificationListenerGranted) {
            updatePermissionsState()
            return
        }
        notificationAccessRequested = true
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        notificationAccessLauncher.launch(intent)
    }

    private fun requestDeviceAdmin() {
        if (permissionsState.value.deviceAdminGranted) {
            updatePermissionsState()
            return
        }
        val component = ComponentName(this, LauncherDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.device_admin_explanation))
        }
        deviceAdminLauncher.launch(intent)
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            updatePermissionsState()
            return
        }

        if (permissionsState.value.exactAlarmsGranted) {
            updatePermissionsState()
            return
        }

        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        if (intent.resolveActivity(packageManager) != null) {
            exactAlarmLauncher.launch(intent)
        } else {
            openRestrictedSettings()
        }
    }

    private fun requestUsageStatsPermission() {
        if (permissionsState.value.usageStatsGranted) {
            updatePermissionsState()
            return
        }
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        usageStatsLauncher.launch(intent)
    }

    private fun requestOverlayPermission() {
        if (permissionsState.value.overlayGranted) {
            updatePermissionsState()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayLauncher.launch(intent)
        }
    }

    private fun updatePermissionsState() {
        val state = PermissionsEvaluator.currentState(this)
        permissionsState.value = state
        if (notificationAccessRequested && !state.notificationListenerGranted) {
            notificationRestrictionHint.value = !isFromTrustedStore()
        } else if (state.notificationListenerGranted) {
            notificationAccessRequested = false
            notificationRestrictionHint.value = false
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(this, NotificationInboxListenerService::class.java)
                )
            } catch (_: Exception) {
                // Ignore; system will bind when possible.
            }
        }
        
        if (state.allGranted) {
            startAppLockMonitorService()
        }
    }

    private fun startAppLockMonitorService() {
        try {
            val serviceIntent = Intent(this, com.minifocus.launcher.service.AppLockMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
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
            return
        }
        updatePermissionsState()
    }

    private fun openRestrictedSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun isFromTrustedStore(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName == "com.android.vending"
            } else {
                @Suppress("DEPRECATION")
                val installer = packageManager.getInstallerPackageName(packageName)
                installer == "com.android.vending"
            }
        } catch (_: Exception) {
            false
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
        // Check if app is locked first (using runBlocking since we need to check before launching)
        val container = (application as LauncherApplication).container
        val lockInfo = runBlocking { container.lockManager.getLockInfo(packageName) }
        
        if (lockInfo != null && lockInfo.lockedUntil > System.currentTimeMillis()) {
            // App is locked - show overlay instead of launching
            val overlayIntent = Intent(this, com.minifocus.launcher.service.AppLockOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(com.minifocus.launcher.service.AppLockMonitorService.EXTRA_PACKAGE_NAME, packageName)
                putExtra(com.minifocus.launcher.service.AppLockMonitorService.EXTRA_LOCKED_UNTIL, lockInfo.lockedUntil)
            }
            startActivity(overlayIntent)
            return
        }
        
        // App is not locked, launch normally
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        when (intent.action) {
            ACTION_OPEN_INBOX -> {
                viewModel.setNotificationInboxVisibility(true)
            }
            Intent.ACTION_MAIN -> {
                if (intent.hasCategory(Intent.CATEGORY_HOME)) {
                    viewModel.resetToHome()
                }
            }
        }
        intent.action = Intent.ACTION_MAIN
    }

    companion object {
        private const val PREFS_NAME = "launcher_preferences"
        private const val KEY_PROMPTED_HOME_ROLE = "prompted_home_role"
        const val ACTION_OPEN_INBOX = "com.minifocus.launcher.action.OPEN_INBOX"
    }
}
