package com.example.ourapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CalendarSchedule::class],
    version = 1
)
@TypeConverters(DateConverter::class)
abstract class CalendarDatabase : RoomDatabase() {
    abstract val dao: CalendarDao

    companion object {
        @Volatile private var INSTANCE: CalendarDatabase? = null

        fun getDatabase(context: Context): CalendarDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CalendarDatabase::class.java,
                    "calendar_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
