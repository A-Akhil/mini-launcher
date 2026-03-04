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
import com.minifocus.launcher.data.dao.AppUsageStatsDao
import com.minifocus.launcher.data.dao.AppTimeReminderDao
import com.minifocus.launcher.data.entity.AppLockEntity
import com.minifocus.launcher.data.entity.DailyTaskEntity
import com.minifocus.launcher.data.entity.HiddenAppEntity
import com.minifocus.launcher.data.entity.PinnedAppEntity
import com.minifocus.launcher.data.entity.TaskEntity
import com.minifocus.launcher.data.entity.NotificationEntity
import com.minifocus.launcher.data.entity.NotificationFilterEntity
import com.minifocus.launcher.data.entity.AppUsageStatsEntity
import com.minifocus.launcher.data.entity.AppTimeReminderEntity

@Database(
    entities = [
        TaskEntity::class,
        PinnedAppEntity::class,
        HiddenAppEntity::class,
        AppLockEntity::class,
        NotificationEntity::class,
        NotificationFilterEntity::class,
        DailyTaskEntity::class,
        AppUsageStatsEntity::class,
        AppTimeReminderEntity::class
    ],
    version = 8,
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
    abstract fun appUsageStatsDao(): AppUsageStatsDao
    abstract fun appTimeReminderDao(): AppTimeReminderDao

    companion object {
        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "minimalist_focus_launcher.db"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_usage_stats` (
                        `package_name` TEXT NOT NULL,
                        `total_score` REAL NOT NULL DEFAULT 0,
                        `morning_score` REAL NOT NULL DEFAULT 0,
                        `midday_score` REAL NOT NULL DEFAULT 0,
                        `evening_score` REAL NOT NULL DEFAULT 0,
                        `late_score` REAL NOT NULL DEFAULT 0,
                        `last_launch_epoch` INTEGER NOT NULL DEFAULT 0,
                        `last_decay_day` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`package_name`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_time_reminders` (
                        `package_name` TEXT NOT NULL,
                        `app_label` TEXT NOT NULL,
                        `default_duration_minutes` INTEGER,
                        `expiry_action` TEXT NOT NULL DEFAULT 'REMINDER',
                        PRIMARY KEY(`package_name`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Convert old expiry_action values to new enum names:
                // REMINDER, CLOSE_APP, RETURN_HOME -> NOTIFICATION
                db.execSQL(
                    """
                    UPDATE app_time_reminders
                    SET expiry_action = 'NOTIFICATION'
                    WHERE expiry_action NOT IN ('NOTIFICATION', 'PROMPT')
                    """.trimIndent()
                )
            }
        }
    }
}
