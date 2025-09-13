package com.example.ourapp

import java.util.Date

/**
 * Holds UI state for the calendar screen.
 */
data class CalendarState(
    val events: List<CalendarSchedule> = emptyList(),
    val eventName: String = "",
    val eventDescription: String = "",
    val eventDate: Date = Date(),
    val eventType: String = "",
    val isAddingEvent: Boolean = false,
    val sortType: SortType = SortType.EVENT_NAME,
    val selectedDate: Date = Date(),
    val filterType: String? = null
)
