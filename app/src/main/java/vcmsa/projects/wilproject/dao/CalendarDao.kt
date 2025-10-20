package vcmsa.projects.wilproject.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import vcmsa.projects.wilproject.models.CalendarSchedule
import java.util.Date

@Dao
interface CalendarDao {
    @Upsert
    suspend fun insertEvent(event: CalendarSchedule)

    @Delete
    suspend fun deleteEvent(event: CalendarSchedule)

    @Query("SELECT * FROM events ORDER BY eventName ASC")
    fun getEventOrderedByName() : Flow<List<CalendarSchedule>>

    @Query("SELECT * FROM events WHERE userId = :userId AND eventType = :eventGroup")
    fun getEventByGroup(userId: String, eventGroup: String): Flow<List<CalendarSchedule>>

    @Query("SELECT * FROM events WHERE userId = :userId AND eventDate BETWEEN :startDate AND :endDate ORDER BY eventDate ASC")
    fun getEventsByDateRange(userId: String, startDate: Date, endDate: Date): Flow<List<CalendarSchedule>>


}