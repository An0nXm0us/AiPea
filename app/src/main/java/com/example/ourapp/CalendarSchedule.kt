package com.example.ourapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "events")
data class CalendarSchedule(
    //data used for eventScheduling
    val eventName: String,
    val eventType: String,
    val eventDescription: String,
    val eventDate: Date, // Added date field
    @PrimaryKey(autoGenerate = true)
    val eventId: Int = 0,
)
