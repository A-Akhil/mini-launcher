package com.minifocus.launcher

import android.app.Application
import com.minifocus.launcher.data.AppDatabase
import com.minifocus.launcher.data.datastore.launcherSettingsDataStore
import com.minifocus.launcher.manager.AppsManager
import com.minifocus.launcher.manager.HiddenAppsManager
import com.minifocus.launcher.manager.LockManager
import com.minifocus.launcher.manager.SearchManager
import com.minifocus.launcher.manager.SettingsManager
import com.minifocus.launcher.manager.TasksManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LauncherApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val database = AppDatabase.build(this)
        val tasksManager = TasksManager(database.taskDao())
        val hiddenManager = HiddenAppsManager(database.hiddenAppDao())
        val lockManager = LockManager(database.appLockDao())
        val appsManager = AppsManager(
            context = this,
            pinnedAppDao = database.pinnedAppDao(),
            hiddenAppsManager = hiddenManager,
            lockManager = lockManager,
            scope = appScope
        )
        val settingsManager = SettingsManager(launcherSettingsDataStore)
    val searchManager = SearchManager(appsManager, tasksManager)
        container = AppContainer(
            tasksManager = tasksManager,
            hiddenAppsManager = hiddenManager,
            lockManager = lockManager,
            appsManager = appsManager,
            settingsManager = settingsManager,
            searchManager = searchManager,
            applicationScope = appScope
        )
    }
}

class AppContainer(
    val tasksManager: TasksManager,
    val hiddenAppsManager: HiddenAppsManager,
    val lockManager: LockManager,
    val appsManager: AppsManager,
    val settingsManager: SettingsManager,
    val searchManager: SearchManager,
    val applicationScope: CoroutineScope
)
