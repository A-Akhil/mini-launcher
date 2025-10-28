package com.minifocus.launcher

import android.app.Application
import com.minifocus.launcher.data.AppDatabase
import com.minifocus.launcher.data.datastore.launcherSettingsDataStore
import com.minifocus.launcher.manager.AppsManager
import com.minifocus.launcher.manager.HiddenAppsManager
import com.minifocus.launcher.manager.InboxLogger
import com.minifocus.launcher.manager.LockManager
import com.minifocus.launcher.manager.NotificationInboxManager
import com.minifocus.launcher.manager.SearchManager
import com.minifocus.launcher.manager.SettingsManager
import com.minifocus.launcher.manager.TasksManager
import com.minifocus.launcher.worker.NotificationMaintenanceWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LauncherApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val database = AppDatabase.build(this)
        val settingsManager = SettingsManager(launcherSettingsDataStore)
        val inboxLogger = InboxLogger(
            context = this,
            onRotation = { timestamp ->
                appScope.launch { settingsManager.setLastLogRotation(timestamp) }
            }
        )
        val tasksManager = TasksManager(database.taskDao(), this)
        val hiddenManager = HiddenAppsManager(database.hiddenAppDao())
        val lockManager = LockManager(database.appLockDao())
        val appsManager = AppsManager(
            context = this,
            pinnedAppDao = database.pinnedAppDao(),
            hiddenAppsManager = hiddenManager,
            lockManager = lockManager,
            scope = appScope
        )
        val searchManager = SearchManager(appsManager, tasksManager)
        val notificationInboxManager = NotificationInboxManager(
            context = this,
            notificationDao = database.notificationDao(),
            notificationFilterDao = database.notificationFilterDao(),
            logger = inboxLogger
        )
        container = AppContainer(
            tasksManager = tasksManager,
            hiddenAppsManager = hiddenManager,
            lockManager = lockManager,
            appsManager = appsManager,
            settingsManager = settingsManager,
            searchManager = searchManager,
            notificationInboxManager = notificationInboxManager,
            inboxLogger = inboxLogger,
            applicationScope = appScope
        )

        appScope.launch {
            settingsManager.observeNotificationRetentionDays().collectLatest { days ->
                notificationInboxManager.updateRetention(days)
            }
        }

        appScope.launch {
            settingsManager.observeLogRetentionDays().collectLatest { days ->
                notificationInboxManager.updateLogRetention(days)
            }
        }

        appScope.launch {
            notificationInboxManager.trimExpired()
        }

        NotificationMaintenanceWorker.schedule(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        container.appsManager.cleanup()
    }
}

class AppContainer(
    val tasksManager: TasksManager,
    val hiddenAppsManager: HiddenAppsManager,
    val lockManager: LockManager,
    val appsManager: AppsManager,
    val settingsManager: SettingsManager,
    val searchManager: SearchManager,
    val notificationInboxManager: NotificationInboxManager,
    val inboxLogger: InboxLogger,
    val applicationScope: CoroutineScope
)
