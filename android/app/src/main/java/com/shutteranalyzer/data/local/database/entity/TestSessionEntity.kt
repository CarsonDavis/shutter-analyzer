package com.shutteranalyzer.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Room entity representing a test session for a camera.
 *
 * A test session records one set of shutter speed measurements.
 * It has a foreign key relationship to CameraEntity.
 */
@Entity(
    tableName = "test_sessions",
    foreignKeys = [
        ForeignKey(
            entity = CameraEntity::class,
            parentColumns = ["id"],
            childColumns = ["cameraId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TestSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Foreign key to the camera this session belongs to */
    @ColumnInfo(index = true)
    val cameraId: Long,

    /** Recording frame rate used for this test (e.g., 240.0 for 240fps slow-mo) */
    val recordingFps: Double,

    /** Timestamp when the test was performed */
    val testedAt: Long = System.currentTimeMillis(),

    /** Average deviation percentage across all events (null if not yet calculated) */
    val avgDeviationPercent: Double? = null,

    /** Expected shutter speeds as comma-separated string (e.g., "1/500,1/250,1/125") */
    val expectedSpeedsJson: String = "",

    /** URI of the recorded video (null if not saved) */
    val videoUri: String? = null
)
