package com.example.ourapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface CalendarDao {
    @Upsert
    suspend fun insertEvent(event: CalendarSchedule)

    @Delete
    suspend fun deleteEvent(event: CalendarSchedule)

    @Query("SELECT * FROM events ORDER BY eventName ASC")
    fun getEventOrderedByName() : Flow<List<CalendarSchedule>>      //Flow for notifying changes.

    @Query("SELECT * FROM events WHERE eventType = :eventGroup")
    fun getEventByGroup(eventGroup: String): Flow<List<CalendarSchedule>>


    // New query to get events by date range
    @Query("SELECT * FROM events WHERE eventDate BETWEEN :startDate AND :endDate ORDER BY eventDate ASC")
    fun getEventsByDateRange(startDate: Date, endDate: Date): Flow<List<CalendarSchedule>>
}