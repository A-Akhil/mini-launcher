package com.minifocus.launcher.model

sealed interface SearchResult {
    data class App(val entry: AppEntry) : SearchResult
    data class Task(val item: TaskItem) : SearchResult
    data class Command(val command: String) : SearchResult
}
