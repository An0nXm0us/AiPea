package com.example.ourapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * DAO for interacting with the events table.
 */
@Dao
interface CalendarDao {
    /**
     * Insert or update (Upsert) an event.
     */
    @Upsert
    suspend fun insertEvent(event: CalendarSchedule)

    /**
     * Delete an event.
     */
    @Delete
    suspend fun deleteEvent(event: CalendarSchedule)

    /**
     * Get events ordered by name (Flow so UI reacts to DB changes).
     */
    @Query("SELECT * FROM events ORDER BY eventName ASC")
    fun getEventOrderedByName() : Flow<List<CalendarSchedule>>

    /**
     * Return events filtered by group/type.
     */
    @Query("SELECT * FROM events WHERE eventType = :eventGroup")
    fun getEventByGroup(eventGroup: String): Flow<List<CalendarSchedule>>

    /**
     * Return events in a date range (inclusive). Useful for day or period queries.
     */
    @Query("SELECT * FROM events WHERE eventDate BETWEEN :startDate AND :endDate ORDER BY eventDate ASC")
    fun getEventsByDateRange(startDate: Date, endDate: Date): Flow<List<CalendarSchedule>>

    /**
     * Default: get upcoming events (now and future) ordered nearest-first.
     * This is used when no date is selected.
     */
    @Query("SELECT * FROM events WHERE eventDate >= :now ORDER BY eventDate ASC")
    fun getUpcomingEvents(now: Date = Date()): Flow<List<CalendarSchedule>>
}
