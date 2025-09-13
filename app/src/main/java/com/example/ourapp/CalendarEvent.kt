package com.example.ourapp

import java.util.Date

/**
 * UI events that CalendarViewModel understands (user actions).
 */
sealed interface CalendarEvent {
    object saveEvent: CalendarEvent
    data class setEventName(val eventName: String): CalendarEvent
    data class setEventType(val eventType: String): CalendarEvent
    data class setEventDescription(val eventDescription: String): CalendarEvent
    data class setEventDate(val eventDate: Date): CalendarEvent
    object showDialog: CalendarEvent
    object hideDialog: CalendarEvent
    data class sortEvent(val sortType: SortType): CalendarEvent
    data class deleteEvent(val event: CalendarSchedule): CalendarEvent
    data class selectDate(val date: Date): CalendarEvent
    data class filterByType(val eventType: String?): CalendarEvent
    data class togglePeriod(val enabled: Boolean): CalendarEvent // toggles weekly period mode
}
