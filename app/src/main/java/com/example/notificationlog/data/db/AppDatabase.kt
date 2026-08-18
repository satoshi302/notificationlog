package com.example.notificationlog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MessageEntity::class, SelfCheckEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun selfCheckDao(): SelfCheckDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v1→v2: self_checks テーブルを追加（messages は保持）。 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `self_checks` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `conversationKey` TEXT NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `draft` TEXT NOT NULL,
                        `riskScore` INTEGER NOT NULL,
                        `flagsCsv` TEXT NOT NULL,
                        `otherIntensity` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_self_checks_timestamp` ON `self_checks` (`timestamp`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_self_checks_conversationKey` ON `self_checks` (`conversationKey`)"
                )
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notificationlog.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
