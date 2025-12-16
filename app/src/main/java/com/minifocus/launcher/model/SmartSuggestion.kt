package com.minifocus.launcher.model

data class SmartSuggestion(
    val app: AppEntry,
    val reason: SmartSuggestionReason
)

enum class SmartSuggestionReason {
    FREQUENT,
    TIME_OF_DAY
}
