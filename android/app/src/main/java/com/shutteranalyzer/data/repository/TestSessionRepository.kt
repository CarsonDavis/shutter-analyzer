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

    /**
     * Update the video URI for a session.
     */
    suspend fun updateVideoUri(sessionId: Long, uri: String)

    /**
     * Update the expected speeds for a session.
     */
    suspend fun updateExpectedSpeeds(sessionId: Long, expectedSpeeds: List<String>)

    /**
     * Save events with expected speeds and calculated deviations.
     */
    suspend fun saveEventsWithExpectedSpeeds(
        sessionId: Long,
        events: List<ShutterEvent>,
        measuredSpeeds: List<Double>,
        expectedSpeeds: List<String>
    )
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

    override suspend fun updateVideoUri(sessionId: Long, uri: String) {
        testSessionDao.updateVideoUri(sessionId, uri)
    }

    override suspend fun updateExpectedSpeeds(sessionId: Long, expectedSpeeds: List<String>) {
        val json = expectedSpeeds.joinToString(",")
        testSessionDao.updateExpectedSpeeds(sessionId, json)
    }

    override suspend fun saveEventsWithExpectedSpeeds(
        sessionId: Long,
        events: List<ShutterEvent>,
        measuredSpeeds: List<Double>,
        expectedSpeeds: List<String>
    ) {
        // Update expected speeds on the session
        updateExpectedSpeeds(sessionId, expectedSpeeds)

        // Delete existing events first
        shutterEventDao.deleteForSession(sessionId)

        // Convert and insert new events with expected speeds and deviations
        val entities = events.mapIndexed { index, event ->
            val measured = measuredSpeeds.getOrElse(index) { 0.0 }
            val expected = expectedSpeeds.getOrNull(index)
            val deviation = if (expected != null && measured > 0) {
                calculateDeviation(expected, measured)
            } else {
                null
            }

            ShutterEventEntity(
                sessionId = sessionId,
                startFrame = event.startFrame,
                endFrame = event.endFrame,
                expectedSpeed = expected,
                measuredSpeed = measured,
                deviationPercent = deviation,
                brightnessValuesJson = event.brightnessValues.joinToString(",")
            )
        }
        shutterEventDao.insertAll(entities)

        // Update average deviation on session
        val validDeviations = entities.mapNotNull { it.deviationPercent }
        if (validDeviations.isNotEmpty()) {
            val avgDeviation = validDeviations.average()
            testSessionDao.updateAvgDeviation(sessionId, avgDeviation)
        }
    }

    /**
     * Calculate deviation percentage between expected and measured speeds.
     *
     * @param expected Expected speed as string (e.g., "1/500")
     * @param measured Measured speed in seconds
     * @return Deviation as percentage
     */
    private fun calculateDeviation(expected: String, measured: Double): Double {
        val expectedSeconds = parseShutterSpeed(expected) ?: return 0.0
        return ((measured - expectedSeconds) / expectedSeconds) * 100.0
    }

    /**
     * Parse a shutter speed string to seconds.
     *
     * @param speed Speed string (e.g., "1/500", "1/60", "1")
     * @return Speed in seconds, or null if parsing fails
     */
    private fun parseShutterSpeed(speed: String): Double? {
        return try {
            if (speed.contains("/")) {
                val parts = speed.split("/")
                if (parts.size == 2) {
                    parts[0].toDouble() / parts[1].toDouble()
                } else {
                    null
                }
            } else {
                speed.toDoubleOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    // Mapping functions
    private fun TestSessionEntity.toDomainModel(events: List<ShutterEvent>): TestSession {
        val expectedSpeedsList = if (expectedSpeedsJson.isEmpty()) {
            emptyList()
        } else {
            expectedSpeedsJson.split(",")
        }
        return TestSession(
            id = id,
            cameraId = cameraId,
            recordingFps = recordingFps,
            testedAt = Instant.ofEpochMilli(testedAt),
            avgDeviationPercent = avgDeviationPercent,
            events = events,
            expectedSpeeds = expectedSpeedsList,
            videoUri = videoUri
        )
    }

    private fun TestSession.toEntity(): TestSessionEntity {
        return TestSessionEntity(
            id = id,
            cameraId = cameraId,
            recordingFps = recordingFps,
            testedAt = testedAt.toEpochMilli(),
            avgDeviationPercent = avgDeviationPercent,
            expectedSpeedsJson = expectedSpeeds.joinToString(","),
            videoUri = videoUri
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
            peakBrightness = null,
            expectedSpeed = expectedSpeed,
            measuredSpeed = measuredSpeed
        )
    }
}
