package com.example.ourapp

import java.util.Calendar
import java.util.Date

data class CalendarState(
    val events: List<CalendarSchedule> = emptyList(),
    val eventName: String = "",
    val eventDescription: String = "",
    val eventDate: Date = Date(),
    val eventType: String = "",
    val isAddingEvent: Boolean = false,
    val sortType: SortType = SortType.EVENT_NAME,
    val selectedDate: Date = Date(),  // Added for date selection
    val filterType: String? = null  // Added for filtering
)
