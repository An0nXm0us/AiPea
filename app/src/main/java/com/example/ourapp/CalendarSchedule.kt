package com.example.ourapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing an event in the calendar.
 * Stored in Room table "events".
 */
@Entity(tableName = "events")
data class CalendarSchedule(
    val eventName: String,
    val eventType: String,
    val eventDescription: String,
    val eventDate: Date,
    @PrimaryKey(autoGenerate = true)
    val eventId: Int = 0,
)
