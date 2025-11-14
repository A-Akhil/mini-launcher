package com.minifocus.launcher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.minifocus.launcher.data.dao.AppLockDao
import com.minifocus.launcher.data.dao.DailyTaskDao
import com.minifocus.launcher.data.dao.HiddenAppDao
import com.minifocus.launcher.data.dao.PinnedAppDao
import com.minifocus.launcher.data.dao.TaskDao
import com.minifocus.launcher.data.dao.NotificationDao
import com.minifocus.launcher.data.dao.NotificationFilterDao
import com.minifocus.launcher.data.entity.AppLockEntity
import com.minifocus.launcher.data.entity.DailyTaskEntity
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
        NotificationFilterEntity::class,
        DailyTaskEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun pinnedAppDao(): PinnedAppDao
    abstract fun hiddenAppDao(): HiddenAppDao
    abstract fun appLockDao(): AppLockDao
    abstract fun notificationDao(): NotificationDao
    abstract fun notificationFilterDao(): NotificationFilterDao
    abstract fun dailyTaskDao(): DailyTaskDao

    companion object {
        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "minimalist_focus_launcher.db"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
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
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_notifications_key` ON `notifications` (`key`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_notifications_is_read` ON `notifications` (`is_read`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_notifications_timestamp` ON `notifications` (`timestamp`)"
                )
                db.execSQL(
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `start_epoch_day` INTEGER,
                        `end_epoch_day` INTEGER,
                        `is_enabled` INTEGER NOT NULL DEFAULT 1,
                        `last_completed_epoch_day` INTEGER,
                        `created_at` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_daily_tasks_created_at` ON `daily_tasks` (`created_at`)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `daily_tasks` ADD COLUMN `repeat_mode` TEXT NOT NULL DEFAULT 'EVERY_DAY'"
                )
                db.execSQL(
                    "ALTER TABLE `daily_tasks` ADD COLUMN `interval_days` INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE `daily_tasks` ADD COLUMN `days_of_week_mask` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
