package com.shutteranalyzer.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shutteranalyzer.data.local.database.entity.CameraEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for camera operations.
 */
@Dao
interface CameraDao {

    /**
     * Get all cameras ordered by creation date (newest first).
     */
    @Query("SELECT * FROM cameras ORDER BY createdAt DESC")
    fun getAllCameras(): Flow<List<CameraEntity>>

    /**
     * Get a camera by its ID.
     */
    @Query("SELECT * FROM cameras WHERE id = :id")
    suspend fun getCameraById(id: Long): CameraEntity?

    /**
     * Get number of test sessions for a camera.
     */
    @Query("SELECT COUNT(*) FROM test_sessions WHERE cameraId = :cameraId")
    suspend fun getTestCountForCamera(cameraId: Long): Int

    /**
     * Insert a new camera and return its generated ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(camera: CameraEntity): Long

    /**
     * Update an existing camera.
     */
    @Update
    suspend fun update(camera: CameraEntity)

    /**
     * Delete a camera (cascades to test sessions and events).
     */
    @Delete
    suspend fun delete(camera: CameraEntity)
}
