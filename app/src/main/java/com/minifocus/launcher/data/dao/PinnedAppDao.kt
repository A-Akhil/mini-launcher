package com.minifocus.launcher.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.minifocus.launcher.data.entity.PinnedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedAppDao {
    @Query("SELECT * FROM pinned_apps ORDER BY position ASC")
    fun observePinned(): Flow<List<PinnedAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPinnedApp(entity: PinnedAppEntity)

    @Query("DELETE FROM pinned_apps WHERE package_name = :packageName")
    suspend fun unpinApp(packageName: String)

    @Query("SELECT COUNT(*) FROM pinned_apps")
    suspend fun getPinnedCount(): Int
}
