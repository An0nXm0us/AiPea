package vcmsa.projects.wilproject.state

import vcmsa.projects.wilproject.SortType
import vcmsa.projects.wilproject.models.CalendarSchedule
import java.util.Date

data class CalendarState(
    val events: List<CalendarSchedule> = emptyList(),
    val eventName: String = "",
    val eventDescription: String = "",
    val eventDate: Date = Date(),
    val eventType: String = "",
    val userId : String = "",
    val isAddingEvent: Boolean = false,
    val sortType: SortType = SortType.EVENT_NAME,
    val selectedDate: Date = Date(),
    val filterType: String? = null,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)