package com.example.ourapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * ViewModel that coordinates UI state and Room DAO.
 *
 * - Holds state flows for sort, selected date, filter type, and period toggle.
 * - Produces an events Flow<List<CalendarSchedule>> that the UI collects.
 */
class CalendarViewModel(private val dao: CalendarDao): ViewModel() {

    // Sort type (by name or type)
    private val _sortType = MutableStateFlow(SortType.EVENT_NAME)

    // Selected date (null = no explicit selection)
    private val _selectedDate = MutableStateFlow<Date?>(null)

    // Filter by event type (null = no filter)
    private val _filterType = MutableStateFlow<String?>(null)

    // Whether user wants to view a period (week) around the selected date
    private val _showPeriod = MutableStateFlow(false)

    /**
     * Combined events flow:
     * - If filterType is set -> return group events.
     * - Else if a date is selected and showPeriod is true -> return events in that week.
     * - Else if a date is selected -> return events for that day.
     * - Else -> default: upcoming events (nearest first).
     */
    private val _events: StateFlow<List<CalendarSchedule>> = combine(
        _sortType, _selectedDate, _filterType, _showPeriod
    ) { sortType, selectedDate, filterType, showPeriod ->

        when {
            // Filter by type overrides other choices
            filterType != null -> dao.getEventByGroup(filterType)

            // Weekly period around selected date
            selectedDate != null && showPeriod -> {
                val cal = Calendar.getInstance().apply { time = selectedDate }
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val startDate = cal.time
                cal.add(Calendar.DAY_OF_WEEK, 6)
                val endDate = cal.time
                dao.getEventsByDateRange(startDate, endDate)
            }

            // Single date selection (entire day)
            selectedDate != null -> {
                val start = Calendar.getInstance().apply {
                    time = selectedDate
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.time

                val end = Calendar.getInstance().apply {
                    time = selectedDate
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                }.time

                dao.getEventsByDateRange(start, end)
            }

            // Default: upcoming events
            else -> dao.getUpcomingEvents()
        }
    }
        // flatMapLatest to get the Flow<List<CalendarSchedule>> returned by DAO into Flow<List<CalendarSchedule>>
        .flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // Keep an inner mutable UI state for the add-dialog and form fields
    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = combine(
        _state, _sortType, _events, _selectedDate, _filterType
    ) { state, sortType, events, selectedDate, filterType ->
        state.copy(
            events = events,
            sortType = sortType,
            selectedDate = selectedDate ?: Date(),
            filterType = filterType
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarState())

    /**
     * Handle UI events from the activity (calendar page).
     */
    fun onEvent(event: CalendarEvent) {
        when(event) {
            is CalendarEvent.deleteEvent -> {
                viewModelScope.launch { dao.deleteEvent(event.event) }
            }
            CalendarEvent.hideDialog -> _state.update { it.copy(isAddingEvent = false) }
            CalendarEvent.showDialog -> _state.update { it.copy(isAddingEvent = true) }
            CalendarEvent.saveEvent -> {
                val eventName = state.value.eventName
                val eventType = state.value.eventType
                val eventDescription = state.value.eventDescription
                val eventDate = state.value.eventDate

                // Validate
                if (eventName.isBlank() || eventType.isBlank() || eventDescription.isBlank()) {
                    return
                }

                val calendarEvent = CalendarSchedule(
                    eventName = eventName,
                    eventType = eventType,
                    eventDescription = eventDescription,
                    eventDate = eventDate
                )

                viewModelScope.launch {
                    dao.insertEvent(calendarEvent)
                }

                // Reset form and close dialog
                _state.update { it.copy(
                    isAddingEvent = false,
                    eventName = "",
                    eventDescription = "",
                    eventType = "",
                    eventDate = Date()
                ) }
            }
            is CalendarEvent.setEventDescription -> _state.update { it.copy(eventDescription = event.eventDescription) }
            is CalendarEvent.setEventName -> _state.update { it.copy(eventName = event.eventName) }
            is CalendarEvent.setEventType -> _state.update { it.copy(eventType = event.eventType) }
            is CalendarEvent.setEventDate -> _state.update { it.copy(eventDate = event.eventDate) }
            is CalendarEvent.sortEvent -> _sortType.value = event.sortType
            is CalendarEvent.selectDate -> _selectedDate.value = event.date
            is CalendarEvent.filterByType -> _filterType.value = event.eventType
            is CalendarEvent.togglePeriod -> _showPeriod.value = event.enabled
        }
    }
}
