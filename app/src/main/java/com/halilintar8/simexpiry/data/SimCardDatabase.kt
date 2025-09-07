package com.halilintar8.simexpiry.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for SIM cards.
 *
 * Provides a singleton instance and access to the DAO.
 * Version is bumped when schema changes (update with migrations if needed).
 */
@Database(
    entities = [SimCard::class],
    version = 1,
    exportSchema = true
)
abstract class SimCardDatabase : RoomDatabase() {

    abstract fun simCardDao(): SimCardDao

    companion object {
        @Volatile
        private var INSTANCE: SimCardDatabase? = null

        /**
         * Get or create the singleton database instance.
         */
        fun getDatabase(context: Context): SimCardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SimCardDatabase::class.java,
                    "sim_card_database"
                )
                    // For production, replace with proper migrations when schema changes
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
