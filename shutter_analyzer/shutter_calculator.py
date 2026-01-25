"""
Shutter speed calculation module for the Camera Shutter Speed Analyzer.
This module provides functionality for calculating and analyzing shutter speeds
based on detected shutter events.
"""

from typing import List, Tuple, Dict, Optional
import numpy as np


class ShutterEvent:
    """
    Represents a single shutter opening/closing event.

    Attributes:
        start_frame: Frame index where the shutter starts opening
        end_frame: Frame index where the shutter finishes closing
        brightness_values: List of brightness values for frames in the event
        baseline_brightness: Brightness when shutter is fully closed
        peak_brightness: Brightness when shutter is fully open
    """

    def __init__(
        self,
        start_frame: int,
        end_frame: int,
        brightness_values: List[float],
        baseline_brightness: Optional[float] = None,
        peak_brightness: Optional[float] = None,
    ):
        """
        Initialize a ShutterEvent.

        Args:
            start_frame: Frame index where the shutter starts opening
            end_frame: Frame index where the shutter finishes closing
            brightness_values: List of brightness values for frames in this event
            baseline_brightness: Brightness when shutter is fully closed (for weighting)
            peak_brightness: Brightness when shutter is fully open (for weighting)
        """
        self.start_frame = start_frame
        self.end_frame = end_frame
        self.brightness_values = brightness_values
        self.baseline_brightness = baseline_brightness
        self.peak_brightness = peak_brightness

    @property
    def duration_frames(self) -> int:
        """
        Calculate the duration of the event in frames.

        Returns:
            Number of frames the shutter was open
        """
        return self.end_frame - self.start_frame + 1

    @property
    def weighted_duration_frames(self) -> float:
        """
        Calculate weighted duration where partial-open frames contribute proportionally.

        Uses per-event peak calculation based on median brightness, which naturally
        identifies the plateau level. This means:
        - Plateau frames (at or above median) contribute 1.0
        - Transition frames (below median) contribute proportionally

        Example: brightness values [20, 80, 80, 100, 100, 80, 20]
        - Median = 80 (the plateau level)
        - 20 → weight 0.25 (transition)
        - 80, 100 → weight 1.0 (plateau/fully open)

        Returns:
            Weighted frame count (float)
        """
        if not self.brightness_values:
            return float(self.duration_frames)

        if self.baseline_brightness is None:
            return float(self.duration_frames)

        # Use median of this event's brightness as the peak (plateau level)
        event_peak = float(np.median(self.brightness_values))

        # Fall back if peak is not greater than baseline
        if event_peak <= self.baseline_brightness:
            return float(self.duration_frames)

        total = 0.0
        brightness_range = event_peak - self.baseline_brightness

        for brightness in self.brightness_values:
            # Calculate weight: 0 at baseline, 1 at event's median (plateau)
            weight = (brightness - self.baseline_brightness) / brightness_range
            # Clamp to [0, 1] - frames above median also count as 1.0
            weight = max(0.0, min(1.0, weight))
            total += weight

        return total

    @property
    def max_brightness(self) -> float:
        """
        Get the maximum brightness value during the event.

        Returns:
            Maximum brightness value
        """
        return max(self.brightness_values) if self.brightness_values else 0

    @property
    def avg_brightness(self) -> float:
        """
        Get the average brightness value during the event.

        Returns:
            Average brightness value
        """
        return (
            sum(self.brightness_values) / len(self.brightness_values)
            if self.brightness_values
            else 0
        )

    def __repr__(self) -> str:
        """String representation of the ShutterEvent."""
        weighted = self.weighted_duration_frames
        return (
            f"ShutterEvent(start_frame={self.start_frame}, end_frame={self.end_frame}, "
            f"duration_frames={self.duration_frames}, weighted={weighted:.2f}, "
            f"max_brightness={self.max_brightness:.2f})"
        )


class ShutterSpeedCalculator:
    """Calculates and analyzes shutter speeds."""

    @staticmethod
    def calculate_duration_seconds(
        start_frame: int,
        end_frame: int,
        fps: float,
        recording_fps: Optional[float] = None,
    ) -> float:
        """
        Calculates duration in seconds.

        Args:
            start_frame: Start frame index
            end_frame: End frame index
            fps: Frames per second of the saved video
            recording_fps: Actual recording FPS (for slow motion videos)

        Returns:
            Duration in seconds
        """
        frame_count = end_frame - start_frame + 1

        if recording_fps is not None:
            # Adjust for slow motion recording
            time_scale_factor = fps / recording_fps
            return (frame_count / fps) * time_scale_factor
        else:
            # Regular calculation
            return frame_count / fps

    @staticmethod
    def calculate_shutter_speed(
        shutter_event: ShutterEvent,
        fps: float,
        recording_fps: Optional[float] = None,
        use_weighted: bool = True,
    ) -> float:
        """
        Calculates the effective shutter speed as a fraction (1/x).

        Args:
            shutter_event: ShutterEvent object
            fps: Frames per second of the saved video
            recording_fps: Actual recording FPS (for slow motion videos)
            use_weighted: If True, use weighted frame count for better accuracy

        Returns:
            Shutter speed as a decimal representing 1/x
            (e.g., 0.002 for 1/500 second)
        """
        # Get frame count (weighted or simple)
        if use_weighted and shutter_event.baseline_brightness is not None:
            frame_count = shutter_event.weighted_duration_frames
        else:
            frame_count = shutter_event.duration_frames

        # Use recording_fps if provided (for slow-motion videos)
        effective_fps = recording_fps if recording_fps is not None else fps

        # Calculate duration in seconds
        duration = frame_count / effective_fps

        # Convert to conventional shutter speed (1/x)
        # Return the reciprocal of the duration
        return 1.0 / duration

    @staticmethod
    def compare_with_expected(measured: float, expected: float) -> float:
        """
        Compares measured vs. expected speeds.

        Args:
            measured: Measured shutter speed in seconds
            expected: Expected shutter speed in seconds

        Returns:
            Percentage error (positive means measured is longer than expected)
        """
        return ((measured - expected) / expected) * 100

    @staticmethod
    def group_shutter_events(
        shutter_events: List[ShutterEvent], expected_speeds: List[float], fps: float
    ) -> Dict[float, List[Tuple[ShutterEvent, float]]]:
        """
        Groups events by expected speed settings.

        Sorts events by duration before matching with expected speeds.

        Args:
            shutter_events: List of ShutterEvent objects
            expected_speeds: List of expected shutter speeds in seconds (from fastest to slowest)
            fps: Frames per second of the video

        Returns:
            Dictionary mapping expected speeds to lists of (event, measured_speed) tuples
        """
        result = {}

        # Sort expected speeds from fastest to slowest (smallest to largest value)
        expected_speeds_sorted = sorted(expected_speeds)

        # Sort shutter events by duration (shortest to longest)
        sorted_events = sorted(shutter_events, key=lambda e: e.duration_frames)

        # Truncate lists to match
        min_length = min(len(sorted_events), len(expected_speeds_sorted))
        sorted_events = sorted_events[:min_length]
        expected_speeds_sorted = expected_speeds_sorted[:min_length]

        # Match events with speeds
        for event, expected in zip(sorted_events, expected_speeds_sorted):
            measured = ShutterSpeedCalculator.calculate_shutter_speed(event, fps)

            if expected not in result:
                result[expected] = []

            result[expected].append((event, measured))

        return result
