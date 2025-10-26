package com.minifocus.launcher.manager

import android.content.Context
import com.minifocus.launcher.data.dao.TaskDao
import com.minifocus.launcher.data.entity.TaskEntity
import com.minifocus.launcher.model.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TasksManager(
    private val taskDao: TaskDao,
    private val context: Context
) {
    private val notificationScheduler = NotificationScheduler(context)

    fun observeTasks(): Flow<List<TaskItem>> = taskDao.observeTasks().map { entities ->
        entities.map { entity ->
            TaskItem(
                id = entity.id,
                title = entity.title,
                isCompleted = entity.isCompleted,
                createdAt = entity.createdAt,
                completedAt = entity.completedAt,
                scheduledFor = entity.scheduledFor,
                notificationId = entity.notificationId
            )
        }
    }

    suspend fun addTask(title: String, scheduledFor: Long? = null): Boolean {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return false
        return withContext(Dispatchers.IO) {
            val taskId = taskDao.insert(
                TaskEntity(
                    title = trimmed,
                    scheduledFor = scheduledFor
                )
            )
            
            // Schedule notification if there's a scheduled time
            if (taskId > 0 && scheduledFor != null) {
                notificationScheduler.scheduleNotification(taskId, trimmed, scheduledFor)
            }
            
            taskId > 0
        }
    }

    suspend fun toggleTask(taskId: Long) {
        withContext(Dispatchers.IO) {
            val existing = taskDao.getTask(taskId) ?: return@withContext
            val newCompletedState = !existing.isCompleted
            
            // Cancel notification if task is being completed
            if (newCompletedState && existing.scheduledFor != null) {
                notificationScheduler.cancelNotification(taskId)
            }
            
            taskDao.update(
                existing.copy(
                    isCompleted = newCompletedState,
                    completedAt = if (newCompletedState) System.currentTimeMillis() else null
                )
            )
        }
    }

    suspend fun update(task: TaskItem) {
        withContext(Dispatchers.IO) {
            taskDao.update(
                TaskEntity(
                    id = task.id,
                    title = task.title,
                    isCompleted = task.isCompleted,
                    createdAt = task.createdAt,
                    completedAt = task.completedAt,
                    scheduledFor = task.scheduledFor,
                    notificationId = task.notificationId
                )
            )
        }
    }

    suspend fun delete(taskId: Long) {
        withContext(Dispatchers.IO) {
            // Cancel notification before deleting
            notificationScheduler.cancelNotification(taskId)
            taskDao.delete(taskId)
        }
    }
}
