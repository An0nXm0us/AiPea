package vcmsa.projects.wilproject.firebase

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import vcmsa.projects.wilproject.dao.CalendarDao
import vcmsa.projects.wilproject.models.CalendarSchedule
import java.util.Date

class CalendarRepos(private val calendarDao: CalendarDao, private val firebaseConnect: FirebaseDB) {

    // Method to sync data
    suspend fun syncEvents(userId: String) {
        if (calendarDao.getAnyEvent(userId) == null) {
            try {
                val remoteEvents = firebaseConnect.fetchEventsForUser(userId)
                remoteEvents.forEach { calendarDao.insertEvent(it) }
            } catch (e: Exception) {
                Log.e("CalendarRepository", "Failed to sync initial events from Firebase: ${e.message}")
            }
        }
    }
//Function to save an event
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
//Funtion to delete
    suspend fun deleteEvent(event: CalendarSchedule) {

        calendarDao.deleteEvent(event)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Async Firebase delete
                firebaseConnect.deleteEventFromFirebase(event)
            } catch (e: Exception) {
                Log.e("CalendarRepository", "Failed to delete event from Firebase: ${e.message}")
            }
        }
    }
    fun getEventByGroup(userId: String, eventGroup: String): Flow<List<CalendarSchedule>> {
        return calendarDao.getEventByGroup(userId, eventGroup)
    }
    fun getEventsByDateRange(userId: String, startDate: Date, endDate: Date): Flow<List<CalendarSchedule>> {
        return calendarDao.getEventsByDateRange(userId, startDate, endDate)
    }
}
