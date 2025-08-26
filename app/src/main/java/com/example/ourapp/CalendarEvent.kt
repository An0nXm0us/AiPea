package com.example.ourapp

import java.util.Date

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
    data class selectDate(val date: Date): CalendarEvent  // Added for date selection
    data class filterByType(val eventType: String?): CalendarEvent  // Added for filtering
}