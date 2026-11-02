package com.mindfuck.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DayEntry::class, Medication::class, MedIntake::class, CustomTracker::class, CustomValue::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): MoodDao

    companion object {
        /** v2: per-day dose on each intake. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE med_intake ADD COLUMN dose TEXT")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "mindfuck.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
