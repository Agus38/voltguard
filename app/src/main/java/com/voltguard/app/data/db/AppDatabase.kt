package com.voltguard.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * AppDatabase - Room persistence
 * Author: Agus Dev
 * 
 * Migration strategy: version changes require explicit migrations to preserve user data.
 * Never use fallbackToDestructiveMigration in production.
 */
@Database(entities = [SampleEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sampleDao(): SampleDao

    companion object {
        private const val NAME = "voltguard.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NAME,
                )
                // Future migrations go here when schema changes
                // .addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
            }
    }
}
