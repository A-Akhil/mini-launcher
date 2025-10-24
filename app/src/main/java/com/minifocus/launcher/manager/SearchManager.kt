package com.minifocus.launcher.manager

import com.minifocus.launcher.model.AppEntry
import com.minifocus.launcher.model.SearchResult
import com.minifocus.launcher.model.TaskItem
import kotlinx.coroutines.flow.first

class SearchManager(
    private val appsManager: AppsManager,
    private val tasksManager: TasksManager
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
        val results = mutableListOf<SearchResult>()

        results += apps.filter { it.label.contains(query, ignoreCase = true) }
            .map { SearchResult.App(it) }
        results += hidden.filter { it.label.contains(query, ignoreCase = true) }
            .map { SearchResult.App(it) }
        results += tasks.filter { it.title.contains(query, ignoreCase = true) }
            .map { SearchResult.Task(it) }

        return results
    }
}
