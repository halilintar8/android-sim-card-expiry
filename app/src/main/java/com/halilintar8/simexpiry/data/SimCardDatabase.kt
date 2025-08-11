package com.halilintar8.simexpiry.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SimCard::class], version = 1, exportSchema = false)
abstract class SimCardDatabase : RoomDatabase() {
    abstract fun simCardDao(): SimCardDao

    companion object {
        @Volatile private var INSTANCE: SimCardDatabase? = null

        fun getDatabase(context: Context): SimCardDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    SimCardDatabase::class.java,
                    "sim_card_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

