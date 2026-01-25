package com.shutteranalyzer.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore extension for app-wide preferences.
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Preference keys for settings.
 */
object SettingsKeys {
    val DEFAULT_SPEED_SET = stringPreferencesKey("default_speed_set")
    val DETECTION_SENSITIVITY = intPreferencesKey("detection_sensitivity")
    val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
}

/**
 * Speed set options.
 */
enum class SpeedSet(val displayName: String, val speeds: List<String>) {
    STANDARD(
        "Standard (11 speeds)",
        listOf("1/1000", "1/500", "1/250", "1/125", "1/60", "1/30", "1/15", "1/8", "1/4", "1/2", "1s")
    ),
    FAST(
        "Fast (5 speeds)",
        listOf("1/1000", "1/500", "1/250", "1/125", "1/60")
    ),
    SLOW(
        "Slow (6 speeds)",
        listOf("1/60", "1/30", "1/15", "1/8", "1/4", "1/2")
    ),
    CUSTOM(
        "Custom",
        emptyList()
    )
}

/**
 * Detection sensitivity levels.
 */
enum class Sensitivity(val value: Int, val displayName: String, val marginFactor: Double) {
    LOW(0, "Low", 2.0),
    NORMAL(1, "Normal", 1.5),
    HIGH(2, "High", 1.2)
}

/**
 * Data class representing app settings.
 */
data class AppSettings(
    val defaultSpeedSet: SpeedSet = SpeedSet.STANDARD,
    val detectionSensitivity: Sensitivity = Sensitivity.NORMAL,
    val hasSeenOnboarding: Boolean = false
)

/**
 * DataStore access for settings.
 */
class SettingsDataStore(private val dataStore: DataStore<Preferences>) {

    /**
     * Flow of app settings.
     */
    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            defaultSpeedSet = preferences[SettingsKeys.DEFAULT_SPEED_SET]?.let { name ->
                SpeedSet.entries.find { it.name == name }
            } ?: SpeedSet.STANDARD,
            detectionSensitivity = preferences[SettingsKeys.DETECTION_SENSITIVITY]?.let { value ->
                Sensitivity.entries.find { it.value == value }
            } ?: Sensitivity.NORMAL,
            hasSeenOnboarding = preferences[SettingsKeys.HAS_SEEN_ONBOARDING] ?: false
        )
    }

    /**
     * Flow of hasSeenOnboarding flag.
     */
    val hasSeenOnboarding: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SettingsKeys.HAS_SEEN_ONBOARDING] ?: false
    }

    /**
     * Update the default speed set.
     */
    suspend fun setDefaultSpeedSet(speedSet: SpeedSet) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.DEFAULT_SPEED_SET] = speedSet.name
        }
    }

    /**
     * Update detection sensitivity.
     */
    suspend fun setDetectionSensitivity(sensitivity: Sensitivity) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.DETECTION_SENSITIVITY] = sensitivity.value
        }
    }

    /**
     * Mark onboarding as seen.
     */
    suspend fun setHasSeenOnboarding(hasSeen: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.HAS_SEEN_ONBOARDING] = hasSeen
        }
    }
}
