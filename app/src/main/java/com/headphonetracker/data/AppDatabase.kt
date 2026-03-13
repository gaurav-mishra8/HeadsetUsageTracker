package com.headphonetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HeadphoneUsage::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun headphoneUsageDao(): HeadphoneUsageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Migration from v1 to v2: add indexes on date and (date, packageName). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_headphone_usage_date` ON `headphone_usage` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_headphone_usage_date_packageName` ON `headphone_usage` (`date`, `packageName`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "headphone_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
