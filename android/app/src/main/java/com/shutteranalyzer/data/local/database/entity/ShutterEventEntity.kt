package com.shutteranalyzer.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Room entity representing a single shutter event measurement.
 *
 * Each event records one shutter actuation with its measured speed
 * and comparison to expected speed.
 */
@Entity(
    tableName = "shutter_events",
    foreignKeys = [
        ForeignKey(
            entity = TestSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ShutterEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Foreign key to the test session this event belongs to */
    @ColumnInfo(index = true)
    val sessionId: Long,

    /** Frame index where the shutter started opening */
    val startFrame: Int,

    /** Frame index where the shutter finished closing */
    val endFrame: Int,

    /** Expected shutter speed as string (e.g., "1/500") or null if not specified */
    val expectedSpeed: String?,

    /** Measured shutter speed as 1/x value (e.g., 500.0 for 1/500s) */
    val measuredSpeed: Double,

    /** Deviation from expected speed as percentage (null if no expected speed) */
    val deviationPercent: Double?,

    /** Brightness values for each frame in the event, stored as comma-separated doubles */
    val brightnessValuesJson: String
)
