package com.minifocus.launcher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.minifocus.launcher.data.dao.AppLockDao
import com.minifocus.launcher.data.dao.HiddenAppDao
import com.minifocus.launcher.data.dao.PinnedAppDao
import com.minifocus.launcher.data.dao.TaskDao
import com.minifocus.launcher.data.dao.NotificationDao
import com.minifocus.launcher.data.dao.NotificationFilterDao
import com.minifocus.launcher.data.entity.AppLockEntity
import com.minifocus.launcher.data.entity.HiddenAppEntity
import com.minifocus.launcher.data.entity.PinnedAppEntity
import com.minifocus.launcher.data.entity.TaskEntity
import com.minifocus.launcher.data.entity.NotificationEntity
import com.minifocus.launcher.data.entity.NotificationFilterEntity

@Database(
    entities = [
        TaskEntity::class,
        PinnedAppEntity::class,
        HiddenAppEntity::class,
        AppLockEntity::class,
        NotificationEntity::class,
        NotificationFilterEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun pinnedAppDao(): PinnedAppDao
    abstract fun hiddenAppDao(): HiddenAppDao
    abstract fun appLockDao(): AppLockDao
    abstract fun notificationDao(): NotificationDao
    abstract fun notificationFilterDao(): NotificationFilterDao

    companion object {
        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "minimalist_focus_launcher.db"
        )
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notifications` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `key` TEXT NOT NULL,
                        `package_name` TEXT NOT NULL,
                        `app_name` TEXT NOT NULL,
                        `title` TEXT,
                        `text` TEXT,
                        `timestamp` INTEGER NOT NULL,
                        `is_read` INTEGER NOT NULL DEFAULT 0,
                        `small_icon` BLOB,
                        `expires_at` INTEGER
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_notifications_key` ON `notifications` (`key`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_notifications_is_read` ON `notifications` (`is_read`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_notifications_timestamp` ON `notifications` (`timestamp`)"
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notification_filters` (
                        `package_name` TEXT NOT NULL,
                        `app_name` TEXT NOT NULL,
                        `is_enabled` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`package_name`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
