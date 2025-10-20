package vcmsa.projects.wilproject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import vcmsa.projects.wilproject.dao.CalendarDao
import vcmsa.projects.wilproject.models.CalendarSchedule
import java.util.Date

class FakeCalendarDao : CalendarDao {
    private val events = mutableListOf<CalendarSchedule>()
    private val eventsFlow = MutableStateFlow<List<CalendarSchedule>>(emptyList())

    override suspend fun insertEvent(event: CalendarSchedule) {
        events.add(event)
        eventsFlow.value = events.toList()
    }

    override suspend fun deleteEvent(event: CalendarSchedule) {
        events.remove(event)
        eventsFlow.value = events.toList()
    }

    override fun getEventOrderedByName(): Flow<List<CalendarSchedule>> =
        eventsFlow

    override fun getEventByGroup(userId: String, eventGroup: String): Flow<List<CalendarSchedule>> =
        eventsFlow.map { list -> list.filter { it.userId == userId && it.eventType == eventGroup } }

    override fun getEventsByDateRange(
        userId: String,
        startDate: Date,
        endDate: Date
    ): Flow<List<CalendarSchedule>> =
        eventsFlow.map { list ->
            list.filter {
                it.userId == userId &&
                        it.eventDate.after(startDate) &&
                        it.eventDate.before(endDate)
            }
        }
}
