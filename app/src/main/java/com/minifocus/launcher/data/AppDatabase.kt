package com.minifocus.launcher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
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
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

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
        fun build(context: Context): AppDatabase {
            // Security: Generate encryption passphrase from Android keystore
            val passphrase = getOrCreateDatabasePassphrase(context)
            val factory = SupportOpenHelperFactory(passphrase)
            
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "minimalist_focus_launcher.db"
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
        }
        
        /**
         * Security: Generate or retrieve database encryption key.
         * Uses a combination of Android ID and app-specific data for device-specific encryption.
         * 
         * Note: For production apps handling highly sensitive data, consider using
         * Android Keystore System for hardware-backed key storage.
         */
        private fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
            val prefs = context.getSharedPreferences("secure_db_prefs", Context.MODE_PRIVATE)
            val keyAlias = "db_encryption_key"
            
            // Check if we have a stored key already
            val storedKeyHex = prefs.getString(keyAlias, null)
            if (storedKeyHex != null) {
                // Decode hex string back to bytes
                return hexStringToByteArray(storedKeyHex)
            }
            
            // Generate new key on first use
            // Get Android ID (can be null or change on factory reset)
            var androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            
            // Fallback if Android ID is null or empty - use persistent device-specific ID
            if (androidId.isNullOrEmpty()) {
                // Check if we already have a fallback ID stored
                androidId = prefs.getString("fallback_device_id", null)
                if (androidId.isNullOrEmpty()) {
                    // Generate unique random ID and store it for future use
                    val randomId = java.util.UUID.randomUUID().toString()
                    prefs.edit().putString("fallback_device_id", randomId).apply()
                    androidId = randomId
                }
            }
            
            // Create a deterministic but device-specific key
            // Note: Key must be deterministic for consistent database access
            val keyComponents = "$keyAlias:$androidId:${context.packageName}"
            
            // Use SHA-256 to create a fixed-length key
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val passphrase = digest.digest(keyComponents.toByteArray(Charsets.UTF_8))
            
            // Store the key for future use (as hex string)
            val passphraseHex = passphrase.joinToString("") { "%02x".format(it) }
            prefs.edit().putString(keyAlias, passphraseHex).apply()
            
            return passphrase
        }
        
        /**
         * Helper function to convert hex string to byte array
         */
        private fun hexStringToByteArray(hexString: String): ByteArray {
            // Validate input
            if (hexString.length % 2 != 0) {
                throw IllegalArgumentException("Hex string must have even length")
            }
            
            val len = hexString.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                val digit1 = Character.digit(hexString[i], 16)
                val digit2 = Character.digit(hexString[i + 1], 16)
                
                if (digit1 == -1 || digit2 == -1) {
                    throw IllegalArgumentException("Invalid hex character at position $i")
                }
                
                data[i / 2] = ((digit1 shl 4) + digit2).toByte()
                i += 2
            }
            return data
        }

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
