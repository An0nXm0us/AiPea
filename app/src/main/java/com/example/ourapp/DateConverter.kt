package com.example.ourapp

import androidx.room.TypeConverter
import java.util.Date

/**
 * Converts Date to Long and back so Room can persist Date fields.
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
