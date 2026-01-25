package com.shutteranalyzer.data.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Range
import android.util.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the slow-motion recording capabilities of a device.
 */
data class SlowMotionCapability(
    /** Maximum supported FPS for high-speed recording */
    val maxFps: Int,
    /** All supported FPS ranges for high-speed recording */
    val supportedFpsRanges: List<Range<Int>>,
    /** Video sizes supported for high-speed recording */
    val supportedSizes: List<Size>,
    /** Whether the device supports any high-speed recording */
    val isHighSpeedSupported: Boolean
)

/**
 * Checks device capabilities for slow-motion recording.
 */
@Singleton
class SlowMotionCapabilityChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /**
     * Get slow-motion capabilities for the back camera.
     */
    fun getCapabilities(): SlowMotionCapability {
        return try {
            val backCameraId = getBackCameraId() ?: return defaultCapability()
            val characteristics = cameraManager.getCameraCharacteristics(backCameraId)
            val configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: return defaultCapability()

            val highSpeedSizes = configs.highSpeedVideoSizes?.toList() ?: emptyList()
            val fpsRanges = configs.highSpeedVideoFpsRanges?.toList() ?: emptyList()

            val maxFps = fpsRanges.maxOfOrNull { it.upper } ?: 30
            val isHighSpeedSupported = highSpeedSizes.isNotEmpty() && fpsRanges.isNotEmpty()

            SlowMotionCapability(
                maxFps = maxFps,
                supportedFpsRanges = fpsRanges,
                supportedSizes = highSpeedSizes,
                isHighSpeedSupported = isHighSpeedSupported
            )
        } catch (e: Exception) {
            defaultCapability()
        }
    }

    /**
     * Get the best FPS for measuring a target shutter speed accurately.
     *
     * Rule of thumb: FPS should be at least 2x the denominator of the shutter speed.
     * - 1/500 needs at least 1000fps (but 240fps can still give reasonable results)
     * - 1/250 needs at least 500fps (240fps is acceptable)
     * - 1/125 needs at least 250fps (240fps is good)
     * - 1/60 needs at least 120fps (60fps is minimum)
     *
     * @param targetSpeed Target shutter speed as string (e.g., "1/500")
     * @return Recommended FPS, or 30 if can't determine
     */
    fun getBestFpsForAccuracy(targetSpeed: String): Int {
        val denominator = parseShutterSpeedDenominator(targetSpeed) ?: return 30
        val capabilities = getCapabilities()

        // Find the highest FPS we can support
        val availableFps = capabilities.supportedFpsRanges
            .map { it.upper }
            .distinct()
            .sortedDescending()

        // Ideal is 2x the denominator, but we'll take what we can get
        val idealFps = denominator * 2

        return availableFps.firstOrNull { it >= idealFps }
            ?: availableFps.firstOrNull()
            ?: 30
    }

    /**
     * Get accuracy information for a given recording FPS.
     *
     * @param recordingFps The FPS being used for recording
     * @return Human-readable accuracy description
     */
    fun getAccuracyDescription(recordingFps: Int): String {
        return when {
            recordingFps >= 240 -> "Accurate to 1/250 and slower"
            recordingFps >= 120 -> "Accurate to 1/125 and slower"
            recordingFps >= 60 -> "Accurate to 1/60 and slower"
            else -> "Limited accuracy - use slow-mo for best results"
        }
    }

    /**
     * Get the fastest measurable shutter speed for a given FPS.
     *
     * @param recordingFps The FPS being used for recording
     * @return Fastest shutter speed that can be measured accurately (as 1/x value)
     */
    fun getFastestMeasurableSpeed(recordingFps: Int): Int {
        // Need at least 2 frames to measure, so fastest is fps/2
        return recordingFps / 2
    }

    private fun getBackCameraId(): String? {
        return cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
        }
    }

    private fun parseShutterSpeedDenominator(speed: String): Int? {
        // Parse "1/500" -> 500
        val match = Regex("""1/(\d+)""").find(speed)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun defaultCapability() = SlowMotionCapability(
        maxFps = 30,
        supportedFpsRanges = emptyList(),
        supportedSizes = emptyList(),
        isHighSpeedSupported = false
    )
}
