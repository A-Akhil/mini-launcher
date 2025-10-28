package com.minifocus.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.minifocus.launcher.manager.NotificationInboxManager
import com.minifocus.launcher.manager.SettingsManager

class NotificationInboxViewModelFactory(
    private val inboxManager: NotificationInboxManager,
    private val settingsManager: SettingsManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationInboxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationInboxViewModel(
                inboxManager = inboxManager,
                settingsManager = settingsManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}
