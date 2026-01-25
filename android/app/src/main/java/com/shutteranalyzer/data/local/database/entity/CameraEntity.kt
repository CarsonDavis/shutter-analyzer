package com.shutteranalyzer.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a camera profile.
 *
 * A camera can have multiple test sessions associated with it.
 */
@Entity(tableName = "cameras")
data class CameraEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** User-defined name for the camera (e.g., "Fuji GW690II") */
    val name: String,

    /** Timestamp when the camera was added */
    val createdAt: Long = System.currentTimeMillis()
)
