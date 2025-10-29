package com.minifocus.launcher.manager

import com.minifocus.launcher.data.dao.AppLockDao
import com.minifocus.launcher.data.entity.AppLockEntity
import kotlinx.coroutines.flow.Flow

class LockManager(private val appLockDao: AppLockDao) {

    fun observeLocks(): Flow<List<AppLockEntity>> = appLockDao.observeLocks()

    suspend fun lockApp(packageName: String, lockedUntil: Long) {
        appLockDao.upsertLock(AppLockEntity(packageName, lockedUntil))
    }

    suspend fun unlockApp(packageName: String) {
        appLockDao.clearLock(packageName)
    }

    suspend fun isLocked(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        val lock = appLockDao.getLock(packageName) ?: return false
        if (lock.lockedUntil <= now) {
            appLockDao.clearLock(packageName)
            return false
        }
        return true
    }

    suspend fun clearExpiredLocks() {
        appLockDao.clearExpiredLocks(System.currentTimeMillis())
    }

    suspend fun getLockInfo(packageName: String): AppLockEntity? {
        return appLockDao.getLock(packageName)
    }

    private suspend fun scheduleCleanupIfNeeded() {
        val earliestLock = appLockDao.getEarliestLock()
        if (earliestLock != null && earliestLock.lockedUntil > System.currentTimeMillis()) {
        }
    }
}
