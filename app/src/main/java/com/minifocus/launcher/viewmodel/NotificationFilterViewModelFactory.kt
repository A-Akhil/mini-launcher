package com.minifocus.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.minifocus.launcher.manager.AppsManager
import com.minifocus.launcher.manager.NotificationInboxManager

class NotificationFilterViewModelFactory(
    private val notificationInboxManager: NotificationInboxManager,
    private val appsManager: AppsManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationFilterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationFilterViewModel(
                notificationInboxManager = notificationInboxManager,
                appsManager = appsManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}
