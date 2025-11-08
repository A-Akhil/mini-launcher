package com.minifocus.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minifocus.launcher.manager.AppsManager
import com.minifocus.launcher.manager.NotificationInboxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationFilterViewModel(
    private val notificationInboxManager: NotificationInboxManager,
    private val appsManager: AppsManager
) : ViewModel() {

    data class NotificationFilterItem(
        val packageName: String,
        val appName: String,
        val isEnabled: Boolean
    )

    data class FilterUiState(
        val items: List<NotificationFilterItem> = emptyList(),
        val query: String = ""
    )

    private val searchQuery = MutableStateFlow("")

    private val itemsFlow = combine(
        notificationInboxManager.observeFilters(),
        appsManager.observeAllApps()
    ) { filters, apps ->
        val filterMap = filters.associateBy { it.packageName }

        val appItems = apps
            .filterNot { it.isSystemApp }
            .map { app ->
                val filter = filterMap[app.packageName]
                NotificationFilterItem(
                    packageName = app.packageName,
                    appName = app.label,
                    isEnabled = filter?.isEnabled ?: false
                )
            }

        val appPackages = appItems.map { it.packageName }.toSet()

        val filterOnlyItems = filters
            .filter { it.packageName !in appPackages }
            .mapNotNull { filterEntity ->
                val displayName = filterEntity.appName.ifBlank { filterEntity.packageName }
                if (displayName == filterEntity.packageName) {
                    null
                } else {
                    NotificationFilterItem(
                        packageName = filterEntity.packageName,
                        appName = displayName,
                        isEnabled = filterEntity.isEnabled
                    )
                }
            }

        (appItems + filterOnlyItems).sortedBy { it.appName.lowercase() }
    }

    val uiState = combine(itemsFlow, searchQuery) { items, query ->
        val trimmed = query.trim()
        val filtered = if (trimmed.isBlank()) {
            items
        } else {
            items.filter { item ->
                item.appName.contains(trimmed, ignoreCase = true) ||
                    item.packageName.contains(trimmed, ignoreCase = true)
            }
        }
        FilterUiState(items = filtered, query = trimmed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = FilterUiState()
    )

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggle(item: NotificationFilterItem) {
        viewModelScope.launch {
            val targetState = !item.isEnabled
            notificationInboxManager.ensureFilterExists(item.packageName, item.appName)
            notificationInboxManager.setFilterEnabled(item.packageName, targetState)
        }
    }

    fun setAll(enabled: Boolean) {
        viewModelScope.launch {
            val packages = itemsFlow.first().map { it.packageName }
            notificationInboxManager.setFiltersEnabled(packages, enabled)
        }
    }
}
