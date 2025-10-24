package com.minifocus.launcher.manager

import com.minifocus.launcher.data.dao.HiddenAppDao
import com.minifocus.launcher.data.entity.HiddenAppEntity
import kotlinx.coroutines.flow.Flow

class HiddenAppsManager(private val hiddenAppDao: HiddenAppDao) {

    fun observeHiddenApps(): Flow<List<HiddenAppEntity>> = hiddenAppDao.observeHiddenApps()

    suspend fun hideApp(packageName: String) {
        hiddenAppDao.hideApp(HiddenAppEntity(packageName))
    }

    suspend fun unhideApp(packageName: String) {
        hiddenAppDao.unhideApp(packageName)
    }
}
