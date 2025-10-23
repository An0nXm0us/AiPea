package vcmsa.projects.wilproject.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.SessionManager
import vcmsa.projects.wilproject.SortType
import vcmsa.projects.wilproject.firebase.CalendarRepos

import vcmsa.projects.wilproject.event.CalendarEvent
import vcmsa.projects.wilproject.models.CalendarSchedule
import vcmsa.projects.wilproject.state.CalendarState
import java.util.Calendar
import java.util.Date
class CalendarViewModel(private val repository: CalendarRepos, private val sessionManager: SessionManager): ViewModel() {


    private val _sortType = MutableStateFlow(SortType.EVENT_NAME)

    private val _selectedDate = MutableStateFlow(Date())
    private val _filterType = MutableStateFlow<String?>(null)

    private val _events = combine(
        _sortType,
        _selectedDate,
        _filterType
    ) { sortType, selectedDate, filterType ->

        val calendar = Calendar.getInstance().apply { time = selectedDate }
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val startDate = calendar.time

        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endDate = calendar.time


        when {
            filterType != null -> repository.getEventByGroup(
                sessionManager.getUserId().toString(),
                filterType
            )

            else -> repository.getEventsByDateRange(
                sessionManager.getUserId().toString(),
                startDate,
                endDate
            )
        }
    }.flatMapLatest { flow ->
        if (flow is Flow<*>) {
            flow as Flow<List<CalendarSchedule>>
        } else {
            flowOf(flow as List<CalendarSchedule>)
        }
    }.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow(CalendarState())
    val state = combine(_state, _events, _sortType) { state, events, sortType ->
        state.copy(
            events = events,
            sortType = sortType
        )
    }.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), CalendarState())

    fun onEvent(events: CalendarEvent){
        when(events){
            is CalendarEvent.deleteEvent -> {
                viewModelScope.launch {
                    repository.deleteEvent(events.event)
                }
            }
            CalendarEvent.hideDialog -> {
                _state.update { it.copy(
                    isAddingEvent = false
                )}
            }
            is CalendarEvent.saveEvent ->{
                val eventName = _state.value.eventName
                val eventType = _state.value.eventType
                val eventDescription = _state.value.eventDescription
                val eventDate = _state.value.eventDate
                val userID = sessionManager.getUserId().toString()

                if(userID.isBlank()){
                    _state.update { it.copy(errorMessage = "Error: Current user is not authorised to enter notes") }
                    return
                }
                if (eventName.isBlank() || eventType.isBlank() || eventDescription.isBlank()) {
                    _state.update { it.copy(errorMessage = "Error regarding entering notes") }
                    return
                }

                val calendarEvent = CalendarSchedule(
                    eventName = eventName,
                    eventType = eventType,
                    eventDescription = eventDescription,
                    eventDate = eventDate,
                    event_userId = userID
                )
                viewModelScope.launch {
                    repository.saveEvent(calendarEvent)
                    _state.update { it.copy(
                        isAddingEvent = false,
                        eventName = "",
                        eventDescription = "",
                        eventType = "",
                        eventDate = Date(),
                        userId = "",
                        isSuccess = true,
                        errorMessage = null
                    )}
                }
            }
            is CalendarEvent.setUserID -> {
                _state.update { it.copy(

                ) }
            }
            is CalendarEvent.setEventDescription -> {
                _state.update { it.copy(
                    eventDescription = events.eventDescription,
                    errorMessage = null

                ) }
            }
            is CalendarEvent.setEventName -> {
                _state.update {
                    it.copy(
                        eventName = events.eventName,
                        errorMessage = null
                    )
                }
            }
            is CalendarEvent.setEventType -> {
                _state.update {
                    it.copy(
                        eventType = events.eventType,
                        errorMessage = null
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
    companion object {

        fun provideFactory(repository: CalendarRepos, sessionManager: SessionManager): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CalendarViewModel(repository,sessionManager) as T
            }
        }
    }
}