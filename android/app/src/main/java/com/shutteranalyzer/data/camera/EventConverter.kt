package com.shutteranalyzer.data.camera

import com.shutteranalyzer.analysis.model.ShutterEvent

/**
 * Converts EventMarker (timestamp-based) to ShutterEvent (frame-index-based).
 *
 * During live recording, events are tracked by timestamps (nanoseconds from system clock).
 * For analysis and storage, we need frame indices based on the recording FPS.
 */
object EventConverter {

    /**
     * Convert an EventMarker to a ShutterEvent.
     *
     * @param marker The timestamp-based event marker from live detection
     * @param recordingStartTimestamp The timestamp when recording started (nanoseconds)
     * @param fps The recording frame rate
     * @param baselineBrightness The baseline brightness value for weighting calculations
     * @return A ShutterEvent with frame indices instead of timestamps
     */
    fun toShutterEvent(
        marker: EventMarker,
        recordingStartTimestamp: Long,
        fps: Double,
        baselineBrightness: Double
    ): ShutterEvent {
        val startFrame = timestampToFrame(marker.startTimestamp, recordingStartTimestamp, fps)
        val endFrame = timestampToFrame(marker.endTimestamp, recordingStartTimestamp, fps)

        return ShutterEvent(
            startFrame = startFrame,
            endFrame = endFrame,
            brightnessValues = marker.brightnessValues,
            baselineBrightness = baselineBrightness,
            peakBrightness = marker.brightnessValues.maxOrNull()
        )
    }

    /**
     * Convert a list of EventMarkers to ShutterEvents.
     *
     * @param markers The list of timestamp-based event markers
     * @param recordingStartTimestamp The timestamp when recording started (nanoseconds)
     * @param fps The recording frame rate
     * @param baselineBrightness The baseline brightness value for weighting calculations
     * @return A list of ShutterEvents with frame indices
     */
    fun toShutterEvents(
        markers: List<EventMarker>,
        recordingStartTimestamp: Long,
        fps: Double,
        baselineBrightness: Double
    ): List<ShutterEvent> {
        return markers.map { marker ->
            toShutterEvent(marker, recordingStartTimestamp, fps, baselineBrightness)
        }
    }

    /**
     * Convert a timestamp to a frame index.
     *
     * @param timestamp The event timestamp in nanoseconds
     * @param startTimestamp The recording start timestamp in nanoseconds
     * @param fps The frame rate
     * @return The frame index (0-based)
     */
    private fun timestampToFrame(timestamp: Long, startTimestamp: Long, fps: Double): Int {
        val elapsedNanos = timestamp - startTimestamp
        val elapsedSeconds = elapsedNanos / 1_000_000_000.0
        return (elapsedSeconds * fps).toInt().coerceAtLeast(0)
    }
}
