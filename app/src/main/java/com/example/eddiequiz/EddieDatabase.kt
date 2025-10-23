package com.example.eddiequiz

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScoreEntity::class], version = 1,  exportSchema = false)
abstract class EddieDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao

    companion object {
        @Volatile private var instance: EddieDatabase? = null

        fun getDatabase(context: Context): EddieDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    EddieDatabase::class.java,
                    "quiz_db"
                ).build().also { instance = it }
            }
        }
    }
}