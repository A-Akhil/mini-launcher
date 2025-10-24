package com.minifocus.launcher

import android.app.role.RoleManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Build
import android.provider.Settings
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
                    onClockFormatChange = viewModel::setClockFormat,
                    onKeyboardSearchOnSwipeChange = viewModel::setKeyboardSearchOnSwipe,
                    onConsumeMessage = viewModel::consumeMessage,
                    canLaunch = viewModel::canLaunch,
                    onLaunchApp = { packageName -> launchPackage(packageName) }
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
        maybePromptForDefaultLauncher()
    }

    private fun launchPackage(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        }
    }

    private fun maybePromptForDefaultLauncher() {
        if (isDefaultLauncher() || alreadyPromptedForHomeRole()) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                requestHomeRoleLauncher.launch(intent)
                markPromptedForHomeRole()
            }
        } else {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                markPromptedForHomeRole()
            }
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        val resolveInfo = packageManager.resolveActivity(intent, 0) ?: return false
        return resolveInfo.activityInfo?.packageName == packageName
    }

    private fun alreadyPromptedForHomeRole(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getBoolean(KEY_PROMPTED_HOME_ROLE, false)
    }

    private fun markPromptedForHomeRole() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PROMPTED_HOME_ROLE, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "launcher_preferences"
        private const val KEY_PROMPTED_HOME_ROLE = "prompted_home_role"
    }
}
