package com.minifocus.launcher.manager

import com.minifocus.launcher.model.AppEntry
import com.minifocus.launcher.model.SearchResult
import com.minifocus.launcher.model.TaskItem
import kotlinx.coroutines.flow.first

class SearchManager(
    private val appsManager: AppsManager,
    private val tasksManager: TasksManager,
    private val appUsageStatsManager: AppUsageStatsManager
) {

    suspend fun search(query: String): List<SearchResult> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        return when {
            trimmed.startsWith(":hidden") -> buildHiddenAppsResult()
            trimmed.startsWith("@") -> buildHiddenAppLaunch(trimmed.removePrefix("@"))
            else -> combineSearch(trimmed)
        }
    }

    fun isCommand(query: String): Boolean {
        val trimmed = query.trim()
        return trimmed.startsWith(":") || trimmed.startsWith("@")
    }

    private suspend fun buildHiddenAppsResult(): List<SearchResult> {
    val hiddenApps = appsManager.observeHiddenApps().first()
    return hiddenApps.map { entry -> SearchResult.App(entry.copy(isHidden = true)) }
    }

    private suspend fun buildHiddenAppLaunch(appName: String): List<SearchResult> {
        if (appName.isBlank()) return emptyList()
    val hiddenApps = appsManager.observeHiddenApps().first()
        val matches = hiddenApps.filter { it.label.contains(appName, ignoreCase = true) }
        return matches.map { SearchResult.App(it.copy(isHidden = true)) }
    }

    private suspend fun combineSearch(query: String): List<SearchResult> {
        val apps = appsManager.observeAllApps().first()
        val tasks = tasksManager.observeTasks().first()
        val hidden = appsManager.observeHiddenApps().first()
        val stats = appUsageStatsManager.observeStats().value

        val results = mutableListOf<SearchResult>()

        results += apps.filter { it.label.contains(query, ignoreCase = true) }
            .sortedByDescending { app ->
                val usage = stats[app.packageName]?.totalScore ?: 0.0
                val multiplier = calculateMatchMultiplier(app.label, query)
                (usage + 0.1) * multiplier
            }
            .map { SearchResult.App(it) }
            
        results += hidden.filter { it.label.contains(query, ignoreCase = true) }
            .map { SearchResult.App(it) }
        results += tasks.filter { it.title.contains(query, ignoreCase = true) }
            .map { SearchResult.Task(it) }

        return results
    }

    private fun calculateMatchMultiplier(label: String, query: String): Double {
        val isSingleChar = query.length == 1
        
        // Priority 1: Starts with query
        if (label.startsWith(query, ignoreCase = true)) {
            return if (isSingleChar) 1000.0 else 4.0 // Boosted for strong prefix preference
        }

        // Priority 2: Word starts with query (e.g. "Proton VPN" matches "vp")
        // Check for boundary: index > 0 and prev char is not letter/digit
        var index = label.indexOf(query, ignoreCase = true)
        while (index >= 0) {
            if (index > 0 && !Character.isLetterOrDigit(label[index - 1])) {
                return if (isSingleChar) 500.0 else 3.0 // Significant boost for word start
            }
            index = label.indexOf(query, index + 1, ignoreCase = true)
        }

        // Priority 3: Just contains
        return 1.0
    }
}
