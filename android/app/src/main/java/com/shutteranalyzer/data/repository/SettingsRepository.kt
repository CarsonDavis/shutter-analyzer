package com.shutteranalyzer.data.repository

import com.shutteranalyzer.data.local.datastore.AppSettings
import com.shutteranalyzer.data.local.datastore.Sensitivity
import com.shutteranalyzer.data.local.datastore.SettingsDataStore
import com.shutteranalyzer.data.local.datastore.SpeedSet
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for app settings.
 */
interface SettingsRepository {
    /**
     * Flow of app settings.
     */
    val settings: Flow<AppSettings>

    /**
     * Flow of hasSeenOnboarding flag.
     */
    val hasSeenOnboarding: Flow<Boolean>

    /**
     * Update the default speed set.
     */
    suspend fun setDefaultSpeedSet(speedSet: SpeedSet)

    /**
     * Update detection sensitivity.
     */
    suspend fun setDetectionSensitivity(sensitivity: Sensitivity)

    /**
     * Mark onboarding as seen.
     */
    suspend fun setHasSeenOnboarding(hasSeen: Boolean)
}

/**
 * Implementation of SettingsRepository using DataStore.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override val settings: Flow<AppSettings> = settingsDataStore.settings

    override val hasSeenOnboarding: Flow<Boolean> = settingsDataStore.hasSeenOnboarding

    override suspend fun setDefaultSpeedSet(speedSet: SpeedSet) {
        settingsDataStore.setDefaultSpeedSet(speedSet)
    }

    override suspend fun setDetectionSensitivity(sensitivity: Sensitivity) {
        settingsDataStore.setDetectionSensitivity(sensitivity)
    }

    override suspend fun setHasSeenOnboarding(hasSeen: Boolean) {
        settingsDataStore.setHasSeenOnboarding(hasSeen)
    }
}
