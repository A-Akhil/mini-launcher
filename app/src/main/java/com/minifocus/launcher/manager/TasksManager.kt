package com.minifocus.launcher.manager

import com.minifocus.launcher.data.dao.TaskDao
import com.minifocus.launcher.data.entity.TaskEntity
import com.minifocus.launcher.model.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TasksManager(private val taskDao: TaskDao) {

    fun observeTasks(): Flow<List<TaskItem>> = taskDao.observeTasks().map { entities ->
        entities.map { entity ->
            TaskItem(
                id = entity.id,
                title = entity.title,
                isCompleted = entity.isCompleted,
                createdAt = entity.createdAt
            )
        }
    }

    suspend fun addTask(title: String) {
        if (title.isBlank()) return
        withContext(Dispatchers.IO) {
            taskDao.insert(TaskEntity(title = title.trim()))
        }
    }

    suspend fun toggleTask(taskId: Long) {
        withContext(Dispatchers.IO) {
            val existing = taskDao.getTask(taskId) ?: return@withContext
            taskDao.update(existing.copy(isCompleted = !existing.isCompleted))
        }
    }

    suspend fun update(task: TaskItem) {
        withContext(Dispatchers.IO) {
            taskDao.update(
                TaskEntity(
                    id = task.id,
                    title = task.title,
                    isCompleted = task.isCompleted,
                    createdAt = task.createdAt
                )
            )
        }
    }

    suspend fun delete(taskId: Long) {
        withContext(Dispatchers.IO) {
            taskDao.delete(taskId)
        }
    }
}
