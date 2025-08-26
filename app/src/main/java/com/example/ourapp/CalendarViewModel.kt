@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.ourapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.Date
import java.util.Calendar

class CalendarViewModel(private val dao: CalendarDao): ViewModel() {

    private val _sortType = MutableStateFlow(SortType.EVENT_NAME)

    private val _selectedDate = MutableStateFlow(Date())
    private val _filterType = MutableStateFlow<String?>(null)

    // Fixed the events flow to use CalendarSchedule instead of Calendar
    private val _events = combine(_sortType, _selectedDate, _filterType) { sortType, selectedDate, filterType ->
        // Calculate date range (e.g., current week)
        val calendar = Calendar.getInstance().apply { time = selectedDate }
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val startDate = calendar.time

        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endDate = calendar.time

        // Get events based on filters
        when {
            filterType != null -> dao.getEventByGroup(filterType)
            else -> dao.getEventsByDateRange(startDate, endDate)
        }
    }.flatMapLatest { flow ->
        if (flow is Flow<*>) {
            flow as Flow<List<CalendarSchedule>>
        } else {
            // Convert List to Flow for consistency
            flowOf(flow as List<CalendarSchedule>)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _calendar = _sortType.flatMapLatest { sortType ->
        when(sortType) {
            SortType.EVENT_NAME -> dao.getEventOrderedByName()
            SortType.EVENT_TYPE -> dao.getEventByGroup("Academic")
        } as Flow<List<CalendarSchedule>>
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val _state = MutableStateFlow(CalendarState())
    val state = combine(_state, _sortType, _calendar) { state, sortType, calendar ->
        state.copy(
            events = calendar, // Assuming CalendarState has an 'events' property
            sortType = sortType
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarState())

    fun onEvent(events: CalendarEvent){
        when(events){
            is CalendarEvent.deleteEvent -> {       //(Vivo, 2019)
                viewModelScope.launch{
                    dao.deleteEvent(events.event)
                }
            }
            CalendarEvent.hideDialog -> {
                _state.update{ it.copy(
                    isAddingEvent = false
                )}
            }
            CalendarEvent.saveEvent -> {
                val eventName = state.value.eventName
                val eventType = state.value.eventType
                val eventDescription = state.value.eventDescription
                val eventDate = state.value.eventDate

                if(eventName.isBlank() || eventType.isBlank() || eventDescription.isBlank())
                {
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
                    _state.update { it.copy(
                        isAddingEvent = false,
                        eventName = "",
                        eventDescription = "",
                        eventType = "",
                        eventDate = Date()
                    )}
                }
            }
            is CalendarEvent.setEventDescription -> {
                _state.update { it.copy(
                    eventDescription = events.eventDescription
                ) }
            }
            is CalendarEvent.setEventName -> {
                _state.update {
                    it.copy(
                        eventName = events.eventName
                    )
                }
            }
            is CalendarEvent.setEventType -> {
                _state.update {
                    it.copy(
                        eventType = events.eventType
                    )
                }
            }
            CalendarEvent.showDialog -> {
                _state.update{ it.copy(
                    isAddingEvent = true
                )}
            }
            is CalendarEvent.sortEvent -> {
                _sortType.value = events.sortType
            }
            is CalendarEvent.selectDate -> {
                _selectedDate.value = events.date
            }
            is CalendarEvent.filterByType -> {
                _filterType.value = events.eventType
            }

            is CalendarEvent.setEventDate -> {
                _state.update {
                    it.copy(
                        eventDate = events.eventDate
                    )
                }
            }

        }
    }
}