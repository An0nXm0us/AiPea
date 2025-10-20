package vcmsa.projects.wilproject.event

import vcmsa.projects.wilproject.SortType
import vcmsa.projects.wilproject.models.CalendarSchedule
import java.util.Date

sealed interface CalendarEvent {
    object saveEvent: CalendarEvent
    data class setEventName(val eventName: String): CalendarEvent
    data class setEventType(val eventType: String): CalendarEvent
    data class setEventDescription(val eventDescription: String): CalendarEvent
    data class setEventDate(val eventDate: Date): CalendarEvent

    data class setUserID(val userId: String): CalendarEvent
    object showDialog: CalendarEvent
    object hideDialog: CalendarEvent
    data class sortEvent(val sortType: SortType): CalendarEvent
    data class deleteEvent(val event: CalendarSchedule): CalendarEvent
    data class selectDate(val date: Date): CalendarEvent
    data class filterByType(val eventType: String?): CalendarEvent
}