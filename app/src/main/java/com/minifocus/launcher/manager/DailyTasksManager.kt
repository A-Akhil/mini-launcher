package com.minifocus.launcher.manager

import com.minifocus.launcher.data.dao.DailyTaskDao
import com.minifocus.launcher.data.entity.DailyTaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class DailyTasksManager(
    private val dailyTaskDao: DailyTaskDao,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    fun observeDailyTasks(): Flow<List<DailyTaskEntity>> = dailyTaskDao.observeDailyTasks()

    suspend fun addDailyTask(
        title: String,
        startEpochDay: Long?,
        endEpochDay: Long?,
        enabled: Boolean
    ): Long {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return -1
        return withContext(Dispatchers.IO) {
            dailyTaskDao.insert(
                DailyTaskEntity(
                    title = trimmed,
                    startEpochDay = startEpochDay,
                    endEpochDay = endEpochDay,
                    isEnabled = enabled
                )
            )
        }
    }

    suspend fun updateDailyTask(entity: DailyTaskEntity): Unit = withContext(Dispatchers.IO) {
        dailyTaskDao.update(entity)
    }

    suspend fun deleteDailyTask(taskId: Long) {
        withContext(Dispatchers.IO) {
            dailyTaskDao.delete(taskId)
        }
    }

    suspend fun getDailyTask(taskId: Long): DailyTaskEntity? = withContext(Dispatchers.IO) {
        dailyTaskDao.get(taskId)
    }

    suspend fun markCompletedForToday(taskId: Long) {
        withContext(Dispatchers.IO) {
            val entity = dailyTaskDao.get(taskId) ?: return@withContext
            val today = LocalDate.now(zoneId).toEpochDay()
            dailyTaskDao.update(entity.copy(lastCompletedEpochDay = today))
        }
    }

    suspend fun resetCompletion(taskId: Long) {
        withContext(Dispatchers.IO) {
            val entity = dailyTaskDao.get(taskId) ?: return@withContext
            dailyTaskDao.update(entity.copy(lastCompletedEpochDay = null))
        }
    }
}
