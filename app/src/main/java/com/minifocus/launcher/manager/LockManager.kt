/*
 * Minimalist Focus Launcher
 * Copyright (C) 2025 A-Akhil
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.minifocus.launcher.manager

import com.minifocus.launcher.data.dao.AppLockDao
import com.minifocus.launcher.data.entity.AppLockEntity
import kotlinx.coroutines.flow.Flow

class LockManager(private val appLockDao: AppLockDao) {

    fun observeLocks(): Flow<List<AppLockEntity>> = appLockDao.observeLocks()

    suspend fun lockApp(packageName: String, lockedUntil: Long) {
        val existing = appLockDao.getLock(packageName)
        if (existing == null || lockedUntil > existing.lockedUntil) {
            appLockDao.upsertLock(AppLockEntity(packageName, lockedUntil))
        }
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
