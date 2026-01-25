package com.shutteranalyzer.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shutteranalyzer.data.local.database.entity.TestSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for test session operations.
 */
@Dao
interface TestSessionDao {

    /**
     * Get all test sessions for a camera, ordered by test date (newest first).
     */
    @Query("SELECT * FROM test_sessions WHERE cameraId = :cameraId ORDER BY testedAt DESC")
    fun getSessionsForCamera(cameraId: Long): Flow<List<TestSessionEntity>>

    /**
     * Get a test session by its ID.
     */
    @Query("SELECT * FROM test_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): TestSessionEntity?

    /**
     * Insert a new test session and return its generated ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: TestSessionEntity): Long

    /**
     * Update the average deviation for a session.
     */
    @Query("UPDATE test_sessions SET avgDeviationPercent = :avgDeviation WHERE id = :sessionId")
    suspend fun updateAvgDeviation(sessionId: Long, avgDeviation: Double)

    /**
     * Delete a test session (cascades to events).
     */
    @Delete
    suspend fun delete(session: TestSessionEntity)

    /**
     * Delete all sessions for a camera.
     */
    @Query("DELETE FROM test_sessions WHERE cameraId = :cameraId")
    suspend fun deleteAllForCamera(cameraId: Long)
}
