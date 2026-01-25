package com.shutteranalyzer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shutteranalyzer.data.repository.CameraRepository
import com.shutteranalyzer.domain.model.Camera
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cameraRepository: CameraRepository
) : ViewModel() {

    /**
     * All cameras from the database.
     */
    val cameras: StateFlow<List<Camera>> = cameraRepository.getAllCameras()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Whether the camera list is empty.
     */
    val isEmpty: StateFlow<Boolean> = cameras
        .map { it.isEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
}
