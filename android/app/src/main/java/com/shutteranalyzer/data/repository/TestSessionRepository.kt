package com.shutteranalyzer.data.repository

import com.shutteranalyzer.analysis.model.ShutterEvent
import com.shutteranalyzer.data.local.database.dao.ShutterEventDao
import com.shutteranalyzer.data.local.database.dao.TestSessionDao
import com.shutteranalyzer.data.local.database.entity.ShutterEventEntity
import com.shutteranalyzer.data.local.database.entity.TestSessionEntity
import com.shutteranalyzer.domain.model.TestSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for test session operations.
 */
interface TestSessionRepository {
    /**
     * Get all test sessions for a camera as a reactive flow.
     */
    fun getSessionsForCamera(cameraId: Long): Flow<List<TestSession>>

    /**
     * Get a test session by its ID, including all events.
     */
    suspend fun getSessionById(id: Long): TestSession?

    /**
     * Save a test session (insert or update).
     * Returns the session's ID.
     */
    suspend fun saveSession(session: TestSession): Long

    /**
     * Save shutter events for a session.
     * Replaces any existing events.
     */
    suspend fun saveEvents(sessionId: Long, events: List<ShutterEvent>, measuredSpeeds: List<Double>)

    /**
     * Update the average deviation for a session.
     */
    suspend fun updateAvgDeviation(sessionId: Long, avgDeviation: Double)

    /**
     * Delete a test session and all its events.
     */
    suspend fun deleteSession(session: TestSession)
}

/**
 * Implementation of TestSessionRepository using Room database.
 */
@Singleton
class TestSessionRepositoryImpl @Inject constructor(
    private val testSessionDao: TestSessionDao,
    private val shutterEventDao: ShutterEventDao
) : TestSessionRepository {

    override fun getSessionsForCamera(cameraId: Long): Flow<List<TestSession>> {
        return testSessionDao.getSessionsForCamera(cameraId).map { entities ->
            entities.map { it.toDomainModel(emptyList()) }
        }
    }

    override suspend fun getSessionById(id: Long): TestSession? {
        val sessionEntity = testSessionDao.getSessionById(id) ?: return null
        val eventEntities = shutterEventDao.getEventsForSessionOnce(id)
        val events = eventEntities.map { it.toDomainModel() }
        return sessionEntity.toDomainModel(events)
    }

    override suspend fun saveSession(session: TestSession): Long {
        val entity = session.toEntity()
        return testSessionDao.insert(entity)
    }

    override suspend fun saveEvents(
        sessionId: Long,
        events: List<ShutterEvent>,
        measuredSpeeds: List<Double>
    ) {
        // Delete existing events first
        shutterEventDao.deleteForSession(sessionId)

        // Convert and insert new events
        val entities = events.mapIndexed { index, event ->
            ShutterEventEntity(
                sessionId = sessionId,
                startFrame = event.startFrame,
                endFrame = event.endFrame,
                expectedSpeed = null, // Can be set later
                measuredSpeed = measuredSpeeds.getOrElse(index) { 0.0 },
                deviationPercent = null, // Can be set later
                brightnessValuesJson = event.brightnessValues.joinToString(",")
            )
        }
        shutterEventDao.insertAll(entities)
    }

    override suspend fun updateAvgDeviation(sessionId: Long, avgDeviation: Double) {
        testSessionDao.updateAvgDeviation(sessionId, avgDeviation)
    }

    override suspend fun deleteSession(session: TestSession) {
        testSessionDao.delete(session.toEntity())
    }

    // Mapping functions
    private fun TestSessionEntity.toDomainModel(events: List<ShutterEvent>): TestSession {
        return TestSession(
            id = id,
            cameraId = cameraId,
            recordingFps = recordingFps,
            testedAt = Instant.ofEpochMilli(testedAt),
            avgDeviationPercent = avgDeviationPercent,
            events = events
        )
    }

    private fun TestSession.toEntity(): TestSessionEntity {
        return TestSessionEntity(
            id = id,
            cameraId = cameraId,
            recordingFps = recordingFps,
            testedAt = testedAt.toEpochMilli(),
            avgDeviationPercent = avgDeviationPercent
        )
    }

    private fun ShutterEventEntity.toDomainModel(): ShutterEvent {
        val brightnessValues = if (brightnessValuesJson.isEmpty()) {
            emptyList()
        } else {
            brightnessValuesJson.split(",").map { it.toDouble() }
        }

        return ShutterEvent(
            startFrame = startFrame,
            endFrame = endFrame,
            brightnessValues = brightnessValues,
            baselineBrightness = null,
            peakBrightness = null
        )
    }
}
