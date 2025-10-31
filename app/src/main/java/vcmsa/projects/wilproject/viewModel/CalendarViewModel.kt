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

/**
 * This view model will be using both its repective dao and repo methods to save locally and on firebase
 * (Tadas Petra.2024 & Philipp Lackner,2023)
 * The state wil be used and reflected on ui (Philipp Lackner,2022)
 **/
class CalendarViewModel(val repository: CalendarRepos, private val sessionManager: SessionManager): ViewModel() {

    init {
        // Fetch and listen for all events for the current user from Firebase.
        viewModelScope.launch {
            repository.syncEvents(sessionManager.getUserId().toString())
        }
    }

    // State  for filtering and sorting events.
    private val _sortType = MutableStateFlow(SortType.EVENT_NAME)
    private val _selectedDate = MutableStateFlow(Date())
    private val _filterType = MutableStateFlow<String?>(null)

    //Dynamically fetches the events based on how they  are sorted by the type of event
    private val _events = combine(
        _sortType,
        _selectedDate,
        _filterType
    ) { sortType, selectedDate, filterType ->

        val userId = sessionManager.getUserId().toString()

        // Calculate the start and end dates of the week based on the selected date.
        val calendar = Calendar.getInstance().apply { time = selectedDate }
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val startDate = calendar.time

        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endDate = calendar.time

        // Determine which repository call to make based on the filter.
        when {
            // If a filter type is set, fetch all events of that type.
            filterType != null -> repository.getEventByGroup(userId, filterType)
            // Otherwise, fetch events within weekly range.
            else -> repository.getEventsByDateRange(userId, startDate, endDate)
        }
    }.flatMapLatest { flow ->
        if (flow is Flow<*>) {
            flow as Flow<List<CalendarSchedule>>
        } else {
            flowOf(flow as List<CalendarSchedule>)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Companion.WhileSubscribed(5000),
        emptyList()
    )

    // State for the overall UI (dialog status, input fields, error messages).
    private val _state = MutableStateFlow(CalendarState())
    //UI will use the UI state and the collected event list to find information
     val state = combine(_state, _events, _sortType) { state, events, sortType ->
        state.copy(
            events = events, // Updated list of filtered/date-range events
            sortType = sortType // Current sort type
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Companion.WhileSubscribed(5000),
        CalendarState()
    )

    /**
     * Processes incoming user events/actions from the UI.
     */
    fun onEvent(events: CalendarEvent){
        when(events){
            // Deletes an event from the locally and on Firebase
            is CalendarEvent.deleteEvent -> {
                viewModelScope.launch {
                    repository.deleteEvent(events.event)
                }
            }
            // Hides the event creation/edit dialog and clears the error message.
            CalendarEvent.hideDialog -> {
                _state.update { it.copy(
                    isAddingEvent = false
                )}
            }
            // Validates and saves a new event to the repository (and Firebase).
            is CalendarEvent.saveEvent ->{
                val eventName = _state.value.eventName
                val eventType = _state.value.eventType
                val eventDescription = _state.value.eventDescription
                val eventDate = _state.value.eventDate
                val userID = sessionManager.getUserId().toString()

                // Validation checks
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

                // Save the event and reset the local state upon successful save.
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
            // Updates the internal state for the current user ID.
            is CalendarEvent.setUserID -> {
                _state.update { it.copy() }
            }
            // Updates the description field in the state.
            is CalendarEvent.setEventDescription -> {
                _state.update { it.copy(
                    eventDescription = events.eventDescription,
                    errorMessage = null
                ) }
            }
            // Updates the event name field in the state.
            is CalendarEvent.setEventName -> {
                _state.update {
                    it.copy(
                        eventName = events.eventName,
                        errorMessage = null
                    )
                }
            }
            // Updates the event type field in the state.
            is CalendarEvent.setEventType -> {
                _state.update {
                    it.copy(
                        eventType = events.eventType,
                        errorMessage = null
                    )
                }
            }
            // Shows the event creation/edit dialog.
            CalendarEvent.showDialog -> {
                _state.update{ it.copy(
                    isAddingEvent = true
                )}
            }
            // Updates the sort type
            is CalendarEvent.sortEvent -> {
                _sortType.value = events.sortType
            }
            // Updates the selected date
            is CalendarEvent.selectDate -> {
                _selectedDate.value = events.date
            }
            // Updates the filter
            is CalendarEvent.filterByType -> {
                _filterType.value = events.eventType
            }

            // Updates the event date
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
                // Ensure the ViewModel class matches before casting and returning.
                return CalendarViewModel(repository,sessionManager) as T
            }
        }
    }
}
