package com.shutteranalyzer.ui.screens.camera

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shutteranalyzer.data.repository.CameraRepository
import com.shutteranalyzer.data.repository.TestSessionRepository
import com.shutteranalyzer.domain.model.Camera
import com.shutteranalyzer.domain.model.TestSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Camera Detail screen.
 */
@HiltViewModel
class CameraDetailViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val testSessionRepository: TestSessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val cameraId: Long = savedStateHandle.get<Long>("cameraId") ?: 0L

    /**
     * The camera being viewed.
     */
    private val _camera = MutableStateFlow<Camera?>(null)
    val camera: StateFlow<Camera?> = _camera.asStateFlow()

    /**
     * Test sessions for this camera.
     */
    val sessions: StateFlow<List<TestSession>> = testSessionRepository
        .getSessionsForCamera(cameraId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Whether a delete confirmation dialog should be shown.
     */
    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation.asStateFlow()

    /**
     * Whether the camera was deleted (for navigation).
     */
    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    /**
     * Whether the camera name is being edited.
     */
    private val _isEditingName = MutableStateFlow(false)
    val isEditingName: StateFlow<Boolean> = _isEditingName.asStateFlow()

    /**
     * Edited camera name.
     */
    private val _editedName = MutableStateFlow("")
    val editedName: StateFlow<String> = _editedName.asStateFlow()

    init {
        loadCamera()
    }

    private fun loadCamera() {
        viewModelScope.launch {
            val loadedCamera = cameraRepository.getCameraById(cameraId)
            _camera.value = loadedCamera
            _editedName.value = loadedCamera?.name ?: ""
        }
    }

    /**
     * Start editing the camera name.
     */
    fun startEditingName() {
        _editedName.value = _camera.value?.name ?: ""
        _isEditingName.value = true
    }

    /**
     * Cancel editing the camera name.
     */
    fun cancelEditingName() {
        _isEditingName.value = false
        _editedName.value = _camera.value?.name ?: ""
    }

    /**
     * Update the edited name value.
     */
    fun updateEditedName(name: String) {
        _editedName.value = name
    }

    /**
     * Save the edited camera name.
     */
    fun saveEditedName() {
        val currentCamera = _camera.value ?: return
        val newName = _editedName.value.trim()
        if (newName.isNotBlank() && newName != currentCamera.name) {
            viewModelScope.launch {
                val updatedCamera = currentCamera.copy(name = newName)
                cameraRepository.saveCamera(updatedCamera)
                _camera.value = updatedCamera
            }
        }
        _isEditingName.value = false
    }

    /**
     * Show delete confirmation dialog.
     */
    fun showDeleteConfirmation() {
        _showDeleteConfirmation.value = true
    }

    /**
     * Dismiss delete confirmation dialog.
     */
    fun dismissDeleteConfirmation() {
        _showDeleteConfirmation.value = false
    }

    /**
     * Delete the camera and all its sessions.
     */
    fun deleteCamera() {
        val currentCamera = _camera.value ?: return
        viewModelScope.launch {
            cameraRepository.deleteCamera(currentCamera)
            _showDeleteConfirmation.value = false
            _isDeleted.value = true
        }
    }
}
