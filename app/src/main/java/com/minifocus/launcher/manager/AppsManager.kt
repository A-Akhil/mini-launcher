package com.minifocus.launcher.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.telecom.TelecomManager
import com.minifocus.launcher.data.dao.PinnedAppDao
import com.minifocus.launcher.data.entity.PinnedAppEntity
import com.minifocus.launcher.model.AppEntry
import com.minifocus.launcher.model.BottomIconSlot
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

    fun resolveDefaultQuickLaunch(slot: BottomIconSlot, apps: List<AppEntry>): String? {
        val availablePackages = apps.map { it.packageName }.toSet()
        if (availablePackages.isEmpty()) return null
        return when (slot) {
            BottomIconSlot.LEFT -> resolveDialerPackage(availablePackages)
            BottomIconSlot.RIGHT -> resolveCameraPackage(availablePackages)
        }
    }

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

        if (hasLauncherActivity(appInfo.packageName)) {
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
        val baseIntents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA),
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            Intent(Settings.ACTION_SETTINGS)
        )
        val categoryIntents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_FILES),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MAPS),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MARKET),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_WEATHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
        )

        return buildSet {
            (baseIntents + categoryIntents).forEach { intent ->
                queryPackages(intent).forEach { add(it) }
            }
        }
    }

    private fun hasLauncherActivity(packageName: String): Boolean {
        val launcherIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(packageName)
        return queryPackages(launcherIntent, flags = 0).any()
    }

    private fun resolveDialerPackage(availablePackages: Set<String>): String? {
        val telecomManager = context.getSystemService(TelecomManager::class.java)
        val defaultDialer = telecomManager?.defaultDialerPackage
        if (defaultDialer != null && defaultDialer in availablePackages) {
            return defaultDialer
        }

        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_CALL_BUTTON),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS)
        )

        intents.forEach { intent ->
            queryPackages(intent).forEach { packageName ->
                if (packageName in availablePackages) {
                    return packageName
                }
            }
        }

        return null
    }

    private fun resolveCameraPackage(availablePackages: Set<String>): String? {
        val intents = listOf(
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA)
        )

        intents.forEach { intent ->
            queryPackages(intent).forEach { packageName ->
                if (packageName in availablePackages) {
                    return packageName
                }
            }
        }

        return null
    }

    private fun queryPackages(intent: Intent, flags: Int = PackageManager.MATCH_DEFAULT_ONLY): Sequence<String> {
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, flags)
        }
        return resolveInfos.asSequence()
            .mapNotNull { info -> info.activityInfo?.packageName }
    }
}
