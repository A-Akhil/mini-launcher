package com.minifocus.launcher.manager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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

class AppsManager(
    context: Context,
    private val pinnedAppDao: PinnedAppDao,
    private val hiddenAppsManager: HiddenAppsManager,
    private val lockManager: LockManager,
    private val scope: CoroutineScope
) {

    private val packageManager: PackageManager = context.packageManager
    private val selfPackage = context.packageName
    private val installedApps = MutableStateFlow<List<AppEntry>>(emptyList())

    init {
        scope.launch { refreshInstalledApps() }
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
        val pinnedMap = pinned.associateBy { it.packageName }
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
        val apps = withContext(Dispatchers.IO) {
            val installed = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            }
            installed
                .filter { app ->
                    packageManager.getLaunchIntentForPackage(app.packageName) != null &&
                        app.packageName != selfPackage
                }
                .map { appInfo ->
                    val label = packageManager.getApplicationLabel(appInfo).toString()
                    AppEntry(packageName = appInfo.packageName, label = label)
                }
        }
        installedApps.value = apps
    }
}
