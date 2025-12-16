package com.minifocus.launcher.model

enum class AppUsageEventSource(val weight: Double) {
    HOME(1.0),
    ALL_APPS_LIST(1.0),
    ALL_APPS_SEARCH(1.3),
    SMART_SUGGESTION(1.1),
    SEARCH_OVERLAY(1.2)
}
