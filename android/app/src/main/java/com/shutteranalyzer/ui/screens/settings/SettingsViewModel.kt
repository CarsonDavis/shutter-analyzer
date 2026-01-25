package com.shutteranalyzer.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shutteranalyzer.data.local.datastore.AppSettings
import com.shutteranalyzer.data.local.datastore.Sensitivity
import com.shutteranalyzer.data.local.datastore.SpeedSet
import com.shutteranalyzer.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /**
     * Current app settings.
     */
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    /**
     * Update the default speed set.
     */
    fun setDefaultSpeedSet(speedSet: SpeedSet) {
        viewModelScope.launch {
            settingsRepository.setDefaultSpeedSet(speedSet)
        }
    }

    /**
     * Update detection sensitivity.
     */
    fun setDetectionSensitivity(sensitivity: Sensitivity) {
        viewModelScope.launch {
            settingsRepository.setDetectionSensitivity(sensitivity)
        }
    }

    /**
     * Reset onboarding flag to show tutorial again.
     */
    fun resetOnboarding() {
        viewModelScope.launch {
            settingsRepository.setHasSeenOnboarding(false)
        }
    }
}
