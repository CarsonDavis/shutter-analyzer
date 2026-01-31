package com.shutteranalyzer.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shutteranalyzer.data.local.database.entity.ShutterEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for shutter event operations.
 */
@Dao
interface ShutterEventDao {

    /**
     * Get all events for a test session, ordered by start frame.
     */
    @Query("SELECT * FROM shutter_events WHERE sessionId = :sessionId ORDER BY startFrame ASC")
    fun getEventsForSession(sessionId: Long): Flow<List<ShutterEventEntity>>

    /**
     * Get all events for a session as a list (non-reactive).
     */
    @Query("SELECT * FROM shutter_events WHERE sessionId = :sessionId ORDER BY startFrame ASC")
    suspend fun getEventsForSessionOnce(sessionId: Long): List<ShutterEventEntity>

    /**
     * Insert multiple events at once.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<ShutterEventEntity>)

    /**
     * Delete all events for a session.
     */
    @Query("DELETE FROM shutter_events WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)

    /**
     * Get the count of events for a session.
     */
    @Query("SELECT COUNT(*) FROM shutter_events WHERE sessionId = :sessionId")
    suspend fun getEventCountForSession(sessionId: Long): Int
}
