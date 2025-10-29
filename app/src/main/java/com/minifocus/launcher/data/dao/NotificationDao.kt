package com.minifocus.launcher.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.minifocus.launcher.data.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE is_read = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE is_read = 0")
    suspend fun countUnread(): Int

    @Query("SELECT COUNT(*) FROM notifications")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(notification: NotificationEntity): Long

    @Query("SELECT * FROM notifications")
    suspend fun getAll(): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE `key` = :key LIMIT 1")
    suspend fun findByKey(key: String): NotificationEntity?

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): NotificationEntity?

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE notifications SET is_read = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    @Query("DELETE FROM notifications WHERE expires_at IS NOT NULL AND expires_at <= :threshold")
    suspend fun deleteExpired(threshold: Long)

    @Query("UPDATE notifications SET expires_at = :expiresAt WHERE id = :id")
    suspend fun updateExpiry(id: Long, expiresAt: Long?)

    @Query("DELETE FROM notifications WHERE package_name = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
