package vcmsa.projects.wilproject.firebase

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.dao.CalendarDao
import vcmsa.projects.wilproject.models.CalendarSchedule
import vcmsa.projects.wilproject.firebase.FirebaseDB
import java.util.Date

class CalendarRepos(private val calendarDao: CalendarDao, private val firebaseConnect: FirebaseDB) {
    suspend fun saveEvent(event: CalendarSchedule) {
        calendarDao.insertEvent(event)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                firebaseConnect.saveEventToFirebase(event)
            } catch (e: Exception) {
                Log.e("CalendarRepository", "Failed to sync event to Firebase: ${e.message}")
            }
        }
    }

    suspend fun deleteEvent(event: CalendarSchedule) {
        calendarDao.deleteEvent(event)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                firebaseConnect.deleteEventFromFirebase(event)
            } catch (e: Exception) {
                Log.e("CalendarRepository", "Failed to delete event from Firebase: ${e.message}")
            }
        }
    }

    fun getEventByGroup(userId: String, eventGroup: String): Flow<List<CalendarSchedule>> {
        val localFlow = calendarDao.getEventByGroup(userId, eventGroup)

        localFlow.onEach { localEvents ->
            if (localEvents.isEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val remoteEvents = firebaseConnect.fetchEventsForUser(userId)
                        remoteEvents.forEach { calendarDao.insertEvent(it) }
                    } catch (e: Exception) {
                        Log.e("CalendarRepository", "Failed to sync initial events from Firebase: ${e.message}")
                    }
                }
            }
        }.launchIn(CoroutineScope(Dispatchers.IO))

        return localFlow
    }

    fun getEventsByDateRange(userId: String, startDate: Date, endDate: Date): Flow<List<CalendarSchedule>> {
        val localFlow = calendarDao.getEventsByDateRange(userId, startDate, endDate)

        localFlow.onEach { localEvents ->
            if (localEvents.isEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val remoteEvents = firebaseConnect.fetchEventsForUser(userId)
                        remoteEvents.forEach { calendarDao.insertEvent(it) }
                    } catch (e: Exception) {
                        Log.e("CalendarRepository", "Failed to sync initial events from Firebase: ${e.message}")
                    }
                }
            }
        }.launchIn(CoroutineScope(Dispatchers.IO))

        return localFlow
    }
}