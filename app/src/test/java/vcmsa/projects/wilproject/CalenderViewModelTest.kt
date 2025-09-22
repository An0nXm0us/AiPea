package vcmsa.projects.wilproject

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private lateinit var fakeDao: FakeCalendarDao
    private lateinit var fakeSession: FakeSessionManager
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setup() {
        fakeDao = FakeCalendarDao()
        fakeSession = FakeSessionManager("user1")
        viewModel = CalendarViewModel(fakeDao, fakeSession)
    }

    @Test
    fun `saveEvent adds event to dao and state updates`() = runTest {
        // Given
        val date = Date()
        viewModel.onEvent(CalendarEvent.setEventName("Meeting"))
        viewModel.onEvent(CalendarEvent.setEventDescription("Discuss project"))
        viewModel.onEvent(CalendarEvent.setEventType("Work"))
        viewModel.onEvent(CalendarEvent.setEventDate(date))

        // When
        viewModel.onEvent(CalendarEvent.saveEvent)

        // Then
        val state = viewModel.state.first()
        assertEquals(1, state.events.size)
        val event = state.events.first()
        assertEquals("Meeting", event.eventName)
        assertEquals("Work", event.eventType)
        assertEquals("Discuss project", event.eventDescription)
    }

    @Test
    fun `deleteEvent removes event`() = runTest {
        // Insert one event manually
        val event = CalendarSchedule(
            eventName = "Meeting",
            eventType = "Work",
            eventDescription = "Discuss project",
            eventDate = Date(),
            userId = "user1"
        )
        fakeDao.insertEvent(event)

        // Delete via viewmodel
        viewModel.onEvent(CalendarEvent.deleteEvent(event))

        val state = viewModel.state.first()
        assertEquals(0, state.events.size)
    }
}
