package com.minifocus.launcher.manager

import android.content.Context
import android.content.pm.PackageManager
import com.minifocus.launcher.data.dao.AppTimeReminderDao
import com.minifocus.launcher.data.entity.AppTimeReminderEntity
import com.minifocus.launcher.model.ExpiryAction
import kotlinx.coroutines.flow.Flow

class AppTimeReminderManager(
    private val dao: AppTimeReminderDao
) {
    fun observeTrackedApps(): Flow<List<AppTimeReminderEntity>> = dao.observeAll()

    suspend fun isTracked(packageName: String): Boolean =
        dao.isTracked(packageName) > 0

    suspend fun getTrackedApp(packageName: String): AppTimeReminderEntity? =
        dao.getByPackage(packageName)

    suspend fun addTrackedApp(
        packageName: String,
        appLabel: String,
        defaultDurationMinutes: Int? = null,
        expiryAction: ExpiryAction = ExpiryAction.NOTIFICATION
    ) {
        dao.upsert(
            AppTimeReminderEntity(
                packageName = packageName,
                appLabel = appLabel,
                defaultDurationMinutes = defaultDurationMinutes,
                expiryAction = expiryAction.name
            )
        )
    }

    suspend fun updateTrackedApp(
        packageName: String,
        appLabel: String,
        defaultDurationMinutes: Int?,
        expiryAction: ExpiryAction
    ) {
        dao.upsert(
            AppTimeReminderEntity(
                packageName = packageName,
                appLabel = appLabel,
                defaultDurationMinutes = defaultDurationMinutes,
                expiryAction = expiryAction.name
            )
        )
    }

    suspend fun updateExpiryAction(packageName: String, expiryAction: ExpiryAction) {
        dao.updateExpiryAction(packageName, expiryAction.name)
    }

    suspend fun removeTrackedApp(packageName: String) {
        dao.delete(packageName)
    }

    /**
     * Seeds the table with well-known social media and gaming apps
     * that are actually installed on the device. Only runs if the
     * table is completely empty (first launch).
     */
    suspend fun seedDefaultsIfEmpty(context: Context) {
        if (dao.count() > 0) return

        val pm = context.packageManager
        val defaults = DEFAULT_TRACKED_APPS.mapNotNull { (pkg, label) ->
            if (isInstalled(pm, pkg)) {
                val resolvedLabel = resolveAppLabel(pm, pkg) ?: label
                AppTimeReminderEntity(
                    packageName = pkg,
                    appLabel = resolvedLabel,
                    defaultDurationMinutes = null,
                    expiryAction = "NOTIFICATION"
                )
            } else null
        }
        if (defaults.isNotEmpty()) {
            dao.insertIfAbsent(defaults)
        }
    }

    private fun isInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun resolveAppLabel(pm: PackageManager, packageName: String): String? {
        return try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        // Social media and messaging apps
        private val DEFAULT_TRACKED_APPS = listOf(
            "com.instagram.android" to "Instagram",
            "com.twitter.android" to "X",
            "com.reddit.frontpage" to "Reddit",
            "com.discord" to "Discord",
            "com.snapchat.android" to "Snapchat",
            "com.facebook.katana" to "Facebook",
            "com.zhiliaoapp.musically" to "TikTok",
            "com.linkedin.android" to "LinkedIn",
            "org.telegram.messenger" to "Telegram",
            "com.pinterest" to "Pinterest",
            "com.tumblr" to "Tumblr",
            // Video streaming
            "com.google.android.youtube" to "YouTube",
            "com.netflix.mediaclient" to "Netflix",
            "in.startv.hotstar" to "Hotstar",
            "com.amazon.avod.thirdpartyclient" to "Prime Video",
            // Games
            "com.supercell.clashofclans" to "Clash of Clans",
            "com.supercell.clashroyale" to "Clash Royale",
            "com.supercell.brawlstars" to "Brawl Stars",
            "com.kiloo.subwaysurf" to "Subway Surfers",
            "com.king.candycrushsaga" to "Candy Crush",
            "com.activision.callofduty.shooter" to "Call of Duty Mobile",
            "com.pubg.imobile" to "BGMI",
            "com.dts.freefireth" to "Free Fire",
            "com.mojang.minecraftpe" to "Minecraft",
            "com.innersloth.spacemafia" to "Among Us",
        )
    }
}
