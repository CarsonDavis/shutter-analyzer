package com.shutteranalyzer.data.repository

import com.shutteranalyzer.data.local.database.dao.CameraDao
import com.shutteranalyzer.data.local.database.entity.CameraEntity
import com.shutteranalyzer.domain.model.Camera
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for camera operations.
 */
interface CameraRepository {
    /**
     * Get all cameras as a reactive flow.
     */
    fun getAllCameras(): Flow<List<Camera>>

    /**
     * Get a camera by its ID.
     */
    suspend fun getCameraById(id: Long): Camera?

    /**
     * Save a camera (insert or update).
     * Returns the camera's ID.
     */
    suspend fun saveCamera(camera: Camera): Long

    /**
     * Delete a camera and all its associated test sessions.
     */
    suspend fun deleteCamera(camera: Camera)
}

/**
 * Implementation of CameraRepository using Room database.
 */
@Singleton
class CameraRepositoryImpl @Inject constructor(
    private val cameraDao: CameraDao
) : CameraRepository {

    override fun getAllCameras(): Flow<List<Camera>> {
        return cameraDao.getAllCameras().map { entities ->
            entities.map { entity ->
                val testCount = cameraDao.getTestCountForCamera(entity.id)
                entity.toDomainModel(testCount)
            }
        }
    }

    override suspend fun getCameraById(id: Long): Camera? {
        return cameraDao.getCameraById(id)?.let { entity ->
            val testCount = cameraDao.getTestCountForCamera(entity.id)
            entity.toDomainModel(testCount)
        }
    }

    override suspend fun saveCamera(camera: Camera): Long {
        val entity = camera.toEntity()
        return cameraDao.insert(entity)
    }

    override suspend fun deleteCamera(camera: Camera) {
        cameraDao.delete(camera.toEntity())
    }

    // Mapping functions
    private fun CameraEntity.toDomainModel(testCount: Int): Camera {
        return Camera(
            id = id,
            name = name,
            createdAt = Instant.ofEpochMilli(createdAt),
            testCount = testCount
        )
    }

    private fun Camera.toEntity(): CameraEntity {
        return CameraEntity(
            id = id,
            name = name,
            createdAt = createdAt.toEpochMilli()
        )
    }
}
