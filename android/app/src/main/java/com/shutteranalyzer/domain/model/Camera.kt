package com.shutteranalyzer.domain.model

import java.time.Instant

/**
 * Domain model representing a camera profile.
 *
 * This is the clean domain representation, separate from the Room entity.
 */
data class Camera(
    val id: Long = 0,
    val name: String,
    val createdAt: Instant,
    val testCount: Int = 0
)
