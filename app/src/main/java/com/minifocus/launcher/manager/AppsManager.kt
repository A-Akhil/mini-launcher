package com.minifocus.launcher.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.minifocus.launcher.data.dao.PinnedAppDao
import com.minifocus.launcher.data.entity.PinnedAppEntity
import com.minifocus.launcher.model.AppEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.LazyThreadSafetyMode

class AppsManager(
    private val context: Context,
    private val pinnedAppDao: PinnedAppDao,
    private val hiddenAppsManager: HiddenAppsManager,
    private val lockManager: LockManager,
    private val scope: CoroutineScope
) {

    private val packageManager: PackageManager = context.packageManager
    private val selfPackage = context.packageName
    private val installedApps = MutableStateFlow<List<AppEntry>>(emptyList())
    private var isRefreshed = false
    private val essentialSystemPackages: Set<String> by lazy(LazyThreadSafetyMode.NONE) { discoverEssentialSystemPackages() }

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_CHANGED -> {
                    scope.launch(Dispatchers.IO) {
                        isRefreshed = false
                        refreshInstalledApps()
                    }
                }
            }
        }
    }

    init {
        // Initial load
        scope.launch(Dispatchers.IO) { 
            refreshInstalledApps()
        }

        // Register package change listener
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        context.registerReceiver(packageChangeReceiver, filter)
    }

    fun cleanup() {
        context.unregisterReceiver(packageChangeReceiver)
    }

    fun observeAllApps(): Flow<List<AppEntry>> = combine(
        installedApps,
        pinnedAppDao.observePinned(),
        hiddenAppsManager.observeHiddenApps(),
        lockManager.observeLocks()
    ) { installed, pinned, hidden, locks ->
        val pinnedPackages = pinned.associateBy { it.packageName }
        val hiddenPackages = hidden.map { it.packageName }.toSet()
        val lockMap = locks.associateBy { it.packageName }
        installed.map { entry ->
            entry.copy(
                isPinned = pinnedPackages.containsKey(entry.packageName),
                isHidden = hiddenPackages.contains(entry.packageName),
                lockedUntil = lockMap[entry.packageName]?.lockedUntil
            )
        }.sortedBy { it.label.lowercase() }
    }.map { list -> list.filterNot { it.isHidden } }
        .distinctUntilChanged()

    fun observePinnedApps(): Flow<List<AppEntry>> = combine(
        installedApps,
        pinnedAppDao.observePinned(),
        lockManager.observeLocks(),
        hiddenAppsManager.observeHiddenApps()
    ) { installed, pinned, locks, hidden ->
        val lockMap = locks.associateBy { it.packageName }
        val hiddenPackages = hidden.map { it.packageName }.toSet()
        pinned.sortedBy { it.position }.mapNotNull { pinnedEntity ->
            installed.firstOrNull { it.packageName == pinnedEntity.packageName }?.copy(
                isPinned = true,
                isHidden = hiddenPackages.contains(pinnedEntity.packageName),
                lockedUntil = lockMap[pinnedEntity.packageName]?.lockedUntil
            )
        }.filterNot { it.isHidden }
    }.distinctUntilChanged()

    fun observeHiddenApps(): Flow<List<AppEntry>> = combine(
        installedApps,
        hiddenAppsManager.observeHiddenApps(),
        lockManager.observeLocks()
    ) { installed, hidden, locks ->
        val hiddenPackages = hidden.map { it.packageName }.toSet()
        val lockMap = locks.associateBy { it.packageName }
        installed.filter { hiddenPackages.contains(it.packageName) }
            .map { entry -> entry.copy(isHidden = true, lockedUntil = lockMap[entry.packageName]?.lockedUntil) }
            .sortedBy { it.label.lowercase() }
    }.distinctUntilChanged()

    suspend fun pinApp(packageName: String) {
        val nextPosition = withContext(Dispatchers.IO) { pinnedAppDao.getPinnedCount() }
        pinnedAppDao.upsertPinnedApp(
            PinnedAppEntity(
                packageName = packageName,
                position = nextPosition
            )
        )
    }

    suspend fun unpinApp(packageName: String) {
        pinnedAppDao.unpinApp(packageName)
    }

    suspend fun refreshInstalledApps() {
        if (isRefreshed) return

        val pm = packageManager
        val self = selfPackage

        val apps = withContext(Dispatchers.IO) {
            val installedApps = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }

            buildList {
                for (app in installedApps) {
                    if (app.packageName == self) continue
                    if (pm.getLaunchIntentForPackage(app.packageName) == null) continue

                    val label = pm.getApplicationLabel(app).toString()
                    if (!shouldIncludeApp(app)) continue
                    val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    add(
                        AppEntry(
                            packageName = app.packageName,
                            label = label,
                            isSystemApp = isSystemApp
                        )
                    )
                }
            }.sortedBy { it.label.lowercase() }
        }

        installedApps.value = apps
        isRefreshed = true
    }

    private fun shouldIncludeApp(appInfo: ApplicationInfo): Boolean {
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        if (!isSystemApp) {
            return true
        }

        if ((appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
            return true
        }

        if (appInfo.packageName in essentialSystemPackages) {
            return true
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val category = appInfo.category
            if (category != ApplicationInfo.CATEGORY_UNDEFINED) {
                return true
            }
        }

        return false
    }

    private fun discoverEssentialSystemPackages(): Set<String> {
        val pm = packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            Intent(Settings.ACTION_SETTINGS)
        )

        return buildSet {
            for (intent in intents) {
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo
                    ?.packageName
                    ?.let { add(it) }
            }
        }
    }
}
